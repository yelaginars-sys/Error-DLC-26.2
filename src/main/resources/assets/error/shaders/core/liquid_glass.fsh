#version 150

#define MAX_RECTS 64

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRegion;
    vec4 uViewport;
    vec4 uCommon;
    vec4 uConfig;
    vec4 uGlass;
    vec4 uRects[MAX_RECTS];
    vec4 uParams[MAX_RECTS];
    vec4 uTints[MAX_RECTS];
};

uniform sampler2D Sampler0;

in vec2 vUV;
in vec2 vPixel;

out vec4 fragColor;

const float PI = 3.14159265359;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float sdPanel(vec2 p, vec2 b, float r, out vec2 grad, out float cornerT) {
    float rr = max(min(r, min(b.x, b.y)), 0.001);

    vec2 sgn = vec2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
    vec2 q = abs(p) - b + rr;
    float outside = length(max(q, vec2(0.0)));
    float d;
    if (outside > 0.0) {
        grad = (max(q, vec2(0.0)) / outside) * sgn;
        d = outside + min(max(q.x, q.y), 0.0) - rr;
    } else {
        if (q.x > q.y) grad = vec2(sgn.x, 0.0);
        else grad = vec2(0.0, sgn.y);
        d = max(q.x, q.y) - rr;
    }

    cornerT = smoothstep(-0.5, 1.5, min(q.x, q.y));
    return d;
}

vec3 blurTap(vec2 uv, float radius) {
    return texture(Sampler0, clamp(uv, vec2(0.0001), vec2(0.9999))).rgb;
}

void main() {
    int n = int(uConfig.x + 0.5);

    float eps = max(uCommon.x, 0.5);

    vec4 R0 = uRects[0];
    vec4 P0 = uParams[0];
    vec4 T0 = uTints[0];
    vec2 c0 = R0.xy + R0.zw * 0.5;
    vec2 h0 = R0.zw * 0.5;
    vec2 gAccum;
    float cornerAccum;
    float dAccum = sdPanel(vPixel - c0, h0, P0.x, gAccum, cornerAccum);

    float sAccum = P0.w;
    float wAccum = 1.0;
    float aAccum = P0.z;
    float bAccum = P0.y * P0.z;
    float cAccum = cornerAccum;
    vec3 tcAccum = T0.rgb * (T0.a * P0.z);
    float tAccum = T0.a * P0.z;

    for (int i = 1; i < MAX_RECTS; ++i) {
        if (i >= n) break;
        vec4 R = uRects[i];
        vec4 P = uParams[i];
        vec4 T = uTints[i];
        vec2 center = R.xy + R.zw * 0.5;
        vec2 halfSize = R.zw * 0.5;
        vec2 gi;
        float ci;
        float di = sdPanel(vPixel - center, halfSize, P.x, gi, ci);

        float s1 = sAccum;
        float s2 = P.w;
        float k = 0.0;
        if (abs(s1) > 0.01 && abs(s2) > 0.01 && (s1 > 0.0 || s2 > 0.0)) {
            k = min(abs(s1), abs(s2));
        }

        float h;
        float w;

        if (k > 0.01) {
            h = clamp(0.5 + 0.5 * (di - dAccum) / k, 0.0, 1.0);
            w = 1.0 - h;
            dAccum = mix(di, dAccum, h) - k * h * (1.0 - h);
            vec2 ng = mix(gi, gAccum, h);
            float len = length(ng);
            gAccum = len > 1e-4 ? ng / len : vec2(0.0, 1.0);
        } else {
            h = di < dAccum ? 0.0 : 1.0;
            w = 1.0 - h;
            dAccum = min(di, dAccum);
            gAccum = mix(gi, gAccum, h);
        }

        sAccum = h < 0.5 ? P.w : sAccum;
        float pa = P.z;
        float ta = T.a * pa;
        wAccum = wAccum * h + w;
        aAccum = aAccum * h + pa * w;
        bAccum = bAccum * h + P.y * pa * w;
        cAccum = cAccum * h + ci * w;
        tAccum = tAccum * h + ta * w;
        tcAccum = tcAccum * h + T.rgb * ta * w;
    }

    float wsum = max(wAccum, 1e-5);
    float alpha = aAccum / wsum;
    float corner = clamp(cAccum / wsum, 0.0, 1.0);
    vec3 tintColor = tcAccum / wsum;
    float tintAlpha = tAccum / wsum;
    float blurPx = (bAccum / wsum) * uConfig.y;

    float mask = 1.0 - smoothstep(-eps, eps, dAccum);
    float edgeT = smoothstep(-eps * 2.0, 0.0, dAccum);
    float interior = 1.0 - edgeT;
    float cosB = 1.0 - cos(edgeT * PI * 0.5);

    vec3 col = blurTap(vUV, blurPx);

    float refrPx = min(blurPx * 0.05, 14.0) * uGlass.y * edgeT * (1.0 + corner * 1.2);
    vec2 offset = -gAccum * refrPx * uViewport.zw;
    vec3 ratios = pow(vec3(cosB), vec3(1.045, 1.0, 0.955));
    vec2 lo = vec2(0.0001);
    vec2 hi = vec2(0.9999);
    vec3 warped = vec3(
        texture(Sampler0, clamp(vUV + offset * ratios.r, lo, hi)).r,
        texture(Sampler0, clamp(vUV + offset * ratios.g, lo, hi)).g,
        texture(Sampler0, clamp(vUV + offset * ratios.b, lo, hi)).b
    );
    col = mix(col, warped, edgeT * uGlass.z * (1.0 + corner));

    float rim = smoothstep(1.5, 0.0, abs(dAccum));
    col += vec3(1.0) * (rim * 0.12 * (1.0 + corner * 0.5));

    float surfaceAlpha = alpha * mask;

    if (uConfig.z > 0.5) {
        if (vPixel.x < uRegion.z * 0.5) {
            fragColor = vec4(col, 1.0);
        } else {
            fragColor = vec4(vec3(surfaceAlpha), 1.0);
        }
        return;
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), surfaceAlpha);
}

#version 330
#moj_import <danq:clip.glsl>

uniform sampler2D InputSampler;

layout(std140) uniform BlurData {
    vec2 InputResolution;
    float Quality;
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    vec4 outlineColor;
    vec2 size;
    vec2 location;
    vec2 axisX;
    vec2 axisY;
    vec4 radius;
    float thickness;
    float softness;
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
};

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    float r = (center.y > 0.0) ? radius.x : radius.y;
    r *= 2.0;
    r = max(r, 0.1);
    vec2 q = abs(center) - (size) + r;
    float exponent = 4.0;
    vec2 max_q = max(q, 0.0);
    float dist = pow(pow(max_q.x, exponent) + pow(max_q.y, exponent), 1.0 / exponent) - r;
    return dist + min(max(q.x, q.y), 0.0);
}

vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

vec2 localPoint(vec2 point) {
    vec2 p = point - location;
    float det = axisX.x * axisY.y - axisX.y * axisY.x;
    if (abs(det) < 0.0001) {
        return vec2(-100000.0);
    }

    vec2 normalized = vec2(
        (p.x * axisY.y - p.y * axisY.x) / det,
        (axisX.x * p.y - axisX.y * p.x) / det
    );
    return normalized * size;
}

vec2 localDirection(vec2 v) {
    float det = axisX.x * axisY.y - axisX.y * axisY.x;
    if (abs(det) < 0.0001) {
        return vec2(0.0);
    }
    return vec2((v.x * axisY.y - v.y * axisY.x) / det, (axisX.x * v.y - axisX.y * v.x) / det) * size;
}

vec4 blurColor() {
    #define TAU 6.28318530718
    vec2 center = localPoint(gl_FragCoord.xy);
    vec4 rectColor = createGradient(center / size, color1, color2, color3, color4);

    vec2 invSize = 1.0 / size;
    vec2 invRes = 1.0 / InputResolution.xy;
    vec2 stepX = localDirection(vec2(1.0, 0.0));
    vec2 stepY = localDirection(vec2(0.0, 1.0));

    vec3 blur = texture(InputSampler, gl_FragCoord.xy * invRes).rgb;

    float step = TAU / 16;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            vec2 offset = vec2(cos(d), sin(d)) * Quality * i;
            vec2 local = clamp(center + stepX * offset.x + stepY * offset.y, vec2(0.0), size);
            vec2 normalized = local * invSize;
            vec2 screen = location + normalized.x * axisX + normalized.y * axisY;
            blur += texture(InputSampler, screen * invRes).rgb;
        }
    }

    blur /= 81.0;

    float tint = clamp(rectColor.a, 0.0, 1.0);
    float opacity = min(tint * 2.0, 1.0);
    return vec4(mix(blur, rectColor.rgb, tint), opacity);
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec2 local = localPoint(gl_FragCoord.xy);
    float distance = roundedBoxSDF(local - (size / 2.0), size / 2.0, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., distance);

    if(smoothedAlpha < 0.49 && thickness > 0.) {
        float smoothedborderAlpha = (1.0 - smoothstep(-softness,  softness, distance));
        fragColor = vec4(outlineColor.rgb, smoothedborderAlpha * outlineColor.a);
    } else {
        float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(distance));
        vec4 blur = blurColor();
        vec4 basicColor = vec4(blur.rgb, blur.a * smoothedAlpha);
        fragColor = mix(vec4(blur.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor, borderAlpha), smoothedAlpha);
    }
}

#version 330
#moj_import <danq:clip.glsl>

uniform sampler2D InputSampler;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform UiBatchData {
    vec4 params;
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
    float squircle;
};

in vec2 LocalPos;
in vec4 VertexColor;
in vec2 ScreenUV;
flat in vec2 HalfSize;
flat in vec4 Radius;
flat in vec4 OutlineColor;
flat in float Thickness;
flat in float Softness;
flat in float IsGlyph;
flat in float AtlasIndex;

out vec4 fragColor;

#define TAU 6.28318530718

const float BlurRadius = 30.0;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    float r = (center.y > 0.0) ? radius.x : radius.y;
    r *= 2.0;
    r = max(r, 0.1);
    vec2 q = abs(center) - (size) + r;
    float exponent = squircle > 0.5 ? 4.0 : 2.0;
    vec2 max_q = max(q, 0.0);
    float dist = pow(pow(max_q.x, exponent) + pow(max_q.y, exponent), 1.0 / exponent) - r;
    return dist + min(max(q.x, q.y), 0.0);
}

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

vec4 glyph() {
    bool second = AtlasIndex > 0.5;
    vec2 textureSizePx = second ? vec2(textureSize(Sampler1, 0)) : vec2(textureSize(Sampler0, 0));
    vec2 unitRange = vec2(second ? params.y : params.x) / textureSizePx;
    vec2 screenTexSize = vec2(1.0) / max(fwidth(LocalPos), vec2(0.000001));
    float pxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);

    vec3 msd = second ? texture(Sampler1, LocalPos).rgb : texture(Sampler0, LocalPos).rgb;
    float signedDistance = median(msd.r, msd.g, msd.b) - 0.5;
    float alpha = clamp(pxRange * signedDistance + 0.5, 0.0, 1.0);
    return vec4(VertexColor.rgb, VertexColor.a * alpha);
}

vec4 panel() {
    vec2 p = vec2(LocalPos.x, -LocalPos.y);
    vec2 size = max(HalfSize * 2.0, vec2(0.0001));
    float dist = roundedBoxSDF(p, HalfSize, Radius);

    vec2 pixel = fwidth(LocalPos);
    float px = max(max(pixel.x, pixel.y), 0.0001);
    float softLocal = Softness * px;
    float thickLocal = Thickness * px;

    vec2 coords = p / size + 0.5;
    vec4 rectColor = VertexColor;
    rectColor += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));

    float smoothedAlpha = 1.0 - smoothstep(-px, Thickness > 0.0 ? px : softLocal + px, dist);

    if (smoothedAlpha < 0.49 && Thickness > 0.0) {
        float borderAlpha = 1.0 - smoothstep(-softLocal, softLocal, dist);
        return vec4(OutlineColor.rgb, borderAlpha * OutlineColor.a);
    }

    mat2 toLocal = mat2(dFdx(p), dFdy(p));
    float det = determinant(toLocal);
    mat2 toScreen = abs(det) < 1.0e-9 ? mat2(1.0, 0.0, 0.0, 1.0) : inverse(toLocal);
    mat2 uvPerPixel = mat2(dFdx(ScreenUV), dFdy(ScreenUV));

    vec2 center = p + HalfSize;
    vec3 blur = texture(InputSampler, ScreenUV).rgb;

    float angleStep = TAU / 16.0;
    for (float d = 0.0; d < TAU; d += angleStep) {
        vec2 direction = vec2(cos(d), sin(d));
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            vec2 local = clamp(center + toLocal * (direction * BlurRadius * i), vec2(0.0), size);
            blur += texture(InputSampler, ScreenUV + uvPerPixel * (toScreen * (local - center))).rgb;
        }
    }
    blur /= 81.0;

    float tint = clamp(rectColor.a, 0.0, 1.0);
    float opacity = min(tint * 2.0, 1.0);
    vec3 blurred = mix(blur, rectColor.rgb, tint);

    float borderAlpha = 1.0 - smoothstep(thickLocal - 2.0 * px, thickLocal, abs(dist));
    vec4 basicColor = vec4(blurred, opacity * smoothedAlpha);
    return mix(vec4(blurred, 0.0),
            mix(basicColor, Thickness > 0.0 ? OutlineColor : basicColor, borderAlpha),
            smoothedAlpha);
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec4 color = IsGlyph > 0.5 ? glyph() : panel();
    if (color.a <= 0.0005) {
        discard;
    }
    fragColor = color;
}

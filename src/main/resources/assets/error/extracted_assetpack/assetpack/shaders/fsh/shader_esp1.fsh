#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ZenithData {
    vec4 OutlineColor;
    vec4 FirstFillColor;
    vec4 SecondFillColor;
    float Time;
};

#define oneTexel (1.0 / InSize)

in vec2 texCoord;

out vec4 fragColor;

const float TAU = 6.28318530718;
const float OUTLINE_WIDTH = 2.6;
const int DIRS = 16;
const int OCTAVES = 6;

float random(vec2 st) {
    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));

    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(st);
        st = rot * st * 2.0 + vec2(100.0);
        amplitude *= 0.5;
    }

    return value;
}

float sampleMask(vec2 uv) {
    vec4 color = texture(InSampler, uv);
    return clamp(max(color.a, max(color.r, max(color.g, color.b))), 0.0, 1.0);
}

float flamePattern(vec2 uv) {
    vec2 resolution = 1.0 / oneTexel;
    vec2 p = gl_FragCoord.xy * 8.0 / resolution.xx;
    float q = fbm(p - Time * 0.10);
    vec2 r = vec2(
            fbm(p + q + vec2(Time * 0.42) - p.x - p.y),
            fbm(p + q - vec2(Time * 0.31, Time * 0.24))
    );
    float body = fbm(p + r);
    float curl = fbm(p * 1.75 + r.yx * 1.35 + vec2(-Time * 0.18, Time * 0.22));
    float heightFade = 1.0 - clamp(gl_FragCoord.y / resolution.y, 0.0, 1.0);
    float pulse = 0.5 + 0.5 * cos(Time * 1.15 + gl_FragCoord.y / resolution.y * 5.8 + r.x * 3.4);
    float flame = body * 0.50 + r.y * 0.30 + curl * 0.20;

    flame = flame * (0.42 + heightFade * 0.72) + pulse * 0.14;
    return clamp(smoothstep(0.34, 1.12, flame), 0.0, 1.0);
}

void main() {
    float center = sampleMask(texCoord);
    float outlineMask = 0.0;

    for (int d = 0; d < DIRS; d++) {
        float angle = TAU * (float(d) / float(DIRS));
        vec2 dir = vec2(cos(angle), sin(angle));
        float nearby = sampleMask(texCoord + dir * oneTexel * OUTLINE_WIDTH);
        outlineMask = max(outlineMask, max(nearby - center, 0.0));
    }

    outlineMask = clamp(outlineMask, 0.0, 1.0);

    if (center <= 0.001 && outlineMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    float swap = smoothstep(0.12, 0.88, 0.5 + 0.5 * sin(Time * 0.62));
    vec4 baseColor = mix(FirstFillColor, SecondFillColor, swap);
    vec4 flameColor = mix(SecondFillColor, FirstFillColor, swap);

    float flame = flamePattern(texCoord);
    float flicker = 0.82 + 0.18 * sin(Time * 3.6 + flame * 5.0 + texCoord.x * 7.0);
    float flameMask = clamp(flame * flicker, 0.0, 1.0);
    float hotCore = smoothstep(0.62, 1.0, flameMask);
    vec3 baseRgb = baseColor.rgb;
    vec3 flameRgb = clamp(flameColor.rgb * (0.92 + hotCore * 0.28), 0.0, 1.0);
    vec3 fillRgb = mix(baseRgb, flameRgb, clamp(flameMask * 0.92, 0.0, 1.0));

    float outsideMask = 1.0 - smoothstep(0.001, 0.20, center);
    float edgePulse = 0.86 + 0.14 * sin(Time * 3.2 + flame * 5.0);
    float fillAlpha = center * baseColor.a;
    float flameAlpha = center * flameMask * flameColor.a * 0.88;
    float insideAlpha = clamp(fillAlpha + flameAlpha * (1.0 - fillAlpha * 0.45), 0.0, 1.0);
    float outlineAlpha = outlineMask * outsideMask * OutlineColor.a * edgePulse;
    float finalAlpha = clamp(insideAlpha + outlineAlpha, 0.0, 1.0);

    vec3 finalColor = fillRgb * insideAlpha;
    finalColor += OutlineColor.rgb * outlineAlpha;
    finalColor = clamp(finalColor / max(finalAlpha, 0.001), 0.0, 1.0);

    fragColor = vec4(finalColor, finalAlpha);
}

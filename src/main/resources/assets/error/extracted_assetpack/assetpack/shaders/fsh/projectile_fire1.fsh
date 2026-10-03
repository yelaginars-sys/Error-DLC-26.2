#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ZenithData {
    vec4 FireColor;
    vec4 EmberColor;
    float Time;
};

#define oneTexel (1.0 / InSize)

in vec2 texCoord;

out vec4 fragColor;

const float TAU = 6.28318530718;
const float FLAME_HEIGHT_PX = 60.0;
const int FLAME_STEPS = 24;
const int OCTAVES = 5;

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
    float amp = 0.55;
    mat2 rot = mat2(cos(0.6), sin(0.6), -sin(0.6), cos(0.6));
    for (int i = 0; i < OCTAVES; i++) {
        value += amp * noise(st);
        st = rot * st * 2.05 + vec2(50.0);
        amp *= 0.5;
    }
    return value;
}

float sampleMask(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 0.0;
    vec4 c = texture(InSampler, uv);
    return clamp(max(c.a, max(c.r, max(c.g, c.b))), 0.0, 1.0);
}

void main() {
    float center = sampleMask(texCoord);

    vec2 resolution = 1.0 / oneTexel;
    vec2 fragPx = gl_FragCoord.xy;

    vec2 nz = vec2(fragPx.x / resolution.y * 6.0, fragPx.y / resolution.y * 6.0);
    float warpX = (fbm(nz * 1.3 + vec2(Time * 0.7, -Time * 0.4)) - 0.5) * 2.0;

    float flameAccum = 0.0;
    float maxBelow = 0.0;
    float closestBelow = 1.0;

    for (int i = 1; i <= FLAME_STEPS; i++) {
        float k = float(i) / float(FLAME_STEPS);
        float distPx = k * FLAME_HEIGHT_PX;

        float sway = (fbm(vec2(fragPx.x * 0.025 + Time * 1.4, distPx * 0.05)) - 0.5) * distPx * 0.55;
        sway += warpX * distPx * 0.35;

        vec2 samplePx = vec2(fragPx.x + sway, fragPx.y - distPx);
        vec2 sampleUv = samplePx * oneTexel;
        float s = sampleMask(sampleUv);

        if (s > 0.05) {
            float fade = 1.0 - k;
            fade = fade * fade;
            flameAccum = max(flameAccum, s * fade);
            if (k < closestBelow) closestBelow = k;
            maxBelow = max(maxBelow, s);
        }
    }

    float outlineMask = 0.0;
    for (int d = 0; d < 8; d++) {
        float a = TAU * (float(d) / 8.0);
        vec2 dir = vec2(cos(a), sin(a));
        float n = sampleMask(texCoord + dir * oneTexel * 2.0);
        outlineMask = max(outlineMask, max(n - center, 0.0));
    }

    if (center <= 0.001 && flameAccum <= 0.001 && outlineMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 p = fragPx * 8.0 / resolution.yy;
    float q = fbm(p - vec2(0.0, Time * 1.8));
    vec2 r = vec2(
        fbm(p + q + vec2(Time * 0.55, -Time * 0.32)),
        fbm(p + q - vec2(Time * 0.28, Time * 0.61))
    );
    float body = fbm(p + r * 1.4);
    float curl = fbm(p * 1.9 + r.yx * 1.3 + vec2(-Time * 0.22, Time * 0.7));
    float flameNoise = body * 0.5 + r.y * 0.3 + curl * 0.2;

    float heightShape = pow(1.0 - closestBelow, 1.4);
    float flameField = flameAccum * heightShape;
    flameField *= smoothstep(0.05, 0.55, flameNoise + 0.15);

    float flicker = 0.78 + 0.22 * sin(Time * 9.0 + flameNoise * 11.0 + fragPx.x * 0.08);
    flameField *= flicker;

    float spark = smoothstep(0.78, 1.0, fbm(p * 5.0 - vec2(0.0, Time * 3.5)));
    float hotCore = smoothstep(0.4, 1.0, flameField);

    vec3 emberCore = mix(EmberColor.rgb, vec3(1.0), 0.4);
    vec3 fireRgb = mix(FireColor.rgb, emberCore, hotCore);
    fireRgb += EmberColor.rgb * spark * flameField * 1.6;

    float bodyMask = center * (0.4 + flameNoise * 0.6) * flicker;
    float bodyAlpha = bodyMask * FireColor.a;
    float trailAlpha = clamp(flameField, 0.0, 1.0) * FireColor.a * 1.4;
    float edgeAlpha = outlineMask * (0.6 + hotCore * 0.6) * EmberColor.a;

    float finalAlpha = clamp(max(bodyAlpha, max(trailAlpha, edgeAlpha)), 0.0, 1.0);

    vec3 finalColor = fireRgb * (bodyAlpha + trailAlpha);
    finalColor += EmberColor.rgb * edgeAlpha;
    finalColor = clamp(finalColor / max(finalAlpha, 0.001), 0.0, 1.2);

    fragColor = vec4(finalColor, finalAlpha);
}

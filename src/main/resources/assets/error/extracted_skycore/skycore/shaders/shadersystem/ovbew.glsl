#version 150 core

in vec2 fragCoord;

uniform sampler2D uTex;
uniform vec2 texelSize;
uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
uniform float scale;
uniform float outline;
uniform float glow;
uniform float fill;
uniform float alpha;
uniform float glowPass;

out vec4 fragColor;

float hash21(vec2 value) {
    value = fract(value * vec2(123.34, 345.45));
    value += dot(value, value + 34.345);
    return fract(value.x * value.y);
}

float noise(vec2 value) {
    vec2 cell = floor(value);
    vec2 local = fract(value);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = p * 2.05 + vec2(7.1, 3.8);
        amplitude *= 0.5;
    }
    return value;
}

float maskAlpha(vec2 uv) {
    return texture(uTex, uv).a;
}

float maskSolid(vec2 uv) {
    float center = maskAlpha(uv);
    float dilated = center;
    dilated = max(dilated, maskAlpha(uv + vec2(texelSize.x, 0.0)));
    dilated = max(dilated, maskAlpha(uv - vec2(texelSize.x, 0.0)));
    dilated = max(dilated, maskAlpha(uv + vec2(0.0, texelSize.y)));
    dilated = max(dilated, maskAlpha(uv - vec2(0.0, texelSize.y)));
    dilated = max(dilated, maskAlpha(uv + vec2(texelSize.x, texelSize.y)));
    dilated = max(dilated, maskAlpha(uv + vec2(-texelSize.x, texelSize.y)));
    dilated = max(dilated, maskAlpha(uv + vec2(texelSize.x, -texelSize.y)));
    dilated = max(dilated, maskAlpha(uv + vec2(-texelSize.x, -texelSize.y)));
    return dilated;
}

float edgeStrength(vec2 uv, float s) {
    float a0 = maskSolid(uv);
    float ax1 = maskSolid(uv + vec2(texelSize.x * s, 0.0));
    float ax2 = maskSolid(uv - vec2(texelSize.x * s, 0.0));
    float ay1 = maskSolid(uv + vec2(0.0, texelSize.y * s));
    float ay2 = maskSolid(uv - vec2(0.0, texelSize.y * s));
    float edge = abs(a0 - ax1) + abs(a0 - ax2) + abs(a0 - ay1) + abs(a0 - ay2);

    float d1 = maskSolid(uv + vec2(texelSize.x * s, texelSize.y * s));
    float d2 = maskSolid(uv + vec2(-texelSize.x * s, texelSize.y * s));
    float d3 = maskSolid(uv + vec2(texelSize.x * s, -texelSize.y * s));
    float d4 = maskSolid(uv + vec2(-texelSize.x * s, -texelSize.y * s));
    edge += 0.7 * (abs(a0 - d1) + abs(a0 - d2) + abs(a0 - d3) + abs(a0 - d4));
    return clamp(edge * 1.95, 0.0, 1.0);
}

float outerHalo(vec2 uv, float width) {
    if (maskSolid(uv) > 0.02) {
        return 0.0;
    }
    float halo = 0.0;
    float spread = clamp(width, 1.0, 8.0);
    for (int i = 1; i <= 6; i++) {
        float dist = float(i) * spread * 0.42;
        float falloff = exp(-float(i) * 0.58);
        halo = max(halo, maskSolid(uv + vec2(texelSize.x * dist, 0.0)) * falloff);
        halo = max(halo, maskSolid(uv - vec2(texelSize.x * dist, 0.0)) * falloff);
        halo = max(halo, maskSolid(uv + vec2(0.0, texelSize.y * dist)) * falloff);
        halo = max(halo, maskSolid(uv - vec2(0.0, texelSize.y * dist)) * falloff);
        halo = max(halo, maskSolid(uv + vec2(texelSize.x * dist * 0.7, texelSize.y * dist * 0.7)) * falloff * 0.85);
        halo = max(halo, maskSolid(uv + vec2(-texelSize.x * dist * 0.7, texelSize.y * dist * 0.7)) * falloff * 0.85);
    }
    return halo;
}

vec3 samplePlasma(vec2 uv, float t) {
    float scl = mix(4.5, 9.5, clamp(scale / 4.0, 0.0, 1.0));
    vec2 position = uv * vec2(scl * 0.85, scl * 1.35);

    float firstWarp = noise(position * 0.55 + vec2(t * 0.45, -t * 0.60));
    float secondWarp = noise(position * 1.25 + firstWarp * 2.2 + vec2(-t * 0.35, t * 0.50));
    float value = noise(position + secondWarp * 2.4 + vec2(t * 0.20, -t * 0.70));

    float mist = fbm(position * 0.62 + vec2(t * 0.18, -t * 0.12) + secondWarp);
    float energy = smoothstep(0.12, 0.96, mix(value, mist, 0.42));
    float vein = pow(energy, 2.35);
    float spark = pow(clamp(noise(position * 3.4 - vec2(t * 0.9, t * 0.55)), 0.0, 1.0), 8.0);

    vec3 deep = mix(color * 0.28, color2 * 0.55, 0.35);
    vec3 mid = mix(color, color2, 0.45);
    vec3 hot = mix(color * 1.55, vec3(1.0), 0.28);
    vec3 plasma = mix(deep, mid, energy);
    plasma = mix(plasma, hot, vein * 0.85);
    plasma += color * vein * 0.75;
    plasma += mix(color, vec3(1.0), 0.45) * spark * 0.55;
    plasma *= 0.94 + 0.06 * sin(t * 1.8 + energy * 6.0);
    return clamp(plasma, 0.0, 1.0);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float t = time * max(speed, 0.001);

    if (glowPass > 0.5) {
        float near = maskAlpha(uv);
        near = max(near, maskAlpha(uv + vec2(texelSize.x, 0.0)));
        near = max(near, maskAlpha(uv - vec2(texelSize.x, 0.0)));
        near = max(near, maskAlpha(uv + vec2(0.0, texelSize.y)));
        near = max(near, maskAlpha(uv - vec2(0.0, texelSize.y)));
        if (near <= 0.02) {
            discard;
        }

        float edge = edgeStrength(uv, outline);
        float halo = outerHalo(uv, outline) * 0.85;
        float glowAmt = max(pow(edge, 0.52), halo);
        if (glowAmt < 0.05) {
            discard;
        }
        glowAmt = min(glowAmt * glow, 1.0);
        float pulse = 0.92 + 0.08 * sin(t * 2.2);
        vec3 neon = mix(mix(color, color2, 0.4), vec3(1.0), 0.22) * glowAmt * pulse;
        float outA = min(glowAmt * alpha * 0.55, 0.55);
        fragColor = vec4(clamp(neon, 0.0, 1.0), outA);
        return;
    }

    float mask = maskSolid(uv);
    if (mask <= 0.02) {
        discard;
    }

    float edge = edgeStrength(uv, outline);
    float edgeBand = smoothstep(0.02, 0.48, edge);
    vec3 plasma = samplePlasma(uv, t);

    float inner = clamp(mask - edgeBand * 0.55, 0.0, 1.0);
    float fillStrength = fill * inner * (0.34 + plasma.r * 0.22 + plasma.g * 0.18 + plasma.b * 0.20);
    fillStrength = clamp(fillStrength, 0.0, 1.2);

    vec3 outlineCol = mix(color, color2, 0.35);
    outlineCol = mix(outlineCol, vec3(1.0), 0.18);
    float edgeStrengthVal = edgeBand * (0.42 + glow * 0.18);

    vec3 rgb = plasma * fillStrength + outlineCol * edgeStrengthVal;
    float outA = clamp(alpha * (fillStrength * 0.95 + edgeBand * 0.55) * mask, 0.0, 1.0);
    if (outA <= 0.001) {
        discard;
    }
    fragColor = vec4(clamp(rgb, 0.0, 1.0), outA);
}

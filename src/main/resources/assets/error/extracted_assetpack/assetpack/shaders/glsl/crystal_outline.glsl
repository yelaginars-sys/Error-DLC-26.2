#version 150 core

in vec2 fragCoord;

uniform sampler2D originalTexture;
uniform vec2 texelSize;
uniform vec2 screenSize;
uniform vec3 accent;
uniform float time;
uniform float speed;
uniform float alpha;
uniform float shaderFill;
uniform float outline;
uniform float outlineAlpha;
uniform float outlineWidth;
uniform float outlineStrength;
uniform float glowPass;

out vec4 fragColor;

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0, 0.0)), u.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.07 + vec2(2.1, 5.3);
        a *= 0.5;
    }
    return v;
}

float nebulaDensity(vec2 p, float t) {
    vec2 warp = vec2(
        fbm(p + vec2(0.0, t * 0.12)),
        fbm(p + vec2(5.2, -t * 0.10))
    );
    return fbm(p + 2.2 * warp);
}

vec3 nebulaPalette(float t, vec3 acc) {
    float m = max(max(acc.r, acc.g), max(acc.b, 0.001));
    vec3 base = acc / m;
    vec3 deep = base * vec3(0.45, 0.45, 0.70);
    vec3 c1 = deep * 0.020;
    vec3 c2 = base * 0.110;
    vec3 c3 = base * 0.380;
    vec3 c4 = base * 0.900;
    vec3 c5 = mix(base, vec3(1.0), 0.55);
    t = clamp(t, 0.0, 1.0);
    if (t < 0.18) return mix(c1, c2, t / 0.18);
    if (t < 0.38) return mix(c2, c3, (t - 0.18) / 0.20);
    if (t < 0.60) return mix(c3, c4, (t - 0.38) / 0.22);
    if (t < 0.82) return mix(c4, c5, (t - 0.60) / 0.22);
    return mix(c5, vec3(1.0), (t - 0.82) / 0.18);
}

float starField(vec2 uv, float density, float t) {
    vec2 g = floor(uv);
    vec2 f = fract(uv) - 0.5;
    float h = hash21(g);
    if (h < density) {
        return 0.0;
    }
    float tw = 0.35 + 0.65 * sin(t * 2.4 + h * 47.0);
    return smoothstep(0.30, 0.0, length(f)) * tw * smoothstep(density, 1.0, h);
}

float maskAlpha(vec2 uv) {
    // Only trust alpha from the dedicated crystal mask FBO.
    // Luminance fallback treated fire/hands/particles as crystal and leaked the effect.
    vec4 tex = texture(originalTexture, uv);
    return tex.a > 0.05 ? tex.a : 0.0;
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

float edgeStrength(vec2 uv, float scale) {
    float a0 = maskSolid(uv);
    float ax1 = maskSolid(uv + vec2(texelSize.x * scale, 0.0));
    float ax2 = maskSolid(uv - vec2(texelSize.x * scale, 0.0));
    float ay1 = maskSolid(uv + vec2(0.0, texelSize.y * scale));
    float ay2 = maskSolid(uv - vec2(0.0, texelSize.y * scale));
    float edge = abs(a0 - ax1) + abs(a0 - ax2) + abs(a0 - ay1) + abs(a0 - ay2);
    return clamp(edge * 1.8, 0.0, 1.0);
}

float outerHalo(vec2 uv, float width) {
    if (maskSolid(uv) > 0.02) {
        return 0.0;
    }
    float halo = 0.0;
    float spread = clamp(width, 1.0, 6.0);
    for (int i = 1; i <= 5; i++) {
        float dist = float(i) * spread * 0.45;
        float falloff = exp(-float(i) * 0.65);
        halo = max(halo, maskSolid(uv + vec2(texelSize.x * dist, 0.0)) * falloff);
        halo = max(halo, maskSolid(uv - vec2(texelSize.x * dist, 0.0)) * falloff);
        halo = max(halo, maskSolid(uv + vec2(0.0, texelSize.y * dist)) * falloff);
        halo = max(halo, maskSolid(uv - vec2(0.0, texelSize.y * dist)) * falloff);
    }
    return halo;
}

vec3 sampleCosmos(vec2 sUv, float t) {
    vec2 p = sUv * 4.5;
    float n1 = nebulaDensity(p, t);
    float n2 = nebulaDensity(p * 1.8 + 11.3, -t * 0.6);
    float n = mix(n1, n2, 0.45);
    float d = smoothstep(0.18, 0.95, n);
    d = pow(d, 1.05);
    d *= 1.0 - smoothstep(0.35, 0.05, n) * 0.85;

    vec3 col = nebulaPalette(d, accent);
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.22);
    col += nebulaPalette(min(1.0, d + 0.10), accent) * smoothstep(0.58, 0.92, d) * 0.52;
    col += vec3(1.0) * smoothstep(0.85, 0.99, d) * 0.38;

    float maxA = max(max(accent.r, accent.g), max(accent.b, 0.001));
    vec3 accN = accent / maxA;
    float stars = starField(sUv * 80.0, 0.958, t) * 1.5 + starField(sUv * 36.0 + 7.0, 0.982, t * 0.7) * 2.3;
    col += mix(vec3(1.0), accN, 0.12) * stars;
    col *= 0.94 + 0.06 * sin(t * 1.3 + n * 5.0);
    return col / (1.0 + col * 0.22) * 1.12;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);

    if (glowPass > 0.5) {
        if (outline < 0.5) {
            discard;
        }
        float near = maskAlpha(uv);
        near = max(near, maskAlpha(uv + vec2(texelSize.x, 0.0)));
        near = max(near, maskAlpha(uv - vec2(texelSize.x, 0.0)));
        near = max(near, maskAlpha(uv + vec2(0.0, texelSize.y)));
        near = max(near, maskAlpha(uv - vec2(0.0, texelSize.y)));
        if (near <= 0.02) {
            discard;
        }
        float edge = edgeStrength(uv, outlineWidth);
        float halo = outerHalo(uv, outlineWidth) * 0.75;
        float glow = max(pow(edge, 0.58), halo);
        if (glow < 0.06) {
            discard;
        }
        glow = min(glow * outlineStrength, 1.0);
        float pulse = 0.94 + 0.06 * sin(time * speed * 2.0);
        vec3 neon = mix(accent, vec3(1.0), 0.22) * glow * pulse;
        float outA = min(glow * outlineAlpha * 0.42, 0.42);
        fragColor = vec4(clamp(neon, 0.0, 1.0), outA);
        return;
    }

    float m = maskSolid(uv);
    if (m <= 0.02) {
        discard;
    }

    vec2 sRes = max(screenSize, vec2(1.0));
    vec2 sUv = fragCoord;
    sUv.x *= sRes.x / sRes.y;
    vec3 col = shaderFill > 0.5 ? sampleCosmos(sUv, time * speed) : accent;
    float outA = min(alpha * m * 0.9, 0.92);
    fragColor = vec4(clamp(col, 0.0, 1.0), outA);
}

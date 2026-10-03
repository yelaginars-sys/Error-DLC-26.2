#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D maskTexture;
uniform sampler2D glowTexture;

uniform vec4 espColor;
uniform vec2 texelSize;
uniform float fillAlpha;
uniform float glowIntensity;
uniform float outlineWidth;
uniform int enableFill;
uniform int enableOuter;
uniform int enableInner;

float maskValue(sampler2D tex, vec2 uv) {
    vec4 c = texture(tex, uv);
    return max(c.a, max(c.r, max(c.g, c.b)));
}

float sampleMask(vec2 uv, vec2 offset) {
    return maskValue(maskTexture, uv + vec2(offset.x, -offset.y));
}

/**
 * Soft circular rim. outside=true → only outside silhouette, else only inside.
 */
float softRim(vec2 uv, float center, float radius, bool outside) {
    if (radius < 0.5) {
        return 0.0;
    }

    if (outside) {
        if (center > 0.18) {
            return 0.0;
        }
    } else {
        if (center < 0.18) {
            return 0.0;
        }
    }

    float best = 0.0;
    float r = clamp(radius, 1.0, 5.0);
    int lim = int(ceil(r));

    for (int x = -5; x <= 5; x++) {
        for (int y = -5; y <= 5; y++) {
            if (x == 0 && y == 0) {
                continue;
            }
            float dist = length(vec2(float(x), float(y)));
            if (dist > r + 0.4) {
                continue;
            }

            float sampleV = sampleMask(uv, vec2(float(x), float(y)) * texelSize);
            bool hit = outside ? (sampleV > 0.18) : (sampleV < 0.18);
            if (!hit) {
                continue;
            }

            // Thin band near the edge distance.
            float band = 1.0 - smoothstep(r * 0.25, r + 0.35, dist);
            float edge = outside
                ? (1.0 - smoothstep(0.0, 0.35, center))
                : smoothstep(0.15, 0.55, center);
            best = max(best, band * edge);
        }
    }

    return pow(clamp(best, 0.0, 1.0), 0.75);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);

    float sharp = maskValue(maskTexture, uv);
    float blurred = maskValue(glowTexture, uv);

    // Controlled fill — readable, not a fog slab.
    float fill = 0.0;
    if (enableFill != 0) {
        fill = smoothstep(0.05, 0.50, sharp) * clamp(fillAlpha, 0.0, 0.70) * 0.85;
    }

    // Dual outline rings.
    float outerLine = 0.0;
    float innerLine = 0.0;
    if (enableOuter != 0) {
        outerLine = softRim(uv, sharp, outlineWidth, true) * 0.95;
    }
    if (enableInner != 0) {
        innerLine = softRim(uv, sharp, outlineWidth * 0.85, false) * 0.80;
    }
    float outline = max(outerLine, innerLine);

    // Tight supportive glow — accents the outline, does not dump bloom.
    float outerGlow = max(blurred - sharp, 0.0);
    float glow = pow(smoothstep(0.02, 0.55, outerGlow), 1.55);
    glow *= clamp(glowIntensity, 0.15, 1.8) * 0.45;
    // Kill glow under strong outline so the rim stays clean.
    glow *= 1.0 - outline * 0.75;
    glow = min(glow, 0.55);

    float alpha = max(outline, max(fill, glow));
    if (alpha < 0.015) {
        discard;
    }

    vec3 base = espColor.rgb;
    vec3 rimCol = mix(base, vec3(1.0), 0.35);
    vec3 fillCol = mix(base * 0.78, base, 0.55);
    vec3 glowCol = mix(base, rimCol, 0.25);

    vec3 col = fillCol;
    float weight = fill;

    if (glow >= weight) {
        col = glowCol;
        weight = glow;
    }
    if (outline >= weight * 0.92) {
        // Outer rim slightly brighter than inner.
        col = mix(rimCol, vec3(1.0), outerLine > innerLine ? 0.12 : 0.0);
        weight = outline;
        alpha = max(alpha, outline * 0.98);
    }

    float baseA = clamp(espColor.a, 0.60, 1.0);
    fragColor = vec4(col, baseA * alpha);
}

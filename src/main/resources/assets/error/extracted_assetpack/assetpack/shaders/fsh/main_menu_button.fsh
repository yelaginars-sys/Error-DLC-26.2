#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 location;
uniform vec2 rectSize;
uniform float radius;
uniform float hover;
uniform vec4 accentColor;
uniform vec4 accentColor2;

float roundSDF(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

float edgeLine(float coord, float center, float width) {
    return 1.0 - smoothstep(0.0, width, abs(coord - center));
}

void main() {
    vec2 rectHalf = rectSize * 0.5;
    vec2 p = rectHalf - (texCoord * rectSize);
    float dist = roundSDF(p, rectHalf - radius - 1.0, radius);

    float fillMask = 1.0 - smoothstep(-0.5, 0.5, dist);
    if (fillMask <= 0.001) {
        discard;
    }

    vec3 accentA = max(accentColor.rgb, vec3(0.08));
    vec3 accentB = max(accentColor2.rgb, vec3(0.08));
    vec3 rimColor = mix(accentA, accentB, 0.38);
    vec3 outlineColor = mix(accentA, accentB, 0.22);

    vec3 idleFillTop = vec3(0.048, 0.050, 0.062);
    vec3 idleFillBottom = vec3(0.030, 0.032, 0.042);
    vec3 idleFill = mix(idleFillBottom, idleFillTop, texCoord.y);

    vec3 activeFillTop = mix(accentA * 0.20, accentB * 0.15, 0.30);
    vec3 activeFillBottom = mix(accentA * 0.09, accentB * 0.07, 0.30);
    vec3 activeFill = mix(activeFillBottom, activeFillTop, pow(texCoord.y, 0.78));

    vec3 fillRgb = mix(idleFill, activeFill, hover);
    float fillAlpha = mix(0.68, 0.92, hover) * fillMask;

    float cornerGuard = smoothstep(0.0, radius / max(rectSize.x, 1.0) + 0.10, min(texCoord.x, 1.0 - texCoord.x));
    float topRim = edgeLine(texCoord.y, 1.0, 0.020) * cornerGuard;
    float bottomRim = edgeLine(texCoord.y, 0.0, 0.020) * cornerGuard;
    float idleRim = (topRim + bottomRim) * (1.0 - hover);
    float idleRimStrength = idleRim * 0.88;
    float idleOuterGlow = exp(-max(dist, 0.0) * 0.70) * 0.11 * (1.0 - hover);

    float activeRing = smoothstep(1.7, 0.15, dist) * smoothstep(-1.3, -0.20, dist) * hover;
    float activeBloom = exp(-max(dist, 0.0) * 0.42) * 0.30 * hover;
    float insideDist = max(-dist, 0.0);
    float activeInnerGlow = exp(-insideDist * 0.26) * hover * 0.30;

    vec3 color = fillRgb * fillAlpha;
    color += rimColor * idleRimStrength;
    color += outlineColor * idleOuterGlow;
    color += outlineColor * (activeRing * 0.95 + activeBloom);
    color += outlineColor * activeInnerGlow;

    float alpha = fillAlpha;
    alpha += idleRimStrength * 0.44;
    alpha += idleOuterGlow * 0.55;
    alpha += (activeRing + activeBloom) * hover * 0.52;
    alpha = clamp(alpha, 0.0, 1.0) * fillMask;

    fragColor = vec4(color, alpha);
}

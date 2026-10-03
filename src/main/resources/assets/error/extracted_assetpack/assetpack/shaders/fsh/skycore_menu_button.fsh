#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 size;
uniform vec4 round;
uniform vec2 smoothness;
uniform vec4 bgColor1;
uniform vec4 bgColor2;
uniform vec4 bgColor3;
uniform vec4 bgColor4;
uniform vec4 borderColor;
uniform float borderWidth;
uniform float glowIntensity;
uniform float glowRadius;
uniform vec2 clickPoint;
uniform float revealRadius;
uniform float revealSoftness;
uniform vec4 revealColor;
uniform float revealAlpha;

float roundedSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float aaStep(float edge, float x) {
    float w = 0.7 * length(vec2(dFdx(x), dFdy(x)));
    return smoothstep(edge - w, edge + w, x);
}

void main() {
    vec2 uv = texCoord;
    vec2 center = uv * size - size * 0.5;

    float dist = roundedSDF(center, size * 0.5, round);

    float bodyAlpha = 1.0 - aaStep(0.0, dist);

    float innerEdge = dist + borderWidth;
    float borderMask = (1.0 - aaStep(0.0, dist)) * aaStep(0.0, innerEdge);

    float glowDist = max(0.0, dist);
    float safeGlowRadius = max(glowRadius, 0.001);
    float glow = exp(-glowDist * glowDist / (safeGlowRadius * safeGlowRadius)) * glowIntensity;
    glow *= (1.0 - aaStep(0.0, -dist + borderWidth * 0.5));

    vec4 colTop = mix(bgColor1, bgColor4, uv.x);
    vec4 colBot = mix(bgColor2, bgColor3, uv.x);
    vec4 bg = mix(colTop, colBot, uv.y);

    vec2 pixel = uv * size;
    float revealDist = distance(pixel, clickPoint);
    float safeRevealSoftness = max(revealSoftness, 0.001);
    float revealMask = 1.0 - smoothstep(revealRadius - safeRevealSoftness, revealRadius + safeRevealSoftness, revealDist);
    revealMask *= bodyAlpha * revealAlpha;
    float edgeMask = 1.0 - smoothstep(0.0, safeRevealSoftness, abs(revealDist - revealRadius));
    edgeMask *= bodyAlpha * revealAlpha * smoothstep(0.0, safeRevealSoftness, revealRadius);

    bg.rgb = mix(bg.rgb, revealColor.rgb, revealMask * revealColor.a);
    bg.rgb += revealColor.rgb * edgeMask * 0.28;
    bg.a = max(bg.a, revealColor.a * revealMask);

    vec4 finalColor = vec4(bg.rgb, bg.a * bodyAlpha);

    finalColor.rgb = mix(finalColor.rgb, borderColor.rgb, borderMask * borderColor.a);
    finalColor.a = max(finalColor.a, borderMask * borderColor.a);

    finalColor.rgb += borderColor.rgb * glow;
    finalColor.a = max(finalColor.a, glow * borderColor.a);

    if (finalColor.a <= 0.001) {
        discard;
    }
    fragColor = finalColor;
}

#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 location, rectSize;
uniform vec4 radii;
uniform vec4 colorA;
uniform vec4 colorB;
uniform float rotation;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 rectHalf = rectSize * 0.5;
    vec2 pos = texCoord * rectSize - rectHalf;
    vec4 r = vec4(radii.z, radii.y, radii.w, radii.x);
    float dist = roundedBoxSDF(pos, rectHalf - 1.0, r);

    // Harmony GradientBar: even dark↔bright waves that travel along X
    float normalizedX = texCoord.x;
    float gradientPos = normalizedX + rotation / 6.28318;
    float gradient = (sin(gradientPos * 6.28318 * 2.0) + 1.0) * 0.5;
    vec4 finalColor = mix(colorA, colorB, gradient);

    float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, dist)) * finalColor.a;
    fragColor = vec4(finalColor.rgb, smoothedAlpha);
}

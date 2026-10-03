#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 location, rectSize;
uniform vec4 color;
uniform vec4 outlineColor;
// Java BorderRadius: (topLeft, topRight, bottomRight, bottomLeft)
uniform vec4 radii;
uniform float thickness;
uniform vec2 smoothness;
uniform float flowPhase;
uniform float flowBands;
uniform float flowMinAlpha;

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
    float t = max(thickness, 0.35);
    float edge = smoothstep(-t, 0.0, dist) - smoothstep(0.0, 1.0, dist);

    // Traveling alpha along the perimeter (angular), white peaks + translucent valleys.
    float angle = atan(pos.y, pos.x);
    float progress = fract(angle * 0.15915494309 + 0.5 + flowPhase);
    float bands = max(flowBands, 1.0);
    float wave = 0.5 + 0.5 * cos(progress * 6.28318530718 * bands);
    float soft = smoothstep(0.15, 0.85, wave);
    float alphaMod = mix(clamp(flowMinAlpha, 0.05, 1.0), 1.0, soft);

    fragColor = vec4(outlineColor.rgb, outlineColor.a * edge * alphaMod);
}

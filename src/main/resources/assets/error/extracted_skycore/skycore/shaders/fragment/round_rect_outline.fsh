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
    fragColor = vec4(outlineColor.rgb, outlineColor.a * edge);
}


/**
 * Computes a Signed Distance Function (SDF) for a rectangle with
 * independent corner radii.
 *
 * @param CenterPosition - point relative to the rectangle center.
 * @param Size - half width and height of the rectangle (center to edge).
 * @param Radius - corner radii in this order:
 *                 (top-left, top-right, bottom-right, bottom-left).
 *
 * @return float - distance from the point to the nearest rectangle surface.
 *                 Negative inside, positive outside.
 */
float roundedBoxSDF(vec2 CenterPosition, vec2 Size, vec4 Radius) {

    vec2 halfSize = Size;
    Radius = min(Radius, vec4(halfSize.x, halfSize.y, halfSize.x, halfSize.y));


    Radius.xy = (CenterPosition.x > 0.0) ? Radius.xy : Radius.zw;
    Radius.x  = (CenterPosition.y > 0.0) ? Radius.x  : Radius.y;


    vec2 q = abs(CenterPosition) - Size + Radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - Radius.x;
}

/**
 * Vertices go clockwise:
 * 0 — top-left (0, 0)
 * 1 — bottom-left (0, 1)
 * 2 — bottom-right (1, 1)
 * 3 — top-right (1, 0)
 */
const vec2[4] RECT_VERTICES_COORDS = vec2[] (
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

vec2 rvertexcoord(int id) {
    return RECT_VERTICES_COORDS[id % 4];
}

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

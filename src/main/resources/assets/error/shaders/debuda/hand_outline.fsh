#version 150

// Чёткая обводка контура руки (тумблер «Обводка»).
// Маска силуэта — по глубине (Sampler0 = depth-attachment). Расширенная маска
// (max соседей в радиусе Thickness) минус сам пиксель = кольцо по краю силуэта.

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Texel;        // 1.0 / resolution
uniform vec3 OutlineColor;
uniform float Thickness;   // толщина обводки в пикселях

out vec4 OutColor;

float hmask(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return step(0.01, max(c.a, max(c.r, max(c.g, c.b))));
}

void main() {
    vec2 uv = TexCoord;
    float c = hmask(uv);

    float probe = c;
    probe = max(probe, hmask(uv + vec2(0.0,  Thickness * Texel.y)));
    probe = max(probe, hmask(uv + vec2(0.0, -Thickness * Texel.y)));
    probe = max(probe, hmask(uv + vec2( Thickness * Texel.x, 0.0)));
    probe = max(probe, hmask(uv + vec2(-Thickness * Texel.x, 0.0)));
    if (probe <= 0.01) discard;

    float maxA = 0.0;
    const int DIRS = 16;
    for (int i = 0; i < DIRS; i++) {
        float ang = 6.2831 * float(i) / float(DIRS);
        vec2 dir = vec2(cos(ang), sin(ang));
        maxA = max(maxA, hmask(uv + dir * Texel * Thickness));
    }

    float ring = clamp(maxA - c, 0.0, 1.0);
    if (ring <= 0.01) discard;

    OutColor = vec4(OutlineColor, ring);
}

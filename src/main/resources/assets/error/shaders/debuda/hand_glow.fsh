#version 150

// Мягкое внешнее свечение по силуэту (тумблер «Свечение»).
// Маска силуэта — по глубине (Sampler0 = depth-attachment). Для пикселя берём
// максимум маски в кольце радиуса Radius с затуханием — ореол наружу. Аддитивно.

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Texel;       // 1.0 / resolution
uniform vec3 GlowColor;
uniform float Radius;     // радиус свечения в пикселях
uniform float Intensity;

out vec4 OutColor;

float hmask(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return step(0.01, max(c.a, max(c.r, max(c.g, c.b))));
}

void main() {
    vec2 uv = TexCoord;
    float center = hmask(uv);

    float probe = center;
    probe = max(probe, hmask(uv + vec2(0.0,  Radius * Texel.y)));
    probe = max(probe, hmask(uv + vec2(0.0, -Radius * Texel.y)));
    probe = max(probe, hmask(uv + vec2( Radius * Texel.x, 0.0)));
    probe = max(probe, hmask(uv + vec2(-Radius * Texel.x, 0.0)));
    if (probe <= 0.003) discard;

    float g = 0.0;
    const int DIRS = 12;
    for (int i = 0; i < DIRS; i++) {
        float ang = 6.2831 * float(i) / float(DIRS);
        vec2 dir = vec2(cos(ang), sin(ang));
        for (int r = 1; r <= 4; r++) {
            float dist = float(r) / 4.0;
            float a = hmask(uv + dir * Texel * Radius * dist);
            g = max(g, a * (1.0 - dist));
        }
    }

    // только ореол СНАРУЖИ силуэта (внутри заливка и так есть)
    float glow = g * (1.0 - center) * Intensity;
    if (glow <= 0.003) discard;

    OutColor = vec4(GlowColor, glow);
}

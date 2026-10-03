#version 150

// Заливка силуэта руки сплошным цветом (стили «Стекло»/«Заливка»).
// Маска силуэта — по ГЛУБИНЕ захваченной руки (Sampler0 = depth-attachment):
// depth < 1.0 там, где нарисована рука/предмет. Надёжнее alpha (которую пайплайн
// 1.21.4 в кастомный FBO может не писать).

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec3 Color;
uniform float Opacity;

out vec4 OutColor;

float hmask(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return step(0.01, max(c.a, max(c.r, max(c.g, c.b))));
}

void main() {
    float a = hmask(TexCoord);
    if (a <= 0.003) discard;
    OutColor = vec4(Color, a * Opacity);
}

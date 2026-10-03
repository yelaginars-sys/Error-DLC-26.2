#version 150

// Ambilance — апскейл неба, посчитанного в уменьшенном буфере (настройка «Качество»).
// Тяжёлый шейдер неба рисуется в FBO размером экран/2 или экран/4, а этот проход
// растягивает результат на весь кадр. Фильтрация GL_LINEAR выставлена на текстуре
// FBO, поэтому здесь достаточно одной выборки.

in vec2 TexCoord;
out vec4 OutColor;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;   // RenderSystem.setShaderColor; в Java всегда 1,1,1,1

void main() {
    OutColor = texture(Sampler0, TexCoord) * ColorModulator;
}

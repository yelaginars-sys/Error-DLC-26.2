#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform float Intensity;

out vec4 OutColor;

void main() {
    vec4 c = texture(Sampler0, TexCoord);
    // Премультиплированный вывод: blend режим в Java выставлен на (ONE, ONE_MINUS_SRC_ALPHA),
    // поэтому RGB должны быть pre-multiplied alpha. Также усиливаем по Intensity.
    OutColor = vec4(c.rgb * Intensity, c.a);
}

#version 150 core
in vec2 fragCoord;
uniform sampler2D font;
uniform vec4 inColor;
uniform float width;
uniform float maxWidth;
out vec4 fragColor;
void main() {
    float f = clamp(smoothstep(0.5, 1.0, 1.0 - (gl_FragCoord.x - maxWidth) / width), 0.0, 1.0);
    vec4 color = texture(font, fragCoord);
    if (color.a > 0.0) {
        color.a = color.a * f;
    }
    fragColor = color * inColor;
}

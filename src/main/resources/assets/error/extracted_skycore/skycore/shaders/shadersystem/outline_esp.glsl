#version 150 core
in vec2 fragCoord;
uniform sampler2D uTexIn;
uniform vec2 texelSize;
uniform vec2 direction;
uniform vec4 color;
uniform float radius;
uniform float weights[32];
out vec4 fragColor;
void main() {
    vec2 uv = fragCoord;
    float a = texture(uTexIn, uv).a * weights[0];
    for (int i = 1; i < 32; i++) {
        float fi = float(i);
        if (fi > radius) break;
        a += texture(uTexIn, uv + (direction * texelSize * fi)).a * weights[i];
        a += texture(uTexIn, uv - (direction * texelSize * fi)).a * weights[i];
    }
    fragColor = vec4(color.rgb, color.a * a);
}

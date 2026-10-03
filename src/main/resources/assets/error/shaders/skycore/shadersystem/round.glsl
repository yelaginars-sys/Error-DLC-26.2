#version 150 core
in vec2 fragCoord;
uniform vec2 size;
uniform vec4 color;
out vec4 fragColor;
void main() {
    vec2 p = fragCoord * size - size * 0.5;
    float r = min(size.x, size.y) * 0.5;
    float d = length(p) - r;
    float alpha = (1.0 - smoothstep(0.0, 1.0, d)) * color.a;
    fragColor = vec4(color.rgb, alpha);
}

#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 location, rectSize;
uniform vec4 color;
uniform float radius;

void main() {
    vec2 center = rectSize * 0.5;
    vec2 p = texCoord * rectSize - center;
    float dist = length(p) - radius;
    float smoothedAlpha = (1.0 - smoothstep(-1.0, 0.0, dist)) * color.a;
    fragColor = vec4(color.rgb, smoothedAlpha);
}

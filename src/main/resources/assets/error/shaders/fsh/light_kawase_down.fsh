#version 150

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    vec2 uv = fragCoord;
    vec2 halfpixel = resolution * offset;
    vec3 sum = texture(image, uv).rgb * 4.0;
    sum += texture(image, uv - halfpixel).rgb;
    sum += texture(image, uv + halfpixel).rgb;
    sum += texture(image, uv + vec2( halfpixel.x, -halfpixel.y)).rgb;
    sum += texture(image, uv - vec2( halfpixel.x, -halfpixel.y)).rgb;
    fragColor = vec4(sum / 8.0, 1.0);
}
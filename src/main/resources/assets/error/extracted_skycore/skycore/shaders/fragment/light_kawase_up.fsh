#version 150

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    vec2 uv = fragCoord;
    vec2 halfpixel = resolution * offset;

    vec3 sum = texture(image, uv + vec2(-halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture(image, uv + vec2(-halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture(image, uv + vec2(0.0, halfpixel.y * 2.0)).rgb;
    sum += texture(image, uv + vec2(halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture(image, uv + vec2(halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture(image, uv + vec2(halfpixel.x, -halfpixel.y)).rgb * 2.0;
    sum += texture(image, uv + vec2(0.0, -halfpixel.y * 2.0)).rgb;
    sum += texture(image, uv + vec2(-halfpixel.x, -halfpixel.y)).rgb * 2.0;

    fragColor = vec4(sum / 12.0, 1.0);
}
#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec2 halfpixel = resolution * max(offset, 0.35);

    // Dual-filter upsample — even radial bloom without blocky diamonds.
    vec4 sum = texture(image, uv + vec2(-halfpixel.x * 2.0, 0.0));
    sum += texture(image, uv + vec2(-halfpixel.x, halfpixel.y)) * 2.0;
    sum += texture(image, uv + vec2(0.0, halfpixel.y * 2.0));
    sum += texture(image, uv + vec2(halfpixel.x, halfpixel.y)) * 2.0;
    sum += texture(image, uv + vec2(halfpixel.x * 2.0, 0.0));
    sum += texture(image, uv + vec2(halfpixel.x, -halfpixel.y)) * 2.0;
    sum += texture(image, uv + vec2(0.0, -halfpixel.y * 2.0));
    sum += texture(image, uv + vec2(-halfpixel.x, -halfpixel.y)) * 2.0;
    sum += texture(image, uv) * 2.0;

    fragColor = sum * (1.0 / 14.0);
}

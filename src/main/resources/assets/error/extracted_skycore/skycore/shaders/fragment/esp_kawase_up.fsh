#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

// Dual-Kawase upsample: 8 taps on a diamond, offset scales the glow radius.
void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec2 hp = resolution * offset;

    vec4 sum = texture(image, uv + vec2(-hp.x * 2.0, 0.0));
    sum += texture(image, uv + vec2(-hp.x,  hp.y)) * 2.0;
    sum += texture(image, uv + vec2( 0.0,   hp.y * 2.0));
    sum += texture(image, uv + vec2( hp.x,  hp.y)) * 2.0;
    sum += texture(image, uv + vec2( hp.x * 2.0, 0.0));
    sum += texture(image, uv + vec2( hp.x, -hp.y)) * 2.0;
    sum += texture(image, uv + vec2( 0.0,  -hp.y * 2.0));
    sum += texture(image, uv + vec2(-hp.x, -hp.y)) * 2.0;

    fragColor = vec4((sum * 0.0833).rgb, 1.0);
}

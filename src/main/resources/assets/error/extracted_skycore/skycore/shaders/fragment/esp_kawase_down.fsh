#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

// Dual-Kawase downsample: 5 taps, weight 4/1/1/1/1.
void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec2 hp = resolution;

    vec4 sum = texture(image, uv) * 4.0;
    sum += texture(image, uv - hp.xy);
    sum += texture(image, uv + hp.xy);
    sum += texture(image, uv + vec2(hp.x, -hp.y));
    sum += texture(image, uv - vec2(hp.x, -hp.y));

    fragColor = vec4((sum * 0.125).rgb, 1.0);
}

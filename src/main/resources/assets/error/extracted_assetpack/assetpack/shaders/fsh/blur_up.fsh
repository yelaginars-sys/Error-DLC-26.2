#version 330

uniform sampler2D InputSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 halfPixel = 0.5 / vec2(textureSize(InputSampler, 0));

    vec4 sum = vec4(0.0);
    sum += texture(InputSampler, texCoord + vec2(-halfPixel.x * 2.0, 0.0));
    sum += texture(InputSampler, texCoord + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(InputSampler, texCoord + vec2(0.0, halfPixel.y * 2.0));
    sum += texture(InputSampler, texCoord + vec2(halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(InputSampler, texCoord + vec2(halfPixel.x * 2.0, 0.0));
    sum += texture(InputSampler, texCoord + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
    sum += texture(InputSampler, texCoord + vec2(0.0, -halfPixel.y * 2.0));
    sum += texture(InputSampler, texCoord + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;

    fragColor = vec4(sum.rgb / 12.0, 1.0);
}

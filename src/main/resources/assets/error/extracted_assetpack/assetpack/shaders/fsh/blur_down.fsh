#version 330

uniform sampler2D InputSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 halfPixel = 0.5 / vec2(textureSize(InputSampler, 0));

    vec4 sum = texture(InputSampler, texCoord) * 4.0;
    sum += texture(InputSampler, texCoord - halfPixel);
    sum += texture(InputSampler, texCoord + halfPixel);
    sum += texture(InputSampler, texCoord + vec2(halfPixel.x, -halfPixel.y));
    sum += texture(InputSampler, texCoord - vec2(halfPixel.x, -halfPixel.y));

    fragColor = vec4(sum.rgb / 8.0, 1.0);
}

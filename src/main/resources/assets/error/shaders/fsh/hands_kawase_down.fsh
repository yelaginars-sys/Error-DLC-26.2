#version 330

uniform sampler2D Sampler0;

layout(std140) uniform EffectData {
    vec4 uColor;
    vec4 uColor2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixelData;
};

in vec2 TexCoord;
out vec4 OutColor;

#define uOffset tex.zw
#define uHalfPixel halfPixelData.xy
#define uSize tex.xy

void main() {
    vec2 uv = TexCoord;
    vec2 halfPixel = uHalfPixel * uOffset;

    vec4 sum = texture(Sampler0, uv) * 4.0;
    sum += texture(Sampler0, uv - halfPixel);
    sum += texture(Sampler0, uv + halfPixel);
    sum += texture(Sampler0, uv + vec2(halfPixel.x, -halfPixel.y));
    sum += texture(Sampler0, uv - vec2(halfPixel.x, -halfPixel.y));

    OutColor = sum / 8.0;
}

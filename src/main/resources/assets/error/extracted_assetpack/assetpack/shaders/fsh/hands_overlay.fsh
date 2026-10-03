#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

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

#define color uColor.rgb
#define fill params0.x
#define alpha screen.w
#define colorMode params1.z

void main() {
    vec2 uv = TexCoord;
    vec4 sample0 = texture(Sampler0, uv);
    float center = sample0.r;
    float inside = smoothstep(0.05, 0.95, center);
    float a = clamp(alpha * inside * fill, 0.0, 1.0);

    if (a <= 0.001) discard;

    vec4 itemSample = texture(Sampler1, uv);
    vec3 itemColor = itemSample.a > 0.0001 ? itemSample.rgb / itemSample.a : color;
    float useItemColor = step(0.5, colorMode);
    vec3 finalColor = mix(color, itemColor, useItemColor);
    OutColor = vec4(mix(finalColor * a, finalColor, useItemColor), a);
}

#version 330

layout(std140) uniform ZenithData {
    vec4 Radius;
    vec4 ZenithColorModulator;
    float Smoothness;
};

#moj_import <zenith:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec2 FragSize;
in vec4 FragColor;

uniform sampler2D Sampler0;

out vec4 OutColor;

void main() {
    vec4 texColor = texture(Sampler0, TexCoord);
    float alpha = ralpha(FragSize, FragCoord, Radius, Smoothness);

    vec4 finalColor = texColor * FragColor * ZenithColorModulator;
    finalColor.a *= alpha;

    if (finalColor.a <= 0.0) {
        discard;
    }

    OutColor = finalColor;
}


#version 330

layout(std140) uniform ZenithData {
    vec4 Radius;
    vec4 ZenithColorModulator;
    float Smoothness;
};

#moj_import <zenith:common.glsl>

in vec2 fragRectCoord;
in vec2 fragSize;
in vec4 FragColor;


out vec4 OutColor;

void main() {
    float alpha = ralpha(fragSize, fragRectCoord, Radius, Smoothness);
    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) {
        discard;
    }

    OutColor = finalColor * ZenithColorModulator;
}


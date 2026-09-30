#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D BeforeTexture;
uniform sampler2D AfterTexture;
uniform sampler2D BeforeDepth;
uniform sampler2D AfterDepth;

layout(std140) uniform HandMaskUniforms {
    vec2 TexelSize;
    vec2 GlowSides;
};


void main() {
    vec4 beforeColor = texture(BeforeTexture, uv);
    vec4 afterColor = texture(AfterTexture, uv);
    float colorDelta = length(afterColor.rgb - beforeColor.rgb);
    float alphaDelta = abs(afterColor.a - beforeColor.a);
    float beforeDepth = texture(BeforeDepth, uv).r;
    float afterDepth = texture(AfterDepth, uv).r;
    float depthDelta = abs(afterDepth - beforeDepth);

    float colorMask = smoothstep(0.012, 0.085, colorDelta + alphaDelta * 0.35);
    float depthMask = smoothstep(0.000001, 0.00012, depthDelta);
    finalColor = vec4(afterColor.rgb, max(colorMask, depthMask));
}

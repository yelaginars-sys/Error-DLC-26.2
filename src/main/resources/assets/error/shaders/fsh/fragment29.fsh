#version 330

layout(std140) uniform ZenithData {
    float Range;
    float Thickness;
    float Smoothness;
    int Outline;
    float OutlineThickness;
    vec4 OutlineColor;
    int EnableFadeout;
    float FadeoutStart;
    float FadeoutEnd;
    float MaxWidth;
    float TextPosX;
    vec4 ZenithColorModulator;
};

in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

uniform sampler2D Sampler0;


out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + Thickness;
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * textureSize(Sampler0, 0);
    float pixels = Range * inversesqrt(h.x * h.x + h.y * h.y);
    float alpha = smoothstep(-Smoothness, Smoothness, dist * pixels);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (Outline != 0) {
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness, Smoothness, (dist + OutlineThickness) * pixels);
    }


    if (EnableFadeout != 0) {
        float fadeAlpha = 1.0;
        float invWidth = 1.0 / max(MaxWidth, 0.0001);
        float normalizedX = (GlobalPos.x - TextPosX) * invWidth;
        if (normalizedX > FadeoutStart) {
            fadeAlpha = 1.0 - smoothstep(FadeoutStart, FadeoutEnd, normalizedX);
        }
        color.a *= fadeAlpha;
    }

    OutColor = color * ZenithColorModulator;
}


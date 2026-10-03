#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

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
#define color2 uColor2.rgb
#define exposure params0.y
#define colorMode params1.z

void main() {
    vec2 uv = TexCoord;

    vec4 blur = texture(Sampler0, uv);
    vec4 item = texture(Sampler1, uv);
    vec4 blurItem = texture(Sampler2, uv);

    float blurredMask = blur.r;
    float sharpMask = item.a;

    float outer = max(0.0, blurredMask - sharpMask);

    float intensity = clamp(outer * exposure, 0.0, 1.0);
    if (intensity <= 0.001) discard;

    vec3 base = mix(color, color2, uv.y);

    vec3 itemColor = blurItem.rgb;
    float itemWeight = blurItem.a;
    if (itemWeight > 0.0001) {
        itemColor /= itemWeight;
    }

    vec3 finalColor = mix(base, itemColor, step(0.5, colorMode));
    OutColor = vec4(finalColor, intensity);
}

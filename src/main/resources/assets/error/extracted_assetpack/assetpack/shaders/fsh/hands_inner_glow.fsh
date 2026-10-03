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
#define color2 uColor2.rgb
#define exposure params0.y
#define colorMode params1.z

void main() {
    vec2 uv = TexCoord;

    float blurredMask = texture(Sampler0, uv).r;
    vec4 item = texture(Sampler1, uv);
    float sharpMask = item.a;

    float inner = sharpMask * max(0.0, 1.0 - blurredMask);

    float intensity = clamp(inner * exposure, 0.0, 1.0);
    if (intensity <= 0.001) discard;

    vec3 base = mix(color, color2, uv.y);

    vec3 itemColor = item.rgb;
    if (sharpMask > 0.0001) {
        itemColor /= sharpMask;
    }

    vec3 finalColor = mix(base, itemColor, step(0.5, colorMode));
    OutColor = vec4(finalColor, intensity);
}

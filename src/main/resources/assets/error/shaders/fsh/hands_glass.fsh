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
#define colorMode params1.z

void main() {
    vec2 uv = TexCoord;

    float sharp = smoothstep(0.02, 0.25, texture(Sampler0, uv).r);
    if (sharp <= 0.001) discard;

    vec3 behind = texture(Sampler1, uv).rgb;

    vec4 itemSample = texture(Sampler2, uv);
    vec3 item = itemSample.a > 0.0001 ? itemSample.rgb / itemSample.a : vec3(1.0);

    vec3 base = mix(color, color2, uv.y);
    vec3 tint = mix(base, item, step(0.5, colorMode));

    float lum = clamp(dot(behind, vec3(0.299, 0.587, 0.114)), 0.0, 1.0);

    vec3 shaded = tint * (0.35 + 1.1 * lum);
    OutColor = vec4(mix(behind, shaded, 0.8), sharp);
}

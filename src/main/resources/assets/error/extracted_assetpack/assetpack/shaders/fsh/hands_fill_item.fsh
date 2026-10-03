#version 330

uniform sampler2D ItemSampler;

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

void main() {
    vec4 item = texture(ItemSampler, TexCoord);
    if (item.a <= 0.001) discard;

    vec3 itemColor = item.rgb / max(item.a, 0.001);
    float luma = dot(itemColor, vec3(0.299, 0.587, 0.114));
    vec3 hueColor = clamp(luma + (itemColor - luma) * 6.0, 0.0, 1.0);

    float inside = smoothstep(0.05, 0.95, item.a);
    float opacity = clamp(screen.w * params0.x * inside, 0.0, 1.0);
    if (opacity <= 0.001) discard;

    OutColor = vec4(hueColor, opacity);
}

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

void main() {
    vec4 color = texture(Sampler0, TexCoord);
    vec3 tinted = color.rgb * uColor.rgb;
    OutColor = vec4(mix(color.rgb, tinted, uColor.a), 1.0);
}

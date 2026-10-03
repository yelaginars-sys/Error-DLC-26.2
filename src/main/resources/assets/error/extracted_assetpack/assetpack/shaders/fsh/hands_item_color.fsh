#version 330

uniform sampler2D Sampler0;
uniform sampler2D ColorSampler;

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
    vec2 uv = TexCoord;

    float m = smoothstep(0.00003, 0.00075, texture(Sampler0, uv).r);
    if (m < 0.001) {
        OutColor = vec4(0.0);
        return;
    }

    vec3 col = texture(ColorSampler, uv).rgb;
    OutColor = vec4(col * m, m);
}

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

#define saturation params0.x

void main() {
    vec4 color = texture(Sampler0, TexCoord);
    float luma = dot(color.rgb, vec3(0.2125, 0.7154, 0.0721));
    vec3 gray = vec3(luma);

    vec3 result;
    if (saturation <= 1.0) {
        result = mix(gray, color.rgb, saturation);
        float contrast = 0.80 + 0.20 * saturation;
        result = clamp((result - 0.5) * contrast + 0.5, 0.0, 1.0);
    } else {
        float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
        float k = 1.0 + (saturation - 1.0) * (1.0 + 2.0 * (1.0 - chroma));
        result = clamp(mix(gray, color.rgb, k), 0.0, 1.0);
        float contrast = 1.0 + (saturation - 1.0) * 0.22;
        result = clamp((result - 0.5) * contrast + 0.5, 0.0, 1.0);
    }
    OutColor = vec4(result, 1.0);
}

#version 330

layout(std140) uniform ZenithData {
    vec2 Resolution;
    float Offset;
    float Saturation;
    float TintIntensity;
    vec3 TintColor;
};

#moj_import <zenith:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;

out vec4 OutColor;

vec3 adjustSaturation(vec3 color, float saturation) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, saturation);
}

void main() {
    vec2 uv = TexCoord * 2.0;
    vec2 halfpixel = Resolution * 2.0;

    vec3 sum = texture(Sampler0, uv).rgb * 4.0;
    sum += texture(Sampler0, uv - halfpixel.xy * Offset).rgb;
    sum += texture(Sampler0, uv + halfpixel.xy * Offset).rgb;
    sum += texture(Sampler0, uv + vec2(halfpixel.x, -halfpixel.y) * Offset).rgb;
    sum += texture(Sampler0, uv - vec2(halfpixel.x, -halfpixel.y) * Offset).rgb;

    vec3 color = sum / 8.0;
    color = adjustSaturation(color, Saturation);
    color = mix(color, TintColor, TintIntensity);

    OutColor = vec4(color, 1.0) * FragColor;
}


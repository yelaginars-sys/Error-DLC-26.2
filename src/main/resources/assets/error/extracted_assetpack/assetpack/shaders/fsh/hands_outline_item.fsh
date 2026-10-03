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

const vec2 DIRECTIONS[8] = vec2[](
    vec2( 1.0,  0.0), vec2(-1.0,  0.0),
    vec2( 0.0,  1.0), vec2( 0.0, -1.0),
    vec2( 0.707,  0.707), vec2(-0.707, -0.707),
    vec2( 0.707, -0.707), vec2(-0.707,  0.707)
);

void main() {
    if (texture(ItemSampler, TexCoord).a > 0.01) {
        discard;
    }

    vec2 pixel = 1.0 / max(screen.xy, vec2(1.0));
    vec3 colorSum = vec3(0.0);
    float colorWeight = 0.0;
    float coverage = 0.0;

    for (int direction = 0; direction < 8; direction++) {
        vec4 sampleColor = texture(ItemSampler, TexCoord + DIRECTIONS[direction] * pixel);
        if (sampleColor.a <= 0.001) {
            continue;
        }

        vec3 rgb = sampleColor.rgb / sampleColor.a;
        float maximum = max(max(rgb.r, rgb.g), rgb.b);
        float minimum = min(min(rgb.r, rgb.g), rgb.b);
        float saturation = maximum - minimum;
        float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
        float visibleColor = 0.12 + saturation * 1.8 + smoothstep(0.03, 0.45, luma) * 0.65;
        float weight = sampleColor.a * visibleColor;
        colorSum += rgb * weight;
        colorWeight += weight;
        coverage += sampleColor.a;
    }

    if (colorWeight <= 0.001) {
        discard;
    }

    vec3 outlineColor = colorSum / colorWeight;
    float alpha = smoothstep(0.02, 0.55, coverage);
    OutColor = vec4(clamp(outlineColor, 0.0, 1.0), alpha);
}

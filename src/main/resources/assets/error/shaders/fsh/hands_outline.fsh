#version 330
precision mediump float;

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

#define uRes screen.xy
#define color uColor.rgb
#define colorMode params1.z

void main() {
    vec4 centerItem = texture(Sampler1, TexCoord);
    if (centerItem.a > 0.01) discard;

    vec2 pixel = 1.0 / max(tex.xy, vec2(1.0));

    vec3 colorSum = vec3(0.0);
    float colorWeight = 0.0;
    float coverage = 0.0;

    vec2 offs[8] = vec2[](
        pixel * vec2( 1.0,  0.0), pixel * vec2(-1.0,  0.0),
        pixel * vec2( 0.0,  1.0), pixel * vec2( 0.0, -1.0),
        pixel * vec2( 1.0,  1.0), pixel * vec2(-1.0, -1.0),
        pixel * vec2( 1.0, -1.0), pixel * vec2(-1.0,  1.0)
    );

    for (int i = 0; i < 8; i++) {
        vec4 s = texture(Sampler1, TexCoord + offs[i]);
        if (s.a <= 0.001) continue;
        vec3 rgb = s.rgb / max(s.a, 0.001);
        float maximum = max(max(rgb.r, rgb.g), rgb.b);
        float minimum = min(min(rgb.r, rgb.g), rgb.b);
        float saturation = maximum - minimum;
        float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
        float visibleColor = 0.12 + saturation * 1.8 + smoothstep(0.03, 0.45, luma) * 0.65;
        float weight = s.a * visibleColor;
        colorSum += rgb * weight;
        colorWeight += weight;
        coverage += s.a;
    }

    if (colorWeight <= 0.001) {
        discard;
    }

    vec3 outlineColor = colorSum / colorWeight;
    float useItemColor = step(0.5, colorMode);
    vec3 finalColor = mix(color, outlineColor, useItemColor);
    float alpha = smoothstep(0.02, 0.55, coverage);
    OutColor = vec4(clamp(finalColor, 0.0, 1.0), mix(1.0, alpha, useItemColor));
}

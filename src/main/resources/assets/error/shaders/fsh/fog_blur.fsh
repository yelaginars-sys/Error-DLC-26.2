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

#define uStrength params0.x
#define uDistance params0.y
#define uNear params0.z
#define uFar params0.w
#define uUseColor params1.x

float viewDistance(float depth) {
    float far = max(uFar, 1.0);
    float near = max(uNear, 0.0001);
    return far / (1.0 + depth * (far - near) / near);
}

void main() {
    vec3 sceneColor = texture(Sampler0, TexCoord).rgb;
    vec3 blurredColor = texture(Sampler1, TexCoord).rgb;
    float depthSample = texture(Sampler2, TexCoord).r;

    float dist = viewDistance(depthSample);

    float fogStart = uDistance;
    float fogEnd = fogStart + 32.0;
    float fogMask = smoothstep(fogStart, fogEnd, dist);
    fogMask *= clamp(uStrength / 20.0, 0.0, 1.0);

    vec3 target = blurredColor;
    if (uUseColor > 0.5) {
        target = mix(blurredColor, uColor.rgb, clamp(uColor.a, 0.0, 1.0) * 0.35);
    }

    vec3 finalColor = mix(sceneColor, target, fogMask);
    OutColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}

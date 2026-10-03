#version 150 core

// Contract matches sky.module.visuals.FogBlur.process() / packDepth().
uniform sampler2D sceneTexture;
uniform sampler2D blurTexture;
uniform sampler2D depthTexture;
uniform float fogStrength;
uniform float fogDistance;
uniform float nearPlane;
uniform float farPlane;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform float colorAlpha;
uniform float time;
uniform float clientColor;

in vec2 fragCoord;
out vec4 fragColor;

#define NOISE 0.5 / 255.0

vec3 createGradient(vec2 coords) {
    float wave = sin(coords.x * 3.14159 + time * 0.3) * 0.5 + 0.5;
    float wave2 = cos(coords.y * 3.14159 + time * 0.2) * 0.5 + 0.5;
    float animFactor = mix(wave, wave2, 0.5);
    float globalShift = sin(time * 0.15) * 0.5 + 0.5;

    animFactor = mix(animFactor, globalShift, 0.3);
    animFactor = smoothstep(0.0, 1.0, animFactor);

    vec3 top = mix(color1.rgb, color2.rgb, coords.x);
    vec3 bottom = mix(color4.rgb, color3.rgb, coords.x);
    vec3 cornerGradient = mix(top, bottom, coords.y);
    vec3 animatedPair = mix(mix(color1.rgb, color3.rgb, animFactor), mix(color2.rgb, color4.rgb, animFactor), globalShift);
    vec3 gradColor = mix(cornerGradient, animatedPair, 0.35);
    gradColor += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return gradColor;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec3 sceneColor = texture(sceneTexture, uv).rgb;
    vec3 blurredColor = texture(blurTexture, uv).rgb;
    vec4 packedDepth = texture(depthTexture, uv);

    float linearDepth = packedDepth.r;
    float rawDepth = packedDepth.g;
    float skyMask = packedDepth.b;

    float strength = clamp(fogStrength / 20.0, 0.0, 1.0);
    float startDist = max(fogDistance, 1.0);
    float endDist = max(startDist * 2.5, startDist + 8.0);
    float maxDist = max(farPlane, endDist);

    float fogMask;
    if (skyMask > 0.5 || rawDepth >= 0.9999) {
        fogMask = strength;
    } else if (linearDepth < startDist) {
        fogMask = 0.0;
    } else {
        fogMask = smoothstep(startDist, min(endDist, maxDist), linearDepth) * strength;
    }

    vec3 finalRgb = sceneColor;
    if (fogMask > 1.0e-4) {
        if (clientColor > 0.5) {
            vec3 gradientColor = createGradient(uv);
            vec3 coloredBlur = mix(blurredColor, gradientColor, clamp(colorAlpha, 0.0, 1.0));
            finalRgb = mix(sceneColor, coloredBlur, fogMask);
        } else {
            finalRgb = mix(sceneColor, blurredColor, fogMask);
        }
    }

    fragColor = vec4(clamp(finalRgb, 0.0, 1.0), 1.0);
}

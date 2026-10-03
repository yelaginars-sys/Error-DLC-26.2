#version 120

#include aura_common

float auraModeSample(vec2 coord, vec2 wind, float voU) {
    vec2 uv = coord * 16.0 + wind * 40.0;
    float waves = sin(uv.x * 2.4 + auraFbm(uv * 1.6) * 4.0) * 0.5 + 0.5;
    waves *= sin(uv.y * 1.8 - wind.x * 12000.0) * 0.5 + 0.5;
    float caustics = auraFbm(uv * 3.5 + vec2(wind.x * 8000.0, 0.0));
    return auraShapeNoise(waves * 0.65 + caustics * 0.35, voU);
}

float auraModeDetail(vec2 coord, vec2 wind) {
    float ripple = auraFbm(coord * 2.0 + wind * 80.0);
    return 0.5 * ripple + 0.75;
}

#include aura_raymarch

uniform float time;
uniform float speed;
uniform float intensity;
uniform float rainStrength;
uniform float visibilityFactor;
uniform float aspectRatio;
uniform float tanHalfFov;

uniform vec3 cameraPos;
uniform vec3 cameraForward;
uniform vec3 cameraUp;
uniform vec3 cameraRight;
uniform vec3 lowColor;
uniform vec3 highColor;

varying vec2 screenUv;

void main() {
    vec3 worldDir = auraWorldDir(screenUv, aspectRatio, tanHalfFov, cameraForward, cameraUp, cameraRight);
    float visibility = auraVisibility(cameraPos, visibilityFactor, rainStrength);
    vec3 color = auraRaymarch(worldDir, cameraPos, time, speed, visibility, lowColor, highColor, intensity);
    gl_FragColor = vec4(color, 1.0);
}

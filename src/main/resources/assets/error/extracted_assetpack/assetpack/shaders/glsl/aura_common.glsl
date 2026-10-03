float auraHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float auraNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = auraHash(i);
    float b = auraHash(i + vec2(1.0, 0.0));
    float c = auraHash(i + vec2(0.0, 1.0));
    float d = auraHash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float auraFbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += amplitude * auraNoise(p);
        p *= 2.02;
        amplitude *= 0.5;
    }
    return value;
}

vec3 auraWorldDir(vec2 screenUv, float aspectRatio, float tanHalfFov, vec3 cameraForward, vec3 cameraUp, vec3 cameraRight) {
    vec2 ndc = screenUv * 2.0 - 1.0;
    vec3 viewDir = normalize(vec3(ndc.x * aspectRatio * tanHalfFov, ndc.y * tanHalfFov, -1.0));
    return normalize(viewDir.x * cameraRight + viewDir.y * cameraUp - viewDir.z * cameraForward);
}

float auraShapeNoise(float raw, float voU) {
    float noise = raw * 6.0;
    return max(1.0 - 4.0 * (0.5 * voU + 0.5) * abs(noise - 3.0), 0.0);
}

vec2 auraWind(float effectTime, float effectSpeed) {
    return vec2(
        effectTime * effectSpeed * 0.000125,
        sin(effectTime * effectSpeed * 0.05) * 0.00025
    );
}

float auraVisibility(vec3 cameraPos, float visibilityFactor, float rainStrength) {
    float visibility = visibilityFactor * (1.0 - rainStrength) * (1.0 - rainStrength);
    visibility *= clamp((cameraPos.y + 6.0) / 8.0, 0.0, 1.0);
    return visibility;
}

#version 150 core

in vec2 fragCoord;

uniform sampler2D sceneTexture;
uniform sampler2D depthPackTexture;
uniform sampler2D blurTexture;

uniform vec2 texelSize;
uniform vec2 screenSize;
uniform vec3 fogColor;
uniform vec3 sunDirection;
uniform vec2 sunScreenPos;

uniform float nearPlane;
uniform float farPlane;
uniform float time;

uniform float density;
uniform float scatterHeight;
uniform float softness;
uniform float rayStrength;
uniform float nightEffect;
uniform float distanceBlur;
uniform float blurStart;
uniform float blurEnd;
uniform float fogStart;
uniform float fogEnd;

out vec4 fragColor;

vec3 applyNight(vec3 color, float amount) {
    vec3 cool = vec3(color.r * 0.62, color.g * 0.72, color.b * 1.10);
    return mix(color, cool * mix(1.0, 0.70, amount), amount);
}

float softFog(float linearDepth, float skyMask) {
    float range = max(fogEnd - fogStart, 1.0);
    float t = clamp((linearDepth - fogStart) / range, 0.0, 1.0);
    t = pow(t, max(softness, 0.25));

    // Height-ish falloff: distant high sky gets less fog than horizon.
    // Approximate with vertical screen factor only for sky pixels.
    float heightFade = 1.0;
    if (skyMask > 0.5) {
        float up = clamp((1.0 - fragCoord.y) * 1.4 - 0.15, 0.0, 1.0);
        heightFade = mix(1.0, 0.15, up);
        t = mix(0.25, 0.85, t) * heightFade;
    } else {
        // Mild vertical softening for terrain (scatterHeight as softness scale).
        float lift = clamp(scatterHeight / 80.0, 0.15, 1.0);
        t *= mix(1.0, lift, 0.35);
    }

    float fog = 1.0 - exp(-density * t * 1.35);
    return clamp(fog, 0.0, 0.92);
}

float godRays(vec2 uv, vec2 sunUv, float skyHere) {
    if (rayStrength < 0.01) {
        return 0.0;
    }
    if (sunUv.x < -0.2 || sunUv.x > 1.2 || sunUv.y < -0.2 || sunUv.y > 1.2) {
        return 0.0;
    }

    vec2 delta = (uv - sunUv) / 24.0;
    vec2 coord = uv;
    float illum = 0.0;
    float decay = 1.0;

    for (int i = 0; i < 24; i++) {
        coord -= delta;
        vec2 s = clamp(coord, vec2(0.002), vec2(0.998));
        float sky = texture(depthPackTexture, s).b;
        // Also pick up bright haze from blur for softer shafts.
        vec3 b = texture(blurTexture, s).rgb;
        float bright = max(max(b.r, b.g), b.b);
        float sample = max(sky, bright * 0.35);
        illum += sample * decay;
        decay *= 0.92;
    }

    float aspect = screenSize.x / max(screenSize.y, 1.0);
    float dist = length((uv - sunUv) * vec2(aspect, 1.0));
    float glow = 1.0 - smoothstep(0.0, 0.75, dist);
    float sunUp = clamp(sunDirection.y + 0.05, 0.0, 1.0);
    return (illum / 24.0) * glow * glow * sunUp * rayStrength * mix(0.35, 1.0, skyHere);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);

    vec3 scene = texture(sceneTexture, uv).rgb;
    // Guard against garbage from previous passes.
    scene = max(scene, vec3(0.0));

    vec3 pack = texture(depthPackTexture, uv).rgb;
    float linearDepth = pack.r;
    float skyMask = pack.b;

    float fogAmount = softFog(linearDepth, skyMask);

    vec3 atmosphere = fogColor;
    float sunHeight = clamp(sunDirection.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 warm = vec3(1.12, 0.90, 0.72);
    vec3 cool = vec3(0.82, 0.90, 1.08);
    atmosphere *= mix(warm, cool, sunHeight);
    if (nightEffect > 0.01) {
        atmosphere = applyNight(atmosphere, nightEffect);
    }

    vec3 color = mix(scene, atmosphere, fogAmount);

    if (distanceBlur > 0.01) {
        float blurT = smoothstep(blurStart, max(blurEnd, blurStart + 1.0), linearDepth);
        blurT = pow(clamp(blurT, 0.0, 1.0), 0.85) * distanceBlur;
        if (skyMask > 0.5) {
            blurT *= 0.55;
        }
        vec3 blurred = texture(blurTexture, uv).rgb;
        blurred = mix(blurred, atmosphere, fogAmount * 0.65);
        color = mix(color, blurred, clamp(blurT, 0.0, 0.95));
    }

    float rays = godRays(uv, sunScreenPos, skyMask);
    if (rays > 0.0) {
        vec3 rayColor = mix(vec3(1.0, 0.94, 0.80), vec3(0.55, 0.70, 1.05), nightEffect);
        color += rayColor * rays * 0.55;
    }

    if (nightEffect > 0.01) {
        color = applyNight(color, nightEffect * 0.25);
    }

    // Never emit NaNs / negatives.
    color = clamp(color, vec3(0.0), vec3(8.0));
    fragColor = vec4(color, 1.0);
}

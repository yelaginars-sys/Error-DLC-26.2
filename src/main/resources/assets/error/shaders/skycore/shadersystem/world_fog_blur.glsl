#version 150 core

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;

uniform vec4 FogParams;
uniform vec4 Color1;
uniform vec4 Color2;
uniform vec4 Color3;
uniform vec4 Color4;

uniform vec4 ColorSettings;

in vec2 fragCoord;
out vec4 fragColor;

#define NOISE 0.5/255.0

bool depthIsSky(float depth) {
    bool reverseZ = FogParams.w > 0.5;
    return reverseZ ? (depth <= 1.0e-5) : (depth >= 0.99999);
}

vec3 worldPositionFromDepth(vec2 coords, float depth) {
    float clipZ = FogParams.w > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 ndc = vec4(coords * 2.0 - 1.0, clipZ, 1.0);
    vec4 world = InvViewProjection * ndc;
    return world.xyz / max(world.w, 1.0e-6);
}

vec3 animatedGradient(vec2 coords, vec4 col1, vec4 col2, vec4 col3, vec4 col4, float time) {
    float waveX = sin(coords.x * 3.14159 + time * 0.3) * 0.5 + 0.5;
    float waveY = cos(coords.y * 3.14159 + time * 0.2) * 0.5 + 0.5;
    float blendWave = mix(waveX, waveY, 0.5);
    float globalShift = sin(time * 0.15) * 0.5 + 0.5;

    blendWave = mix(blendWave, globalShift, 0.3);
    blendWave = smoothstep(0.0, 1.0, blendWave);

    vec3 topRow = mix(col1.rgb, col2.rgb, coords.x);
    vec3 bottomRow = mix(col4.rgb, col3.rgb, coords.x);
    vec3 cornerBlend = mix(topRow, bottomRow, coords.y);
    vec3 animatedBlend = mix(
        mix(col1.rgb, col3.rgb, blendWave),
        mix(col2.rgb, col4.rgb, blendWave),
        globalShift
    );
    vec3 gradColor = mix(cornerBlend, animatedBlend, 0.35);
    gradColor += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(41.283, 93.117))) * 28421.7313));
    return gradColor;
}

float thinOverlayMask(vec2 uv, float z) {
    if (depthIsSky(z)) {
        return 0.0;
    }
    bool revZ = FogParams.w > 0.5;
    vec2 texel = 1.0 / vec2(max(textureSize(DepthSampler, 0).xy, ivec2(1)));
    float dL = texture(DepthSampler, uv + vec2(-texel.x, 0.0)).r;
    float dR = texture(DepthSampler, uv + vec2( texel.x, 0.0)).r;
    float dU = texture(DepthSampler, uv + vec2(0.0,  texel.y)).r;
    float dD = texture(DepthSampler, uv + vec2(0.0, -texel.y)).r;
    float spread = max(max(abs(dL - z), abs(dR - z)), max(abs(dU - z), abs(dD - z)));
    if (spread < 0.0005) {
        return 0.0;
    }
    float minN = min(min(dL, dR), min(dU, dD));
    float maxN = max(max(dL, dR), max(dU, dD));
    bool isolated = revZ ? (z < minN - 0.00002) : (z > maxN + 0.00002);
    bool crossPlant = spread > 0.002 && spread < 0.06;
    return (isolated || crossPlant) ? 1.0 : 0.0;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec3 sceneColor = texture(SceneSampler, uv).rgb;
    vec3 blurredColor = texture(BlurSampler, uv).rgb;
    float depthSample = texture(DepthSampler, uv).r;

    if (thinOverlayMask(uv, depthSample) > 0.5) {
        fragColor = vec4(clamp(sceneColor, 0.0, 1.0), 1.0);
        return;
    }

    float strength = clamp(FogParams.x / 20.0, 0.0, 1.0);
    float startDist = max(FogParams.y, 1.0);
    float endDist = max(FogParams.z, startDist + 1.0);

    float fogMask;
    if (depthIsSky(depthSample)) {
        fogMask = strength * 0.12;
    } else {
        float linearDepth = length(worldPositionFromDepth(uv, depthSample));
        if (linearDepth < startDist) {
            fogMask = 0.0;
        } else {
            fogMask = smoothstep(startDist, endDist, linearDepth) * strength;
            float edgeFade = 1.0 - smoothstep(endDist * 0.88, endDist * 1.05, linearDepth);
            fogMask *= mix(0.65, 1.0, edgeFade);
            fogMask = min(fogMask, 0.72);
        }
    }

    vec3 finalRgb = sceneColor;
    if (fogMask > 1.0e-4) {
        if (ColorSettings.z > 0.5) {
            vec3 gradientColor = animatedGradient(uv, Color1, Color2, Color3, Color4, ColorSettings.y);
            vec3 coloredBlur = mix(blurredColor, gradientColor, ColorSettings.x);
            finalRgb = mix(sceneColor, coloredBlur, fogMask);
        } else {
            finalRgb = mix(sceneColor, blurredColor, fogMask);
        }
    }

    fragColor = vec4(clamp(finalRgb, 0.0, 1.0), 1.0);
}

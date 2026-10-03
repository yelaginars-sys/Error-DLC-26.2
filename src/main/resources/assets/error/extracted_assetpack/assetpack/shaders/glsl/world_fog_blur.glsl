#version 150 core

// Ported 1:1 from SkycoreRecode post/world_fog_blur.fsh

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;
// x = strength, y = startBlocks, z = endBlocks, w = zZeroToOne (0 = classic)
uniform vec4 FogParams;
uniform vec4 Color1;
uniform vec4 Color2;
uniform vec4 Color3;
uniform vec4 Color4;
// x = colorMix, y = time, z = colorEnabled, w = unused
uniform vec4 ColorSettings;

in vec2 fragCoord;
out vec4 fragColor;

#define NOISE 0.5/255.0

bool isSkyDepth(float depth) {
    if (FogParams.w > 0.5) {
        return depth <= 1.0e-5;
    }
    return depth >= 0.99999;
}

vec3 reconstructPosition(vec2 coords, float depth) {
    float clipZ = FogParams.w > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 ndc = vec4(coords * 2.0 - 1.0, clipZ, 1.0);
    vec4 position = InvViewProjection * ndc;
    return position.xyz / max(position.w, 1.0e-6);
}

vec3 createGradient(vec2 coords, vec4 col1, vec4 col2, vec4 col3, vec4 col4, float time) {
    float wave = sin(coords.x * 3.14159 + time * 0.3) * 0.5 + 0.5;
    float wave2 = cos(coords.y * 3.14159 + time * 0.2) * 0.5 + 0.5;
    float animFactor = mix(wave, wave2, 0.5);
    float globalShift = sin(time * 0.15) * 0.5 + 0.5;

    animFactor = mix(animFactor, globalShift, 0.3);
    animFactor = smoothstep(0.0, 1.0, animFactor);

    vec3 top = mix(col1.rgb, col2.rgb, coords.x);
    vec3 bottom = mix(col4.rgb, col3.rgb, coords.x);
    vec3 cornerGradient = mix(top, bottom, coords.y);
    vec3 animatedPair = mix(mix(col1.rgb, col3.rgb, animFactor), mix(col2.rgb, col4.rgb, animFactor), globalShift);
    vec3 gradColor = mix(cornerGradient, animatedPair, 0.35);
    gradColor += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));

    return gradColor;
}

void main() {
    // FBO attachments are GL bottom-origin; fragCoord.y=0 is screen top.
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec3 sceneColor = texture(SceneSampler, uv).rgb;
    vec3 blurredColor = texture(BlurSampler, uv).rgb;
    float depthSample = texture(DepthSampler, uv).r;

    float strength = clamp(FogParams.x / 20.0, 0.0, 1.0);
    float startDist = max(FogParams.y, 1.0);
    float endDist = max(FogParams.z, startDist + 1.0);

    float fogMask;
    if (isSkyDepth(depthSample)) {
        fogMask = strength;
    } else {
        float linearDepth = length(reconstructPosition(uv, depthSample));
        if (linearDepth < startDist) {
            fogMask = 0.0;
        } else {
            fogMask = smoothstep(startDist, endDist, linearDepth) * strength;
        }
    }

    vec3 finalRgb = sceneColor;
    if (fogMask > 1.0e-4) {
        if (ColorSettings.z > 0.5) {
            vec3 gradientColor = createGradient(uv, Color1, Color2, Color3, Color4, ColorSettings.y);
            vec3 coloredBlur = mix(blurredColor, gradientColor, ColorSettings.x);
            finalRgb = mix(sceneColor, coloredBlur, fogMask);
        } else {
            finalRgb = mix(sceneColor, blurredColor, fogMask);
        }
    }

    fragColor = vec4(clamp(finalRgb, 0.0, 1.0), 1.0);
}

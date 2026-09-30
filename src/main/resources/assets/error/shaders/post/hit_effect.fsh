#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

#define MAX_HITS 4
#define PI 3.14159265359

struct HitData {
    vec4 posProgress;
    vec4 params;
    vec4 color;
};

layout (std140) uniform HitEffectUniforms {
    mat4 Proj;
    mat4 InvProj;
    mat4 View;
    mat4 InvView;
    vec4 CameraPos;
    vec4 ScreenParams;
    HitData Hits[MAX_HITS];
};

vec3 getViewPosition(vec2 texCoord, float rawDepth) {
    vec4 clip = vec4(texCoord * 2.0 - 1.0, rawDepth, 1.0);
    vec4 viewH = InvProj * clip;
    return viewH.xyz / max(abs(viewH.w), 0.000001);
}

void main() {
    vec4 baseScene = texture(SceneSampler, uv);
    float depth = texture(DepthSampler, uv).r;

    if (depth <= 0.00001 || depth >= 0.99999 || ScreenParams.z <= 0.0) {
        finalColor = baseScene;
        return;
    }

    vec3 viewPos = getViewPosition(uv, depth);
    vec3 worldPos = CameraPos.xyz + (InvView * vec4(viewPos, 1.0)).xyz;

    vec2 totalDistortion = vec2(0.0);
    vec3 totalGlow = vec3(0.0);

    int activeCount = int(ScreenParams.z);
    for (int i = 0; i < MAX_HITS; i++) {
        if (i >= activeCount) break;

        HitData hit = Hits[i];
        vec3 hitPos = hit.posProgress.xyz;
        float progress = clamp(hit.posProgress.w, 0.0, 1.0);
        float maxRadius = hit.params.x;
        float distStrength = hit.params.y;
        float glowStrength = hit.params.z;
        float isWorldMode = hit.params.w;
        vec3 col = hit.color.rgb;

        float dist = length(worldPos - hitPos);

        vec3 worldDir = worldPos - hitPos;
        vec4 viewDir = View * vec4(worldDir, 0.0);
        vec4 projDir = Proj * viewDir;
        vec2 screenDir = normalize(projDir.xy + vec2(0.00001));

        if (isWorldMode > 0.5) {
            float waveFade = pow(1.0 - progress, 1.8);
            float curRadius = maxRadius * sin(progress * (PI * 0.5));
            float ringThickness = max(0.35, maxRadius * 0.12);
            float ringDelta = abs(dist - curRadius);

            float ring = exp(-pow(ringDelta / ringThickness, 2.0));
            if (ring > 0.001) {
                float wave = sin((dist - curRadius) * 4.5) * ring;
                totalDistortion += screenDir * (wave * distStrength * 0.045 * waveFade);

                float edgeGlow = pow(ring, 1.6) * glowStrength * 1.8 * waveFade;
                totalGlow += col * edgeGlow;
            }
        } else {
            float envelope = sin(progress * PI);
            float smoothFade = pow(envelope, 1.3);

            float expandEase = 1.0 - pow(1.0 - progress, 2.5);
            float currentRadius = maxRadius * (0.35 + 0.65 * expandEase);

            float normDist = dist / currentRadius;

            if (normDist < 1.4) {
                float edgeMask = smoothstep(1.4, 0.0, normDist);

                float ringDist = abs(dist - currentRadius);
                float outlineThickness = 0.06 + 0.04 * expandEase;
                float outline = exp(-pow(ringDist / outlineThickness, 2.0)) * smoothFade;
                float bulge = exp(-pow(normDist * 2.2, 2.0)) * smoothFade;
                float ripple = sin(dist * 16.0 - progress * 18.0) * exp(-normDist * 2.0) * smoothFade;
                float totalDistort = (bulge * 0.75 + ripple * 0.35) * distStrength * 0.055 * edgeMask;
                totalDistortion += screenDir * totalDistort;
                vec3 outlineGlow = col * outline * (glowStrength * 2.2);
                vec3 innerGlow = col * bulge * (glowStrength * 0.75);
                totalGlow += (outlineGlow + innerGlow) * edgeMask;
            }
        }
    }

    if (length(totalDistortion) <= 0.00005 && length(totalGlow) <= 0.00005) {
        finalColor = baseScene;
        return;
    }

    vec2 finalUv = clamp(uv + totalDistortion, 0.001, 0.999);

    float r = texture(SceneSampler, clamp(finalUv + totalDistortion * 0.3, 0.001, 0.999)).r;
    float g = texture(SceneSampler, finalUv).g;
    float b = texture(SceneSampler, clamp(finalUv - totalDistortion * 0.3, 0.001, 0.999)).b;

    finalColor = vec4(vec3(r, g, b) + totalGlow, baseScene.a);
}
#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 GlowColor;
uniform float Intensity;
uniform float DownShift;
uniform float TrailStrength;
uniform float UseItemColor;
uniform float Dynamic;
uniform float Time;

out vec4 OutColor;

const float TAU = 6.28318530718;

float maxChannel(vec3 color) {
    return max(color.r, max(color.g, color.b));
}

vec3 liftVisible(vec3 color) {
    float peak = maxChannel(color);
    if (peak <= 0.001) {
        return vec3(0.78);
    }
    return clamp(color * (max(peak, 0.62) / peak), 0.0, 1.0);
}

vec3 readableItemColor(vec3 premultiplied, float coverage, vec3 fallback) {
    vec3 color = clamp(premultiplied / max(coverage, 0.0001), 0.0, 1.0);
    float peak = maxChannel(color);

    // Never normalize tiny RGB noise from a black texture into a random saturated
    // hue. Low-confidence samples smoothly use the selected glow color instead.
    vec3 stableFallback = liftVisible(clamp(fallback, 0.0, 1.0));
    vec3 liftedColor = liftVisible(color);
    float colorConfidence = smoothstep(0.08, 0.20, peak);
    return mix(stableFallback, liftedColor, colorConfidence);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float smoothNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * local
            * (local * (local * 6.0 - 15.0) + 10.0);

    float a = hash12(cell);
    float b = hash12(cell + vec2(1.0, 0.0));
    float c = hash12(cell + vec2(0.0, 1.0));
    float d = hash12(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

vec2 flowingWarp(vec2 p, float time) {
    float x = smoothNoise(p + vec2(time * 0.11, -time * 0.07));
    float y = smoothNoise(p + vec2(-time * 0.09, time * 0.13)
            + vec2(19.7, 7.3));
    return vec2(x, y) - 0.5;
}

float shimmerField(vec2 p, vec2 warp, float time) {
    float broad = smoothNoise(p + warp * 1.25
            + vec2(time * 0.085, -time * 0.055));
    float detail = smoothNoise(p * 1.73 - warp * 0.65
            + vec2(-time * 0.13, time * 0.09));

    // A noise-bent travelling wave makes the glow visibly flow instead of only
    // breathing in place. Its long wavelength stays smooth at item edges.
    float wavePhase = dot(p, vec2(1.15, 0.72))
            + (broad - detail) * TAU * 1.35 - time * 1.10;
    float travellingWave = 0.5 + sin(wavePhase) * 0.5;
    travellingWave = travellingWave * travellingWave
            * (3.0 - 2.0 * travellingWave);
    return clamp(broad * 0.48 + detail * 0.27
            + travellingWave * 0.25, 0.0, 1.0);
}

vec3 pearlescentPalette(float phase) {
    return 0.58 + 0.42 * cos(TAU
            * (phase + vec3(0.0, 0.333333, 0.666667)));
}

float softBloomEnergy(float coverage) {
    float value = max(coverage, 0.0);
    // Monotonic compression prevents a bright contour without moving peak energy
    // into a detached outer blob.
    return value / (1.0 + value * 5.0) * 1.60;
}

vec4 sampleSoft(sampler2D source, vec2 uv) {
    vec2 offset = 1.10 / vec2(textureSize(source, 0));
    vec4 result = texture(source, uv) * 0.20;
    result += texture(source, uv + vec2(offset.x, offset.y)) * 0.20;
    result += texture(source, uv + vec2(-offset.x, offset.y)) * 0.20;
    result += texture(source, uv + vec2(offset.x, -offset.y)) * 0.20;
    result += texture(source, uv - offset) * 0.20;
    return result;
}

void main() {
    float dynamicAmount = clamp(Dynamic, 0.0, 1.0);

    // Most of the screen contains no held item. Reject it before procedural
    // noise so the animated field only costs GPU time around glow/trail pixels.
    float coarseCoverage = texture(Sampler0, TexCoord).a
            + texture(Sampler0, TexCoord + vec2(0.0, DownShift)).a
            + texture(Sampler1, TexCoord).a;
    if (coarseCoverage <= 0.00002) discard;

    vec2 sourceSize = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / sourceSize;
    vec2 fieldUv = TexCoord * sourceSize / 18.0;
    float animationTime = Time * 1.75;
    vec2 warp = flowingWarp(fieldUv * 0.58, animationTime);
    float field = shimmerField(fieldUv, warp, animationTime);
    float shapedField = smoothstep(0.08, 0.92, field);

    float unevenLight = mix(1.0, mix(0.50, 1.55, shapedField), dynamicAmount);
    float breathing = mix(1.0, 0.95 + sin(Time * 1.65) * 0.05,
            dynamicAmount);
    vec2 organicOffset = warp * texel * 2.35 * dynamicAmount;
    float downFlow = mix(1.0, 0.30 + shapedField * 1.45, dynamicAmount);

    vec4 currentSample = sampleSoft(Sampler0, TexCoord + organicOffset);
    vec4 downSample = sampleSoft(Sampler0,
            TexCoord + organicOffset + vec2(0.0, DownShift * downFlow));

    // The same broad mask carries RGB and coverage. A small, smoothly moving
    // displacement prevents the glow from becoming a rigid copy of item pixels.
    float downWeight = mix(0.12, 0.09 + shapedField * 0.17, dynamicAmount);
    vec4 currentGlow = mix(currentSample, downSample, downWeight);

    vec4 historyGlow = sampleSoft(Sampler1, TexCoord + organicOffset * 0.55);
    float trailCoverage = max(historyGlow.a - currentGlow.a, 0.0);
    float residueFade = smoothstep(0.002, 0.024, trailCoverage);
    float trailField = shimmerField(fieldUv + vec2(7.3, 3.1), -warp,
            animationTime * 0.82);
    float unevenTrail = mix(1.0,
            mix(0.58, 1.42, smoothstep(0.08, 0.92, trailField)),
            dynamicAmount);

    // Reinhard-style compression prevents the inner bloom from saturating into a
    // solid pixel-shaped border while preserving its wide, continuous falloff.
    float currentEnergy = softBloomEnergy(currentGlow.a)
            * unevenLight * breathing;
    float trailEnergy = softBloomEnergy(trailCoverage)
            * residueFade * TrailStrength * unevenTrail;
    float totalEnergy = currentEnergy + trailEnergy;
    float exposure = max(totalEnergy * Intensity, 0.0);
    float glowAlpha = 0.94 * (1.0 - exp(-exposure * 1.85));
    if (glowAlpha <= 0.001) discard;

    vec3 currentColor = readableItemColor(currentGlow.rgb, currentGlow.a,
            GlowColor);
    vec3 trailColor = readableItemColor(historyGlow.rgb, historyGlow.a,
            GlowColor);
    vec3 sampledColor = (currentColor * currentEnergy + trailColor * trailEnergy)
            / max(totalEnergy, 0.0001);
    vec3 baseColor = mix(GlowColor, sampledColor,
            clamp(UseItemColor, 0.0, 1.0));

    float phase = field * 1.10 + Time * 0.13
            + dot(TexCoord, vec2(0.55, -0.35));
    vec3 spectrum = pearlescentPalette(phase);
    vec3 pearlescent = clamp(baseColor * (0.72 + spectrum * 0.32)
            + spectrum * 0.24, 0.0, 1.0);
    float sheen = dynamicAmount * (0.17
            + smoothstep(0.50, 0.90, shapedField) * 0.21);
    vec3 finalColor = mix(baseColor, pearlescent, sheen);

    OutColor = vec4(finalColor, glowAlpha);
}

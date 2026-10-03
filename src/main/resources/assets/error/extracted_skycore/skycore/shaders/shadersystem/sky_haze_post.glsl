#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;

uniform vec4 SkyParams;
uniform float LightningTime;
uniform float LightningCount;
uniform vec2 SkyTexel;

in vec2 fragCoord;
out vec4 fragColor;

#define TAU 6.28318530718

float rand3(vec3 p) {
    p = fract(p * vec3(0.2171, 0.1917, 0.2531));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z * 73.0);
}

float trilinearNoise(vec3 p) {
    vec3 cell = floor(p);
    vec3 fracPart = fract(p);
    fracPart = fracPart * fracPart * fracPart * (fracPart * (fracPart * 6.0 - 15.0) + 10.0);
    float c000 = rand3(cell);
    float c100 = rand3(cell + vec3(1.0, 0.0, 0.0));
    float c010 = rand3(cell + vec3(0.0, 1.0, 0.0));
    float c110 = rand3(cell + vec3(1.0, 1.0, 0.0));
    float c001 = rand3(cell + vec3(0.0, 0.0, 1.0));
    float c101 = rand3(cell + vec3(1.0, 0.0, 1.0));
    float c011 = rand3(cell + vec3(0.0, 1.0, 1.0));
    float c111 = rand3(cell + vec3(1.0, 1.0, 1.0));
    float blendX0 = mix(c000, c100, fracPart.x);
    float blendX1 = mix(c010, c110, fracPart.x);
    float blendX2 = mix(c001, c101, fracPart.x);
    float blendX3 = mix(c011, c111, fracPart.x);
    float blendY0 = mix(blendX0, blendX1, fracPart.y);
    float blendY1 = mix(blendX2, blendX3, fracPart.y);
    return mix(blendY0, blendY1, fracPart.z);
}

float fractalNoise2(vec3 p) {
    return 0.5 * trilinearNoise(p)
         + 0.25 * trilinearNoise(p * 2.05 + vec3(1.7, -1.3, 0.9));
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    vec2 ap = p - a;
    float proj = clamp(dot(ap, ab) / max(dot(ab, ab), 1e-5), 0.0, 1.0);
    return length(ap - ab * proj);
}

bool depthIsSky(float depth) {
    bool reverseZ = SkyParams.w > 0.5;
    return reverseZ ? (depth <= 0.000001) : (depth >= 0.99999);
}

bool hasSkyNeighbor(vec2 coords) {
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            vec2 offset = vec2(float(dx), float(dy)) * SkyTexel * 2.0;
            if (depthIsSky(texture(DepthSampler, coords + offset).r)) {
                return true;
            }
        }
    }
    return false;
}

vec3 viewRayDirection(vec2 coords) {
    vec2 ndc = coords * 2.0 - 1.0;
    float farZ = SkyParams.w > 0.5 ? 0.0 : -1.0;
    vec4 nearClip = InvViewProjection * vec4(ndc, 1.0, 1.0);
    vec4 farClip = InvViewProjection * vec4(ndc, farZ, 1.0);
    vec3 nearWorld = nearClip.xyz / max(nearClip.w, 1e-6);
    vec3 farWorld = farClip.xyz / max(farClip.w, 1e-6);
    return normalize(farWorld - nearWorld);
}

float evaluateCloudLayer(vec3 rd, float t, float sc, float scale, vec3 drift, float lo, float hi) {
    vec3 samplePos = rd * (scale * sc) + drift;
    vec3 warp = vec3(
        fractalNoise2(samplePos * 0.9 + t * 0.02),
        fractalNoise2(samplePos * 0.9 + vec3(3.1, -t * 0.015, 1.4)),
        0.0
    );
    float density = fractalNoise2(samplePos + warp.xyx * 0.95);
    return smoothstep(lo, hi, density);
}

vec3 renderSingleBolt(vec3 rd, float wallT, float period, float boltId, float boltCount) {
    if (period < 0.05) {
        return vec3(0.0);
    }

    vec2 sphereUv = vec2(
        atan(rd.z, rd.x) / TAU + 0.5,
        acos(clamp(rd.y, -1.0, 1.0)) / 3.14159265
    );
    float edgeFade = smoothstep(0.0, 0.04, min(sphereUv.x, 1.0 - sphereUv.x));

    float cycleIdx = floor(wallT / period);
    float phase = fract(wallT / period - boltId * 0.018);
    float seed = cycleIdx * 17.0 + boltId * 91.3;

    float flash = smoothstep(0.0, 0.008, phase) * (1.0 - smoothstep(0.035, 0.09, phase));
    flash += 0.45 * smoothstep(0.11, 0.12, phase) * (1.0 - smoothstep(0.13, 0.18, phase));
    if (flash * edgeFade < 0.01) {
        return vec3(0.0);
    }

    float boltTotal = max(boltCount, 1.0);
    float slot = (boltId + 0.5) / boltTotal;
    float jitter = (rand3(vec3(seed, 3.1, 0.7)) - 0.5) * (0.55 / boltTotal);
    float anchorX = clamp(slot + jitter, 0.07, 0.93);
    float topY = 0.02 + rand3(vec3(seed, 7.7, 1.2)) * 0.06;
    float botY = 0.36 + rand3(vec3(seed, 13.3, 2.1)) * 0.34;
    vec2 strikePoint = vec2(anchorX, mix(topY, botY, 0.45));

    float minDist = 1e5;
    vec2 prevPoint = vec2(anchorX, topY);
    for (int seg = 1; seg <= 5; seg++) {
        float segT = float(seg) / 5.0;
        float segY = mix(topY, botY, segT);
        float jag = (rand3(vec3(seed, float(seg) * 19.17, 4.4)) - 0.5) * (0.045 + 0.09 * segT);
        vec2 curPoint = vec2(anchorX + jag, segY);
        minDist = min(minDist, segmentDistance(sphereUv, prevPoint, curPoint));
        prevPoint = curPoint;
    }

    float core = smoothstep(0.0035, 0.0, minDist);
    float glow = smoothstep(0.022, 0.0, minDist);
    float pulse = flash * edgeFade;

    float strikeDist = length((sphereUv - strikePoint) * vec2(1.15, 1.55));
    float skyBloom = exp(-strikeDist * strikeDist * 8.0) * smoothstep(0.48, 0.0, strikeDist);

    vec3 boltCol = (vec3(0.92, 0.96, 1.0) * core * 2.4 + vec3(0.45, 0.65, 1.0) * glow * 0.75) * pulse;
    vec3 bloomCol = vec3(0.40, 0.52, 0.82) * skyBloom * pulse * 0.7;
    return boltCol + bloomCol;
}

vec3 accumulateBolts(vec3 rd, float wallT, float period, float count) {
    float boltCountF = clamp(count, 1.0, 6.0);
    int boltCount = int(boltCountF + 0.5);
    vec3 total = vec3(0.0);
    for (int b = 0; b < 6; b++) {
        if (b >= boltCount) {
            break;
        }
        total += renderSingleBolt(rd, wallT, period, float(b), boltCountF);
    }
    return total;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    if (!hasSkyNeighbor(uv)) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 rd = viewRayDirection(uv);
    rd.y = -rd.y;
    float elevation = rd.y;
    float t = SkyParams.x;
    float sc = max(0.5, SkyParams.y);

    vec3 theme = PrimaryColor.rgb;
    vec3 themeDark = SecondaryColor.rgb;
    vec3 themeSoft = mix(theme, themeDark, 0.35);

    vec3 zenith = mix(vec3(0.02, 0.028, 0.05), themeDark * 0.35, 0.55);
    vec3 midSky = mix(vec3(0.045, 0.06, 0.095), themeSoft * 0.45, 0.5);
    vec3 horizon = mix(vec3(0.07, 0.085, 0.12), themeSoft * 0.85, 0.75);

    float zenithW = smoothstep(-0.05, 0.9, elevation);
    float horizW = smoothstep(0.45, -0.25, elevation);
    vec3 col = mix(midSky, zenith, zenithW);
    vec3 horizonGlow = mix(horizon, theme, 0.55) + theme * 0.25;
    col = mix(col, horizonGlow, horizW * 0.9);

    float aboveHorizon = smoothstep(-0.35, 0.05, elevation);
    float skyWeight = mix(0.75, 1.15, smoothstep(-0.05, 0.75, elevation));

    float L0 = evaluateCloudLayer(rd, t, sc, 2.0, vec3(t * 0.014, t * 0.005, -t * 0.008), 0.20, 0.50) * aboveHorizon * skyWeight;
    float L1 = evaluateCloudLayer(rd + vec3(0.21, 0.07, -0.11), t * 1.15, sc, 3.2, vec3(-t * 0.022, t * 0.009, t * 0.006), 0.26, 0.56) * aboveHorizon * skyWeight;
    float L2 = evaluateCloudLayer(rd + vec3(-0.13, 0.18, 0.09), t * 0.85, sc, 4.4, vec3(t * 0.01, -t * 0.012, t * 0.004), 0.32, 0.62) * aboveHorizon * skyWeight;

    vec3 cDark = mix(vec3(0.08, 0.09, 0.14), themeDark * 0.55, 0.65);
    vec3 cLite = mix(vec3(0.28, 0.32, 0.42), themeSoft * 0.9, 0.55);
    vec3 cDeep = mix(vec3(0.12, 0.10, 0.18), theme * 0.4, 0.7);
    float shade = fractalNoise2(rd * 2.0 * sc + t * 0.01);
    vec3 cloudCol = mix(cDark, cLite, smoothstep(0.15, 0.85, shade));
    cloudCol = mix(cloudCol, cDeep, (1.0 - shade) * 0.35);
    cloudCol = mix(cloudCol, themeSoft, 0.18 + 0.22 * shade);

    col = mix(col, cloudCol, clamp(L0 * 0.98, 0.0, 1.0));
    col = mix(col, mix(cloudCol, cLite, 0.45), clamp(L1 * 0.85, 0.0, 1.0));
    col = mix(col, mix(cDark, cloudCol, 0.6), clamp(L2 * 0.75, 0.0, 1.0));

    float cover = clamp(L0 * 0.85 + L1 * 0.65 + L2 * 0.5, 0.0, 1.0);
    col += themeSoft * cover * cover * 0.22;
    col = mix(col, horizonGlow, smoothstep(0.25, -0.45, elevation) * 0.45);
    col += theme * pow(horizW, 1.4) * 0.18;

    col += accumulateBolts(rd, LightningTime, SkyParams.z, LightningCount);

    float gaps = (1.0 - cover) * smoothstep(0.20, 0.80, elevation);
    float star = step(0.994, rand3(floor(rd * 160.0))) * gaps;
    col += mix(vec3(0.8, 0.88, 1.0), theme * 1.2 + 0.55, 0.25) * star * 0.45;

    fragColor = vec4(pow(clamp(max(col, vec3(0.0)), 0.0, 1.0), vec3(0.9)), 1.0);
}

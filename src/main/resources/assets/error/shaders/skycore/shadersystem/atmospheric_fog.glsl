#version 150 core

// SkyCore atmosphere: distance fog, dusk veil, puddle-style wet patches.
// Reflections NEVER sample the color buffer (SSR) — that glued ghosts to the camera.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BlurSampler;
uniform mat4 InvProjection;
uniform mat4 InvViewProjection;
uniform mat4 Projection;

uniform vec3 FogColor;
uniform vec3 SunDirection;
uniform vec3 WorldUpView;
uniform vec2 Texel;

// x=density y=softness z=unused w=reverseZ
uniform vec4 AtmosParams;
// x=fogStart y=fogEnd z=duskAmount w=distanceBlur
uniform vec4 FogRange;
// x=blockReflect y=scatterHeight z=waterReflect w=minUp
uniform vec4 Extra;
// x=near y=far
uniform vec4 ClipRange;

in vec2 fragCoord;
out vec4 fragColor;

bool depthIsSky(float z) {
    return AtmosParams.w > 0.5 ? (z <= 1.0e-5) : (z >= 0.99999);
}

// Item entities, dropped loot, grass/flower cross-models — depth jumps in a 3x3.
float thinOverlayMask(vec2 uv, float z) {
    if (depthIsSky(z)) {
        return 0.0;
    }
    bool revZ = AtmosParams.w > 0.5;
    float dL = texture(DepthSampler, uv + vec2(-Texel.x, 0.0)).r;
    float dR = texture(DepthSampler, uv + vec2( Texel.x, 0.0)).r;
    float dU = texture(DepthSampler, uv + vec2(0.0,  Texel.y)).r;
    float dD = texture(DepthSampler, uv + vec2(0.0, -Texel.y)).r;
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

float clipFromDepth(float z) {
    return AtmosParams.w > 0.5 ? z : (z * 2.0 - 1.0);
}

vec3 reconstructView(vec2 uv, float z) {
    vec4 clip = vec4(uv * 2.0 - 1.0, clipFromDepth(z), 1.0);
    vec4 v = InvProjection * clip;
    return v.xyz / max(abs(v.w), 1e-6);
}

vec3 reconstructWorld(vec2 uv, float z) {
    vec4 clip = vec4(uv * 2.0 - 1.0, clipFromDepth(z), 1.0);
    vec4 w = InvViewProjection * clip;
    return w.xyz / max(abs(w.w), 1e-6);
}

float measureDistance(vec2 uv, float z, float skyBit) {
    float farLimit = max(ClipRange.y, 64.0);
    if (skyBit > 0.5) {
        return farLimit * 0.92;
    }
    float nearLimit = max(ClipRange.x, 0.05);
    float len = length(reconstructView(uv, z));
    if (len > nearLimit * 0.5 && len < farLimit * 1.5) {
        return len;
    }
    float approx;
    if (AtmosParams.w > 0.5) {
        approx = nearLimit / max(z, 1e-4);
    } else {
        float ndcZ = z * 2.0 - 1.0;
        approx = (2.0 * nearLimit * farLimit)
            / max(farLimit + nearLimit - ndcZ * (farLimit - nearLimit), 1e-4);
    }
    return clamp(approx, nearLimit, farLimit);
}

float computeFogFactor(float dist, float skyBit) {
    float dens = AtmosParams.x;
    if (dens < 0.01) {
        return 0.0;
    }
    float farLimit = max(ClipRange.y, 48.0);
    float fogFar = min(max(FogRange.y, FogRange.x + 8.0), farLimit * 0.78);
    float fogNear = min(max(FogRange.x, 1.0), fogFar * 0.4);
    float softPow = max(AtmosParams.y, 0.25);

    float ramp = smoothstep(fogNear, fogFar, dist);
    ramp = pow(clamp(ramp, 0.0, 1.0), softPow * 1.05);
    float edge = 1.0 - smoothstep(fogFar * 0.82, farLimit * 0.98, dist);
    ramp *= mix(0.55, 1.0, edge);

    if (skyBit > 0.5) {
        float upScreen = 1.0 - fragCoord.y;
        ramp *= mix(0.2, 0.75, 1.0 - upScreen * 0.9);
    } else {
        float heightScale = clamp(Extra.y / 80.0, 0.35, 1.0);
        ramp *= mix(1.0, heightScale, 0.15);
    }
    ramp *= mix(1.0, 1.08, FogRange.z);
    return clamp(1.0 - exp(-dens * ramp * 0.95), 0.0, 0.62);
}

float surfaceWetness(vec3 albedo, float upAlign) {
    float luma = dot(albedo, vec3(0.299, 0.587, 0.114));
    float blue = albedo.b - max(albedo.r, albedo.g);
    float flatness = smoothstep(0.82, 0.97, upAlign);
    float waterHue = smoothstep(0.06, 0.22, blue) * smoothstep(0.12, 0.4, albedo.b);
    float notGreen = 1.0 - smoothstep(0.0, 0.12, albedo.g - albedo.r);
    float notBlack = smoothstep(0.04, 0.14, luma);
    return clamp(flatness * waterHue * notGreen * notBlack, 0.0, 1.0);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm2(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = mat2(0.8, -0.6, 0.6, 0.8);
    for (int i = 0; i < 4; i++) {
        v += a * noise2(p);
        p = m * p * 2.05 + vec2(11.7, 3.1);
        a *= 0.5;
    }
    return v;
}

// Round puddle blobs snapped to world cells — visible on grass/dirt/stone tops.
float puddleMask(vec3 worldPos, float coverage) {
    float cell = mix(2.8, 5.5, clamp(coverage, 0.0, 1.0));
    vec2 baseCell = floor(worldPos.xz / cell);
    float best = 0.0;

    for (int oy = -1; oy <= 1; oy++) {
        for (int ox = -1; ox <= 1; ox++) {
            vec2 cid = baseCell + vec2(float(ox), float(oy));
            float rnd = hash21(cid);
            // coverage 0→few puddles, 1→most cells wet
            if (rnd > mix(0.92, 0.28, clamp(coverage, 0.0, 1.0))) {
                continue;
            }
            vec2 rnd2 = vec2(hash21(cid + 17.3), hash21(cid + 41.7));
            vec2 center = (cid + rnd2 * 0.72 + 0.14) * cell;
            float radius = cell * mix(0.42, 0.78, hash21(cid + 91.1));
            float dist = length(worldPos.xz - center);
            float blob = 1.0 - smoothstep(radius * 0.55, radius, dist);
            blob *= smoothstep(radius * 0.08, radius * 0.35, radius - dist);
            best = max(best, blob);
        }
    }
    return clamp(best, 0.0, 1.0);
}

// Soft environment tint inside puddles — no razor sun disk.
vec3 puddleSkyColor(vec3 reflectDir, float waterMix) {
    vec3 R = normalize(reflectDir);
    vec3 up = normalize(WorldUpView);
    float elev = dot(R, up);
    vec3 zenith = mix(FogColor * 0.42, vec3(0.09, 0.12, 0.2), 0.45);
    vec3 horizon = mix(FogColor * 0.95, vec3(0.42, 0.5, 0.62), 0.3);
    vec3 env = mix(horizon, zenith, smoothstep(-0.15, 0.7, elev));
    env = mix(FogColor * 0.16, env, smoothstep(-0.5, 0.05, elev));

    vec3 L = normalize(SunDirection);
    float sunDot = max(dot(R, L), 0.0);
    float day = smoothstep(-0.1, 0.25, L.y);
    // Broad soft sheen only — never thin stripes.
    float glow = smoothstep(0.35, 0.95, sunDot);
    glow = glow * glow;
    env += vec3(1.0, 0.95, 0.86) * glow * mix(0.06, 0.14, waterMix) * day;
    return env;
}

vec3 applyWetLayer(vec2 uv, float z, vec3 scene, float skyBit) {
    float blockAmt = Extra.x;
    float waterAmt = Extra.z;
    float minUp = clamp(Extra.w, 0.2, 0.98);
    if (skyBit > 0.5 || (blockAmt < 0.01 && waterAmt < 0.01)) {
        return scene;
    }
    if (thinOverlayMask(uv, z) > 0.5) {
        return scene;
    }

    float zC = z;
    float zX = texture(DepthSampler, uv + vec2(Texel.x * 1.5, 0.0)).r;
    float zY = texture(DepthSampler, uv + vec2(0.0, Texel.y * 1.5)).r;
    if (depthIsSky(zX) || depthIsSky(zY)) {
        return scene;
    }
    if (abs(zX - zC) > 0.025 || abs(zY - zC) > 0.025) {
        return scene;
    }

    vec3 pC = reconstructView(uv, zC);
    vec3 pX = reconstructView(uv + vec2(Texel.x * 1.5, 0.0), zX);
    vec3 pY = reconstructView(uv + vec2(0.0, Texel.y * 1.5), zY);
    vec3 n = normalize(cross(pX - pC, pY - pC));
    if (dot(n, normalize(-pC)) < 0.0) {
        n = -n;
    }

    vec3 up = normalize(WorldUpView);
    float upAlign = abs(dot(n, up));
    if (upAlign < minUp) {
        return scene;
    }

    float flatLock = smoothstep(minUp, 0.97, upAlign);
    float upSign = dot(n, up) >= 0.0 ? 1.0 : -1.0;
    n = normalize(mix(n, up * upSign, flatLock * 0.7));

    float wetScore = surfaceWetness(scene, upAlign);
    float waterMix = smoothstep(0.35, 0.75, wetScore);

    float luma = dot(scene, vec3(0.299, 0.587, 0.114));
    // All flat block tops — grass, dirt, stone (no grass exclusion).
    float blockEligible = (1.0 - waterMix) * flatLock * smoothstep(0.02, 0.06, luma);

    vec3 worldPos = reconstructWorld(uv, zC);
    float puddles = mix(puddleMask(worldPos, blockAmt), 1.0, waterMix);
    float strength = mix(blockAmt * blockEligible, waterAmt * flatLock, waterMix) * puddles;
    if (strength < 0.008) {
        return scene;
    }

    float startLen = length(pC);
    vec3 I = normalize(pC);
    vec3 R = reflect(I, n);
    vec3 mirrorCol = puddleSkyColor(R, waterMix);
    // Pull ambience/fog tint into puddles so they read clearly at night.
    mirrorCol = mix(mirrorCol, FogColor * 1.45, mix(0.35, 0.65, 1.0 - waterMix));
    mirrorCol = mix(mirrorCol, texture(BlurSampler, uv).rgb, 0.28 * strength);

    vec3 V = normalize(-pC);
    float ndotv = clamp(dot(n, V), 0.0, 1.0);
    float fresnel = pow(1.0 - ndotv, mix(2.0, 1.2, waterMix));

    float wetDark = mix(0.48, 0.58, waterMix);
    vec3 wetBase = scene * mix(1.0, wetDark, clamp(strength * 1.1, 0.0, 1.0));
    float blend = strength * mix(0.42, 0.72, fresnel);
    blend *= smoothstep(0.25, 1.5, startLen);
    blend *= smoothstep(110.0, 22.0, startLen);
    blend = clamp(blend, 0.0, mix(0.62, 0.82, waterMix));

    return clamp(mix(wetBase, mirrorCol, blend), vec3(0.0), vec3(8.0));
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec3 scene = max(texture(SceneSampler, uv).rgb, vec3(0.0));
    float z = texture(DepthSampler, uv).r;
    float skyBit = depthIsSky(z) ? 1.0 : 0.0;

    float dist = measureDistance(uv, z, skyBit);
    if (!(dist > 0.0) || dist > 1.0e6) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    scene = applyWetLayer(uv, z, scene, skyBit);

    float overlay = thinOverlayMask(uv, z);
    float fogAmt = computeFogFactor(dist, skyBit) * (1.0 - overlay * 0.85);
    vec3 mist = FogColor;
    float dayCurve = clamp(SunDirection.y * 0.5 + 0.5, 0.0, 1.0);
    mist *= mix(vec3(1.1, 0.95, 0.82), vec3(0.85, 0.9, 1.05), dayCurve);
    if (FogRange.z > 0.01) {
        vec3 duskTint = mix(vec3(0.06, 0.09, 0.16), FogColor * 0.55, 0.35);
        mist = mix(mist, duskTint, FogRange.z * 0.85);
    }

    vec3 color = mix(scene, mist, fogAmt);

    float blurAmt = FogRange.w;
    if (blurAmt > 0.01) {
        float blurT = smoothstep(FogRange.x * 0.75, FogRange.y, dist) * blurAmt;
        if (skyBit > 0.5) {
            blurT *= 0.45;
        }
        color = mix(color, texture(BlurSampler, uv).rgb, clamp(blurT * 0.7, 0.0, 0.65));
    }

    fragColor = vec4(clamp(color, 0.0, 8.0), 1.0);
}

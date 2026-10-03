#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 InvProjection;
uniform mat4 InvView;
uniform vec4 Tint;
uniform vec4 Params;
uniform vec4 CameraTime;
uniform vec4 RainStyle;
uniform vec4 PerfParams;

in vec2 fragCoord;
out vec4 fragColor;

float pseudoRand2(vec2 p) {
    return fract(sin(dot(p, vec2(91.345, 47.291))) * 28421.7313);
}

vec2 pseudoRand2v(vec2 p) {
    vec2 q = vec2(dot(p, vec2(91.345, 47.291)), dot(p, vec2(173.2, 119.7)));
    return fract(sin(q) * 28421.7313);
}

bool depthIsSky(float depth) {
    bool reverseZ = Params.w > 0.5;
    return reverseZ ? (depth <= 0.000001) : (depth >= 0.99999);
}

float thinOverlayMask(vec2 uv, float z) {
    if (depthIsSky(z)) {
        return 0.0;
    }
    bool revZ = Params.w > 0.5;
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

vec3 unprojectView(vec2 uv, float depth) {
    float clipZ = Params.w > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 clip = InvProjection * vec4(uv * 2.0 - 1.0, clipZ, 1.0);
    return clip.xyz / max(abs(clip.w), 1e-6);
}

vec3 unprojectWorld(vec2 uv, float depth) {
    vec3 viewPos = unprojectView(uv, depth);
    return CameraTime.xyz + (InvView * vec4(viewPos, 0.0)).xyz;
}

float topSurfaceWeight(vec2 uv, float depth) {
    ivec2 texSize = max(textureSize(DepthSampler, 0), ivec2(1));
    vec2 texel = 1.0 / vec2(texSize);
    vec2 offsets[4];
    offsets[0] = vec2(-texel.x, 0.0);
    offsets[1] = vec2( texel.x, 0.0);
    offsets[2] = vec2(0.0, -texel.y);
    offsets[3] = vec2(0.0,  texel.y);
    vec3 neighbors[4];
    for (int k = 0; k < 4; k++) {
        vec2 sampleUv = uv + offsets[k];
        float sampleDepth = texture(DepthSampler, sampleUv).r;
        if (depthIsSky(sampleDepth)) {
            return 0.0;
        }
        neighbors[k] = unprojectWorld(sampleUv, sampleDepth);
    }
    vec3 tangentX = neighbors[1] - neighbors[0];
    vec3 tangentZ = neighbors[3] - neighbors[2];
    vec3 normal = normalize(cross(tangentX, tangentZ));
    return smoothstep(0.72, 0.92, abs(normal.y));
}

float streakRadialDist(vec3 samplePos, vec2 cellId, float cellSize, float time,
                       float dropLen, float fallSpeed, out float alongDrop) {
    vec2 cellRand = pseudoRand2v(cellId);
    float phaseOffset = cellRand.x * 6.2831;
    float speed = max(fallSpeed, 0.15) * 0.28;
    float travelRange = 22.0 + dropLen * 2.0;
    float fallOffset = fract(time * speed + cellRand.y + phaseOffset * 0.05) * travelRange;
    float headY = samplePos.y + (travelRange * 0.55) - fallOffset;
    float tailY = headY - dropLen;
    vec2 dropCenter = (cellId + cellRand) * cellSize;
    alongDrop = clamp((headY - samplePos.y) / max(dropLen, 0.05), 0.0, 1.0);
    if (samplePos.y > headY + 0.35 || samplePos.y < tailY - 0.35) {
        return 1e3;
    }
    return length(samplePos.xz - dropCenter);
}

float groundRippleField(vec3 worldPos, float time, float ringLife, float cellSize) {
    vec2 baseCell = floor(worldPos.xz / cellSize);
    float rippleSum = 0.0;
    int neighborRadius = int(clamp(PerfParams.y, 0.0, 1.0));
    for (int oy = -1; oy <= 1; oy++) {
        for (int ox = -1; ox <= 1; ox++) {
            if (abs(ox) > neighborRadius || abs(oy) > neighborRadius) {
                continue;
            }
            vec2 cellId = baseCell + vec2(float(ox), float(oy));
            if (pseudoRand2(cellId + 3.1) > 0.55) {
                continue;
            }
            vec2 cellRand = pseudoRand2v(cellId + 17.3);
            float cyclePeriod = max(ringLife, 0.35) * (0.85 + cellRand.y * 0.35);
            float ringAge = fract(time * 0.7 / cyclePeriod + cellRand.x);
            float ringRadius = ringAge * (0.18 + cellRand.x * 0.16);
            vec2 ringCenter = (cellId + cellRand) * cellSize;
            float radialDist = length(worldPos.xz - ringCenter);
            float outline = 1.0 - smoothstep(0.0, 0.04, abs(radialDist - ringRadius));
            float innerFill = (1.0 - smoothstep(0.0, ringRadius * 0.35, radialDist)) * (1.0 - ringAge) * 0.25;
            float lifeFade = smoothstep(0.0, 0.08, ringAge) * (1.0 - smoothstep(0.45, 0.95, ringAge));
            rippleSum = max(rippleSum, (outline + innerFill) * lifeFade);
        }
    }
    return rippleSum;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float rawDepth = texture(DepthSampler, uv).r;

    float farDepth = Params.w > 0.5 ? 0.0 : 0.999999;
    vec3 viewFar = unprojectView(uv, farDepth);
    vec3 rayDir = normalize((InvView * vec4(viewFar, 0.0)).xyz);

    float maxDist = max(Params.z, 16.0);
    bool skyPixel = depthIsSky(rawDepth);
    float sceneDist = skyPixel ? maxDist : length(unprojectView(uv, rawDepth));
    float marchEnd = min(sceneDist, maxDist);

    float time = CameraTime.w;
    vec3 cam = CameraTime.xyz;
    float intensity = max(Params.x, 0.15);
    float density = clamp(Params.y, 0.15, 1.0);
    float dropLen = max(RainStyle.x, 1.2);
    float fallSpeed = max(RainStyle.y, 0.2);
    float ringLife = max(RainStyle.z, 0.45);
    float cellSize = max(RainStyle.w, 1.1);

    int marchSteps = int(clamp(PerfParams.x, 6.0, 14.0));
    float invSteps = 1.0 / float(marchSteps);
    float streak = 0.0;

    for (int step = 0; step < 14; step++) {
        if (step >= marchSteps) {
            break;
        }
        float marchT = marchEnd * ((float(step) + 0.3) * invSteps);
        vec3 samplePos = cam + rayDir * marchT;
        vec2 gridCell = floor(samplePos.xz / cellSize);
        float stepBest = 0.0;
        float marchFrac = (float(step) + 0.3) * invSteps;

        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                vec2 cellId = gridCell + vec2(float(ox), float(oy));
                if (pseudoRand2(cellId) > density) {
                    continue;
                }
                float alongDrop;
                float radial = streakRadialDist(samplePos, cellId, cellSize, time,
                                                dropLen, fallSpeed, alongDrop);
                float halfWidth = mix(0.028, 0.095, alongDrop * alongDrop);
                halfWidth *= mix(1.0, 1.55, marchFrac);
                float body = 1.0 - smoothstep(halfWidth * 0.2, halfWidth, radial);
                float tipFade = smoothstep(0.0, 0.1, alongDrop);
                float bulb = mix(0.75, 1.45, pow(alongDrop, 1.35));
                stepBest = max(stepBest, body * tipFade * bulb);
            }
        }
        streak = max(streak, stepBest * mix(1.2, 0.85, marchFrac));
    }

    float rings = 0.0;
    if (!skyPixel && sceneDist < maxDist * 0.98 && thinOverlayMask(uv, rawDepth) < 0.5) {
        vec3 hitPos = cam + rayDir * sceneDist;
        float lookDown = smoothstep(-0.15, 0.35, -rayDir.y);
        float heightOk = smoothstep(cam.y + 6.0, cam.y - 1.0, hitPos.y);
        float nearGround = smoothstep(maxDist * 0.95, 2.0, sceneDist);
        float flatTop = PerfParams.z > 0.5 ? 1.0 : topSurfaceWeight(uv, rawDepth);
        float groundFactor = max(lookDown * 0.85, heightOk) * nearGround * flatTop;
        if (groundFactor > 0.02) {
            rings = groundRippleField(hitPos, time, ringLife, cellSize) * mix(0.55, 1.25, groundFactor);
        }
    }

    float dropAlpha = streak * intensity * 1.35;
    float ringAlpha = rings * intensity * 1.65;
    float alpha = clamp(max(dropAlpha, ringAlpha), 0.0, 1.0);
    if (alpha < 0.01) {
        discard;
    }

    vec3 baseCol = max(Tint.rgb, vec3(0.25));
    vec3 ringCol = min(baseCol * 1.7 + vec3(0.25), vec3(1.7));
    vec3 col = mix(baseCol * 0.95, min(baseCol * 1.55 + vec3(0.18), vec3(1.6)), clamp(streak, 0.0, 1.0));
    col = mix(col, ringCol, clamp(rings * 1.1, 0.0, 1.0));
    fragColor = vec4(col, alpha);
}

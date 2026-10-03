#version 150 core

in vec2 fragCoord;

uniform vec4 u_Color;
uniform vec3 u_Color2;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Night;
uniform float u_Lightning;

out vec4 fragColor;

#define TAU 6.28318530718
#define PI  3.14159265359

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
    float bx0 = mix(c000, c100, fracPart.x);
    float bx1 = mix(c010, c110, fracPart.x);
    float bx2 = mix(c001, c101, fracPart.x);
    float bx3 = mix(c011, c111, fracPart.x);
    float by0 = mix(bx0, bx1, fracPart.y);
    float by1 = mix(bx2, bx3, fracPart.y);
    return mix(by0, by1, fracPart.z);
}

float fractalNoise3(vec3 p) {
    float accum = 0.0;
    float weight = 0.5;
    vec3 coord = p;
    for (int i = 0; i < 3; i++) {
        accum += weight * trilinearNoise(coord);
        coord = coord * 2.05 + vec3(1.7, -1.3, 0.9);
        weight *= 0.5;
    }
    return accum;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    vec2 ap = p - a;
    float proj = clamp(dot(ap, ab) / max(dot(ab, ab), 1e-5), 0.0, 1.0);
    return length(ap - ab * proj);
}

float evaluateCloudLayer(vec3 rd, float t, float sc, float scale, vec3 drift, float lo, float hi) {
    vec3 samplePos = rd * scale * sc + drift;
    samplePos.y *= 0.55;
    vec3 warp = vec3(
        fractalNoise3(samplePos + t * 0.02),
        fractalNoise3(samplePos + vec3(3.1, -t * 0.015, 1.4)),
        fractalNoise3(samplePos + vec3(-1.7, 2.2, t * 0.01))
    );
    float density = fractalNoise3(samplePos + warp * 1.05);
    return smoothstep(lo, hi, density);
}

vec3 renderLightning(vec3 rd, float t, out float flashAmt) {
    flashAmt = 0.0;
    if (u_Lightning < 0.5) {
        return vec3(0.0);
    }

    vec2 sphereUv = vec2(atan(rd.z, rd.x) / TAU + 0.5, acos(clamp(rd.y, -1.0, 1.0)) / PI);

    float period = 7.2;
    float cycleIdx = floor(t / period);
    float phase = fract(t / period);

    float flash = smoothstep(0.0, 0.012, phase) * (1.0 - smoothstep(0.05, 0.14, phase));
    flash += 0.55 * smoothstep(0.16, 0.175, phase) * (1.0 - smoothstep(0.19, 0.26, phase));
    flashAmt = flash;
    if (flash < 0.01) {
        return vec3(0.0);
    }

    float anchorX = 0.18 + rand3(vec3(cycleIdx, 3.1, 0.7)) * 0.64;
    float topY = 0.02 + rand3(vec3(cycleIdx, 7.7, 1.2)) * 0.05;
    float botY = 0.40 + rand3(vec3(cycleIdx, 13.3, 2.1)) * 0.28;

    float minDist = 1e5;
    vec2 prevPoint = vec2(anchorX, topY);
    for (int seg = 1; seg <= 7; seg++) {
        float segT = float(seg) / 7.0;
        float segY = mix(topY, botY, segT);
        float jag = (rand3(vec3(cycleIdx, float(seg) * 19.17, 4.4)) - 0.5) * (0.05 + 0.1 * segT);
        vec2 curPoint = vec2(anchorX + jag, segY);
        minDist = min(minDist, segmentDistance(sphereUv, prevPoint, curPoint));
        prevPoint = curPoint;
    }

    float core = smoothstep(0.004, 0.0, minDist);
    float glow = smoothstep(0.035, 0.0, minDist);
    return (vec3(0.9, 0.95, 1.0) * core * 2.2 + vec3(0.5, 0.7, 1.0) * glow * 0.9) * flash;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float theta = uv.x * TAU;
    float phi = uv.y * PI;
    vec3 rd = normalize(vec3(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta)));
    float elevation = rd.y;
    float t = u_Time;
    float sc = max(0.5, u_Scale);

    vec3 zenith = vec3(0.015, 0.022, 0.045);
    vec3 midSky = vec3(0.045, 0.060, 0.095);
    vec3 horizon = vec3(0.085, 0.105, 0.145);
    vec3 accent = mix(u_Color.rgb, u_Color2, 0.55) * 0.18;

    float zenithW = smoothstep(-0.05, 0.9, elevation);
    float horizW = smoothstep(0.45, -0.25, elevation);
    vec3 col = mix(midSky, zenith, zenithW);
    col = mix(col, horizon + accent * 0.3, horizW * 0.85);

    float band = smoothstep(-0.15, 0.12, elevation) * (1.0 - smoothstep(0.52, 0.92, elevation));

    float L0 = evaluateCloudLayer(rd, t, sc, 2.4, vec3(t * 0.014, t * 0.005, -t * 0.008), 0.28, 0.58) * band;
    float L1 = evaluateCloudLayer(rd + vec3(0.21, 0.07, -0.11), t * 1.2, sc, 3.8, vec3(-t * 0.022, t * 0.009, t * 0.006), 0.34, 0.66) * band * 0.9;

    vec3 cDark = vec3(0.12, 0.14, 0.20);
    vec3 cLite = vec3(0.32, 0.36, 0.46);
    vec3 cViol = vec3(0.18, 0.15, 0.26);

    float shade = fractalNoise3(rd * vec3(1.8, 1.1, 1.8) * sc + t * 0.01);
    vec3 cloudCol = mix(cDark, cLite, smoothstep(0.2, 0.8, shade));
    cloudCol = mix(cloudCol, cViol, (1.0 - shade) * 0.25);
    cloudCol += accent * (0.12 + 0.15 * shade);

    col = mix(col, cloudCol, clamp(L0 * 0.92, 0.0, 1.0));
    col = mix(col, mix(cloudCol, cLite, 0.35), clamp(L1 * 0.7, 0.0, 1.0));

    float cover = clamp(L0 * 0.75 + L1 * 0.5, 0.0, 1.0);
    col += vec3(0.06, 0.08, 0.14) * cover * cover * 0.45;

    float mist = smoothstep(0.25, -0.4, elevation);
    col = mix(col, horizon, mist * 0.4);

    float flashAmt;
    vec3 bolt = renderLightning(rd, t, flashAmt);
    col += vec3(0.35, 0.5, 0.8) * flashAmt * cover * 0.35;
    col += bolt;

    float gaps = (1.0 - cover) * smoothstep(0.35, 0.85, elevation);
    float star = step(0.994, rand3(floor(rd * 180.0))) * gaps;
    col += vec3(0.8, 0.88, 1.0) * star * 0.4;

    col = max(col, vec3(0.0));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.9)), 1.0);
}

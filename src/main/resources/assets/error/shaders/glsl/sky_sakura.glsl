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

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
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

// 3 octaves — enough volume, much cheaper than 5–6 + domain warp.
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise2(p);
        p = p * 2.05 + vec2(1.7, -1.3);
        a *= 0.5;
    }
    return v;
}

float distSeg(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-5), 0.0, 1.0);
    return length(pa - ba * h);
}

float cloudSheet(vec2 uv, float t, float sc, float scale, vec2 drift, float lo, float hi) {
    vec2 p = uv * vec2(scale, scale * 0.55) * sc + drift;
    // Single light warp instead of double fbmSoft stacks.
    vec2 w = vec2(fbm(p + t * 0.02), fbm(p + vec2(3.1, -t * 0.015)));
    float n = fbm(p + w * 1.1);
    return smoothstep(lo, hi, n);
}

vec3 boltFlash(vec2 uv, float t, out float flashAmt, out vec2 boltCenter) {
    flashAmt = 0.0;
    boltCenter = vec2(0.5, 0.22);
    if (u_Lightning < 0.5) {
        return vec3(0.0);
    }

    float period = 7.2;
    float cycle = floor(t / period);
    float phase = fract(t / period);

    float flash = smoothstep(0.0, 0.012, phase) * (1.0 - smoothstep(0.05, 0.14, phase));
    flash += 0.55 * smoothstep(0.16, 0.175, phase) * (1.0 - smoothstep(0.19, 0.26, phase));
    flashAmt = flash;
    if (flash < 0.01) {
        return vec3(0.0);
    }

    float ax = 0.18 + hash21(vec2(cycle, 3.1)) * 0.64;
    float topY = 0.02 + hash21(vec2(cycle, 7.7)) * 0.05;
    float botY = 0.40 + hash21(vec2(cycle, 13.3)) * 0.28;
    boltCenter = vec2(ax, mix(topY, botY, 0.4));

    float d = 1e5;
    vec2 prev = vec2(ax, topY);
    for (int i = 1; i <= 7; i++) {
        float fi = float(i) / 7.0;
        float ny = mix(topY, botY, fi);
        float jag = (hash21(vec2(cycle, float(i) * 19.17)) - 0.5) * (0.05 + 0.1 * fi);
        vec2 cur = vec2(ax + jag, ny);
        d = min(d, distSeg(uv, prev, cur));
        prev = cur;
    }

    float core = smoothstep(0.004, 0.0, d);
    float glow = smoothstep(0.035, 0.0, d);
    return (vec3(0.9, 0.95, 1.0) * core * 2.2 + vec3(0.5, 0.7, 1.0) * glow * 0.9) * flash;
}

void main() {
    vec2 uv = fragCoord;
    float theta = uv.x * TAU;
    float phi = uv.y * PI;
    vec3 rd = normalize(vec3(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta)));
    float y = rd.y;
    float t = u_Time;
    float sc = max(0.5, u_Scale);

    vec2 skyUv = vec2(atan(rd.z, rd.x) / TAU + 0.5, acos(clamp(rd.y, -1.0, 1.0)) / PI);

    vec3 zenith = vec3(0.01, 0.015, 0.035);
    vec3 midSky = vec3(0.04, 0.055, 0.09);
    vec3 horizon = vec3(0.08, 0.1, 0.14);

    float zenithW = smoothstep(-0.05, 0.9, y);
    float horizW = smoothstep(0.45, -0.25, y);
    vec3 col = mix(midSky, zenith, zenithW);
    col = mix(col, horizon, horizW * 0.85);

    // Band where storm clouds live — mid sky, not crushed into horizon fog.
    float band = smoothstep(-0.15, 0.12, y) * (1.0 - smoothstep(0.55, 0.95, y));

    // Two chunky sheets — clearly brighter than the sky so they read as clouds.
    float L0 = cloudSheet(skyUv, t, sc, 2.4, vec2(t * 0.014, t * 0.005), 0.28, 0.58);
    float L1 = cloudSheet(skyUv + vec2(0.21, 0.07), t * 1.2, sc, 3.8, vec2(-t * 0.022, t * 0.009), 0.34, 0.66);
    L0 *= band;
    L1 *= band * 0.9;

    vec3 cDark = vec3(0.12, 0.14, 0.20);
    vec3 cLite = vec3(0.32, 0.36, 0.46);
    vec3 cViol = vec3(0.18, 0.15, 0.26);

    float shade = fbm(skyUv * vec2(1.8, 1.1) * sc + t * 0.01);
    vec3 cloudCol = mix(cDark, cLite, smoothstep(0.2, 0.8, shade));
    cloudCol = mix(cloudCol, cViol, (1.0 - shade) * 0.25);

    // Strong coverage so clouds are obvious, not a faint tint.
    col = mix(col, cloudCol, clamp(L0 * 0.92, 0.0, 1.0));
    col = mix(col, mix(cloudCol, cLite, 0.35), clamp(L1 * 0.7, 0.0, 1.0));

    float cover = clamp(L0 * 0.75 + L1 * 0.5, 0.0, 1.0);
    col += vec3(0.06, 0.08, 0.14) * cover * cover * 0.45;

    float mist = smoothstep(0.25, -0.4, y);
    col = mix(col, horizon, mist * 0.4);

    float flashAmt;
    vec2 boltCenter;
    vec3 bolt = boltFlash(skyUv, t, flashAmt, boltCenter);
    float distBolt = length((skyUv - boltCenter) * vec2(1.5, 1.0));
    col += vec3(0.35, 0.5, 0.8) * flashAmt * exp(-distBolt * 4.5) * cover * 0.65;
    col += bolt;

    float gaps = (1.0 - cover) * smoothstep(0.25, 0.8, y);
    float star = step(0.993, hash21(floor(skyUv * vec2(200.0, 100.0)))) * gaps;
    col += vec3(0.8, 0.88, 1.0) * star * 0.45;

    col = max(col, vec3(0.0));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.9)), 1.0);
}

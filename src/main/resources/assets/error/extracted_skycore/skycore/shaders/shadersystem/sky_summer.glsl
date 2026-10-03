#version 150 core

in vec2 fragCoord;

uniform vec4 u_Color;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Night;

out vec4 fragColor;

#define TAU 6.28318530718
#define PI  3.14159265359

float latticeRand(vec3 p) {
    return fract(sin(dot(p, vec3(91.345, 47.291, 61.17))) * 28421.731313);
}

float latticeNoise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 fracPart = fract(p);
    vec3 smoothFrac = fracPart * fracPart * (3.0 - 2.0 * fracPart);
    float c000 = latticeRand(cell);
    float c100 = latticeRand(cell + vec3(1.0, 0.0, 0.0));
    float c010 = latticeRand(cell + vec3(0.0, 1.0, 0.0));
    float c110 = latticeRand(cell + vec3(1.0, 1.0, 0.0));
    float c001 = latticeRand(cell + vec3(0.0, 0.0, 1.0));
    float c101 = latticeRand(cell + vec3(1.0, 0.0, 1.0));
    float c011 = latticeRand(cell + vec3(0.0, 1.0, 1.0));
    float c111 = latticeRand(cell + vec3(1.0, 1.0, 1.0));
    float bx0 = mix(c000, c100, smoothFrac.x);
    float bx1 = mix(c010, c110, smoothFrac.x);
    float bx2 = mix(c001, c101, smoothFrac.x);
    float bx3 = mix(c011, c111, smoothFrac.x);
    float by0 = mix(bx0, bx1, smoothFrac.y);
    float by1 = mix(bx2, bx3, smoothFrac.y);
    return mix(by0, by1, smoothFrac.z);
}

float layeredNoise3(vec3 p) {
    float accum = 0.0;
    float weight = 0.5;
    vec3 coord = p;
    for (int octave = 0; octave < 5; octave++) {
        accum += weight * latticeNoise3(coord);
        coord *= 2.07;
        weight *= 0.5;
    }
    return accum;
}

vec3 sphereDirection(vec2 uv) {
    float theta = uv.x * TAU;
    float phi = uv.y * PI;
    return vec3(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta));
}

void main() {
    vec2 uv = fragCoord;
    vec3 rd = sphereDirection(uv);
    float elevation = rd.y;
    float t = u_Time;
    float nightFactor = clamp(u_Night, 0.0, 1.0);

    float breathe = 0.93 + 0.07 * sin(t * 0.26);
    float pulseSat = 0.96 + 0.04 * sin(t * 0.19 + 1.7);
    float pulseNight = 0.88 + 0.12 * sin(t * 0.12 + 0.4);

    float hazeMask = smoothstep(0.2, -0.38, elevation);
    hazeMask *= hazeMask;
    vec3 hazeOff = vec3(
        sin(t * 2.15 + dot(rd, vec3(1.0, 0.55, 0.35)) * 12.0),
        cos(t * 1.95 + dot(rd, vec3(-0.45, 1.0, 0.5)) * 11.5),
        sin(t * 2.35 + dot(rd, vec3(0.25, -0.4, 1.0)) * 13.0)
    ) * (0.0055 * hazeMask * u_Scale * mix(1.0, 0.65, nightFactor));

    vec3 perturbed = rd + hazeOff;
    float rlen = length(perturbed);
    vec3 rs = (rlen > 1.0e-4) ? (perturbed / rlen) : rd;

    float sc = u_Scale;
    vec3 drift = vec3(t * 0.042, t * 0.024, t * 0.033);

    vec3 zenith = mix(vec3(0.04, 0.26, 0.78) * pulseSat, vec3(0.015, 0.04, 0.14) * pulseNight, nightFactor);
    vec3 horizonLight = mix(vec3(0.62, 0.86, 1.0), vec3(0.05, 0.07, 0.16), nightFactor);
    vec3 grassHint = mix(vec3(0.1, 0.42, 0.24), vec3(0.02, 0.05, 0.09), nightFactor);

    float gradMix = smoothstep(-0.38, 0.68, elevation);
    vec3 skyBase = mix(horizonLight, zenith, gradMix);
    float nearGround = (1.0 - gradMix) * smoothstep(-0.55, 0.18, elevation);
    skyBase = mix(skyBase, mix(skyBase, grassHint, 0.4), nearGround * mix(0.55, 0.35, nightFactor));

    vec3 cloudPos = rs * sc * 1.85 + drift;
    float boil = layeredNoise3(cloudPos * 2.35 + vec3(t * 0.11));
    float cLow = layeredNoise3(cloudPos + vec3(boil * 0.5, -boil * 0.38, boil * 0.22));
    float cHi = layeredNoise3(cloudPos * 1.35 - drift * 0.55 + vec3(17.3, 84.1, 31.7));
    float clouds = pow(clamp(cLow * 0.58 + cHi * 0.42, 0.0, 1.0), mix(0.82, 0.92, nightFactor));

    float puffCore = smoothstep(0.5, 0.84, clouds);
    float puffEdge = smoothstep(0.36, 0.9, clouds) * (1.0 - puffCore) * mix(0.45, 0.32, nightFactor);
    vec3 cloudTint = mix(vec3(1.0, 0.99, 0.97), vec3(0.22, 0.24, 0.32), nightFactor);

    vec3 col = mix(skyBase, cloudTint, puffCore);
    col = mix(col, mix(skyBase, cloudTint, 0.88), puffEdge);
    col += vec3(1.0, 0.92, 0.75) * puffEdge * mix(0.12, 0.02, nightFactor) * breathe;

    vec3 goldBase = rs * sc * 1.25 + drift * 0.85 + vec3(0.0, sin(t * 0.28) * 0.12, 0.0);
    float g1 = layeredNoise3(goldBase + layeredNoise3(goldBase * 1.65 + vec3(t * 0.095)) * 0.55);
    float g2 = layeredNoise3(goldBase * 2.2 - vec3(t * 0.05, t * 0.03, t * 0.04));
    float goldMask = smoothstep(0.52, 0.8, g1 * 0.55 + g2 * 0.45);
    goldMask *= smoothstep(-0.15, 0.58, elevation);
    goldMask *= 0.55 + 0.45 * sin(dot(rs, vec3(1.15, 2.05, 0.75)) * 3.0 + t * 0.7);
    goldMask *= mix(1.0, 0.08, nightFactor);
    vec3 goldCol = mix(vec3(1.0, 0.843, 0.0), vec3(1.0, 0.58, 0.15),
        sin(t * 0.45 + dot(rs, vec3(0.55, 1.95, 1.05)) * 4.0) * 0.5 + 0.5);
    vec3 tintRgb = vec3(u_Color.r, u_Color.g, u_Color.b);
    col += mix(goldCol, clamp(tintRgb, 0.15, 1.0), 0.2) * goldMask * 0.5 * breathe;

    float airLight = pow(max(0.0, 1.0 - abs(elevation) - 0.04), 2.15);
    col = mix(col, col * 1.06 + vec3(0.06, 0.07, 0.03), airLight * mix(0.38, 0.12, nightFactor));

    vec3 starPos = rs * 48.0 + vec3(t * 0.018, t * 0.011, t * 0.014);
    float starNoise = layeredNoise3(starPos);
    float twinkle = sin(t * 2.4 + starNoise * 40.0) * 0.5 + 0.5;
    float starField = smoothstep(0.72, 0.92, starNoise) * smoothstep(0.15, 0.85, elevation) * nightFactor;
    col += vec3(0.75, 0.82, 1.0) * starField * (0.35 + 0.65 * twinkle) * 0.55;

    col = clamp(col * mix(1.01 + 0.035 * sin(t * 0.21), 0.92 + 0.04 * sin(t * 0.17), nightFactor), 0.0, 1.0);
    fragColor = vec4(col, 1.0);
}

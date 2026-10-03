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
    float corners[8];
    corners[0] = latticeRand(cell);
    corners[1] = latticeRand(cell + vec3(1.0, 0.0, 0.0));
    corners[2] = latticeRand(cell + vec3(0.0, 1.0, 0.0));
    corners[3] = latticeRand(cell + vec3(1.0, 1.0, 0.0));
    corners[4] = latticeRand(cell + vec3(0.0, 0.0, 1.0));
    corners[5] = latticeRand(cell + vec3(1.0, 0.0, 1.0));
    corners[6] = latticeRand(cell + vec3(0.0, 1.0, 1.0));
    corners[7] = latticeRand(cell + vec3(1.0, 1.0, 1.0));
    float bx0 = mix(corners[0], corners[1], smoothFrac.x);
    float bx1 = mix(corners[2], corners[3], smoothFrac.x);
    float bx2 = mix(corners[4], corners[5], smoothFrac.x);
    float bx3 = mix(corners[6], corners[7], smoothFrac.x);
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

vec3 applyAtmosphericHaze(vec3 rd, float t, float scale, float mask) {
    vec3 wobble = vec3(
        sin(t * 1.4 + dot(rd, vec3(1.0, 0.45, 0.3)) * 10.0),
        cos(t * 1.2 + dot(rd, vec3(-0.4, 1.0, 0.5)) * 9.5),
        sin(t * 1.55 + dot(rd, vec3(0.25, -0.35, 1.0)) * 11.0)
    ) * (0.0035 * mask * scale);
    vec3 perturbed = rd + wobble;
    float len = length(perturbed);
    return (len > 1.0e-4) ? (perturbed / len) : rd;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec3 rd = sphereDirection(uv);
    float elevation = rd.y;
    float t = u_Time;
    float breathe = 0.98 + 0.02 * sin(t * 0.2);

    float hazeMask = smoothstep(0.28, -0.42, elevation);
    hazeMask *= hazeMask;
    vec3 rs = applyAtmosphericHaze(rd, t, u_Scale, hazeMask);

    float sc = u_Scale;
    vec3 drift = vec3(t * 0.032, t * 0.018, t * 0.025);

    vec3 zenith = vec3(0.22, 0.52, 0.88);
    vec3 midSky = vec3(0.48, 0.72, 0.94);
    vec3 horizon = vec3(0.78, 0.86, 0.92);
    vec3 groundWarm = vec3(0.55, 0.68, 0.52);

    float highBand = smoothstep(-0.08, 0.72, elevation);
    float midBand = smoothstep(-0.4, 0.32, elevation);
    vec3 skyBase = mix(mix(horizon, midSky, midBand), zenith, highBand);

    float nearGround = (1.0 - midBand) * smoothstep(-0.55, 0.15, elevation);
    skyBase = mix(skyBase, mix(skyBase, groundWarm, 0.35), nearGround * 0.45);

    vec3 cloudPos = rs * sc * 1.7 + drift;
    float boil = layeredNoise3(cloudPos * 2.25 + vec3(t * 0.09));
    float cLow = layeredNoise3(cloudPos + vec3(boil * 0.48, -boil * 0.34, boil * 0.2));
    float cHi = layeredNoise3(cloudPos * 1.32 - drift * 0.5 + vec3(17.3, 84.1, 31.7));
    float cDetail = layeredNoise3(cloudPos * 2.8 + vec3(-t * 0.04, t * 0.06, 11.2));
    float clouds = pow(clamp(cLow * 0.5 + cHi * 0.35 + cDetail * 0.15, 0.0, 1.0), 0.85);

    float cloudBand = smoothstep(-0.22, 0.5, elevation) * (1.0 - smoothstep(0.5, 0.92, elevation));
    float puffCore = smoothstep(0.5, 0.84, clouds) * cloudBand;
    float puffEdge = smoothstep(0.36, 0.88, clouds) * (1.0 - puffCore) * 0.48 * cloudBand;
    float puffSoft = smoothstep(0.28, 0.7, clouds) * cloudBand * 0.22;

    vec3 cloudLit = vec3(0.96, 0.97, 0.99);
    vec3 cloudShade = vec3(0.72, 0.78, 0.88);
    vec3 cloudTint = mix(cloudShade, cloudLit, smoothstep(0.55, 0.88, clouds));

    vec3 col = mix(skyBase, cloudTint, puffCore * 0.88);
    col = mix(col, mix(skyBase, cloudTint, 0.7), puffEdge);
    col = mix(col, mix(skyBase, cloudLit, 0.35), puffSoft);

    vec3 sunDir = normalize(vec3(0.22, 0.68, 0.52));
    float sunDot = max(0.0, dot(rs, sunDir));
    col += vec3(1.0, 0.94, 0.72) * pow(sunDot, 72.0) * 0.55;
    col += vec3(1.0, 0.88, 0.55) * pow(sunDot, 10.0) * 0.14;
    col += vec3(0.95, 0.8, 0.45) * pow(sunDot, 3.0) * 0.06 * breathe;

    float rayMask = pow(max(0.0, sunDot), 4.0) * (1.0 - puffCore * 0.7);
    float rays = smoothstep(0.45, 0.78, layeredNoise3(rs * sc * 3.4 + vec3(t * 0.045, 0.0, -t * 0.035)));
    col += vec3(1.0, 0.92, 0.7) * rayMask * rays * 0.055 * breathe;

    vec3 goldBase = rs * sc * 1.2 + drift * 0.8;
    float g1 = layeredNoise3(goldBase + layeredNoise3(goldBase * 1.55) * 0.45);
    float goldMask = smoothstep(0.58, 0.82, g1);
    goldMask *= smoothstep(-0.05, 0.55, elevation) * (1.0 - puffCore * 0.5);
    goldMask *= 0.55 + 0.45 * sin(dot(rs, vec3(1.1, 2.0, 0.7)) * 2.8 + t * 0.55);
    vec3 goldCol = mix(vec3(1.0, 0.82, 0.35), vec3(1.0, 0.62, 0.25), 0.5 + 0.5 * sin(t * 0.35));
    vec3 tintRgb = clamp(vec3(u_Color.r, u_Color.g, u_Color.b), 0.2, 1.0);
    col += mix(goldCol, tintRgb, 0.18) * goldMask * 0.18 * breathe;

    float airLight = pow(max(0.0, 1.0 - abs(elevation) - 0.03), 2.0);
    col = mix(col, col * 1.03 + vec3(0.04, 0.045, 0.025), airLight * 0.28);
    col = mix(col, col * mix(vec3(1.0), tintRgb, 0.28), 0.1);

    fragColor = vec4(clamp(col * breathe, 0.0, 1.0), 1.0);
}

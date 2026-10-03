#version 150 core

in vec2 fragCoord;

uniform vec4 u_Color;
uniform vec3 u_Color2;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Night;

out vec4 fragColor;

float pairRand(vec2 p) {
    p = fract(p * vec2(443.897, 397.297));
    p += dot(p, p + 23.317);
    return fract(p.x * p.y);
}

float latticeNoise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 fracPart = fract(p);
    fracPart = fracPart * fracPart * (3.0 - 2.0 * fracPart);
    float a = pairRand(cell);
    float b = pairRand(cell + vec2(1.0, 0.0));
    float c = pairRand(cell + vec2(0.0, 1.0));
    float d = pairRand(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, fracPart.x), mix(c, d, fracPart.x), fracPart.y);
}

float layeredNoise2(vec2 p) {
    float accum = 0.0;
    float weight = 0.5;
    vec2 coord = p;
    mat2 rot = mat2(1.62, -1.18, 1.18, 1.62);
    for (int octave = 0; octave < 4; octave++) {
        accum += latticeNoise2(coord) * weight;
        coord = rot * coord + vec2(5.2, 2.7);
        weight *= 0.5;
    }
    return accum;
}

void main() {
    vec2 uv = fragCoord;
    float theta = uv.x * 6.2831853;
    float elevation = uv.y * 2.0 - 1.0;
    vec2 polar = vec2(cos(theta), sin(theta)) * (1.15 + elevation * 0.18);
    polar.y += elevation * 1.25;
    polar *= max(0.35, u_Scale);

    float t = u_Time * 0.45;
    float baseNoise = layeredNoise2(polar * 1.35 + vec2(t * 0.32, -t * 0.21));
    vec2 warped = polar + vec2(baseNoise * 0.55, layeredNoise2(polar * 2.0 - t * 0.18) * 0.42);

    float veinA = abs(sin(warped.x * 4.4 + warped.y * 2.1 + t));
    float veinB = abs(sin(warped.x * -2.8 + warped.y * 5.0 - t * 0.82)) * 0.6;
    float veins = pow(1.0 - smoothstep(0.18, 0.85, (veinA + veinB) * 0.5), 1.35);

    vec3 base = mix(u_Color2, u_Color.rgb, smoothstep(-0.8, 1.0, elevation));
    vec3 glow = mix(u_Color.rgb, vec3(0.25, 0.95, 1.0), 0.45);
    vec3 col = base * (0.16 + 0.22 * smoothstep(-1.0, 1.0, elevation));
    col += glow * (veins * 0.82 + smoothstep(0.35, 0.95, baseNoise) * 0.34);
    col += mix(u_Color.rgb, vec3(1.0), 0.5) * pow(max(0.0, veins), 3.0) * 0.42;
    col *= mix(1.0, 0.82, clamp(u_Night, 0.0, 1.0));
    col = col / (col + vec3(0.82));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.92)), 1.0);
}

#version 150 core

in vec2 fragCoord;

uniform vec4 u_Color;
uniform vec3 u_Color2;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Night;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(443.897, 397.297));
    p += dot(p, p + 23.317);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = mat2(1.62, -1.18, 1.18, 1.62) * p + vec2(5.2, 2.7);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = fragCoord;
    float theta = uv.x * 6.2831853;
    float y = uv.y * 2.0 - 1.0;
    vec2 p = vec2(cos(theta), sin(theta)) * (1.15 + y * 0.18);
    p.y += y * 1.25;
    p *= max(0.35, u_Scale);

    float t = u_Time * 0.28;
    vec2 q = p * 1.22 + vec2(t * 0.085, -t * 0.062);
    float n1 = fbm(q);
    vec2 warp = vec2(n1 * 0.55, fbm(q * 1.35 - t * 0.12) * 0.48);
    float n2 = fbm(q * 1.85 + warp + vec2(-t * 0.07, t * 0.05));
    float n3 = fbm(q * 2.55 - warp * 0.65 + vec2(t * 0.04, t * 0.09));

    float body = smoothstep(0.22, 0.82, n1 * 0.58 + n2 * 0.52);
    float ridges = smoothstep(0.40, 0.88, n2 * 0.65 + n3 * 0.45);
    float cores = pow(clamp(n2 * n3, 0.0, 1.0), 2.4);
    float veil = smoothstep(0.18, 0.70, n1) * (0.55 + 0.45 * n3);

    vec3 dark = mix(u_Color2 * 0.22, vec3(0.012, 0.010, 0.028), 0.45);
    vec3 col = mix(dark, u_Color2 * 0.35, smoothstep(-1.0, 0.35, y) * 0.55);

    vec3 cloudA = mix(u_Color.rgb, u_Color2, n2);
    vec3 cloudB = mix(u_Color2, mix(u_Color.rgb, vec3(0.85, 0.45, 1.0), 0.28), n3);
    col += cloudA * body * 0.72;
    col += cloudB * ridges * 0.48;
    col += mix(u_Color.rgb, vec3(1.0, 0.82, 0.95), 0.40) * cores * 0.85;
    col += mix(u_Color.rgb, u_Color2, 0.5) * veil * 0.22;

    float altitude = 0.55 + 0.45 * smoothstep(-0.85, 1.0, y);
    col *= altitude;

    vec2 cell = floor((p * 18.0 + vec2(t * 0.08, 0.0)));
    vec2 local = fract(p * 18.0) - 0.5;
    float h = hash21(cell);
    float star = smoothstep(0.030, 0.0, length(local)) * step(0.988, h);
    star *= (0.65 + 0.35 * sin(t * 2.1 + h * 36.0));
    col += vec3(0.90, 0.93, 1.0) * star * (1.0 - body * 0.72) * 0.55;

    col *= mix(1.0, 0.80, clamp(u_Night, 0.0, 1.0));
    col = col / (col + vec3(0.78));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.90)), 1.0);
}

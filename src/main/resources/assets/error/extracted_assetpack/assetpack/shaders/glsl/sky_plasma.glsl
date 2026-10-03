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

    float t = u_Time * 0.45;
    float n = fbm(p * 1.35 + vec2(t * 0.32, -t * 0.21));
    vec2 q = p + vec2(n * 0.55, fbm(p * 2.0 - t * 0.18) * 0.42);
    float veins = abs(sin(q.x * 4.4 + q.y * 2.1 + t));
    veins += abs(sin(q.x * -2.8 + q.y * 5.0 - t * 0.82)) * 0.6;
    veins = pow(1.0 - smoothstep(0.18, 0.85, veins * 0.5), 1.35);

    vec3 base = mix(u_Color2, u_Color.rgb, smoothstep(-0.8, 1.0, y));
    vec3 glow = mix(u_Color.rgb, vec3(0.25, 0.95, 1.0), 0.45);
    vec3 col = base * (0.16 + 0.22 * smoothstep(-1.0, 1.0, y));
    col += glow * (veins * 0.82 + smoothstep(0.35, 0.95, n) * 0.34);
    col += mix(u_Color.rgb, vec3(1.0), 0.5) * pow(max(0.0, veins), 3.0) * 0.42;
    col *= mix(1.0, 0.82, clamp(u_Night, 0.0, 1.0));
    col = col / (col + vec3(0.82));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.92)), 1.0);
}

#version 150 core

in vec3 vWorldDir;

uniform vec4 u_Color;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Alpha;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p *= 2.05;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec3 dir = normalize(vWorldDir);
    vec2 uv = vec2(atan(dir.x, dir.z), asin(clamp(dir.y, -1.0, 1.0))) * u_Scale;
    float t = u_Time * 0.3;
    float veil = fbm(uv * 1.8 + vec2(t * 0.4, -t * 0.25));
    float ribbons = abs(sin(uv.x * 3.0 + veil * 4.0 + t));
    ribbons = pow(1.0 - smoothstep(0.1, 0.9, ribbons), 1.6);
    vec3 col = u_Color.rgb * (0.2 + 0.35 * veil);
    col += mix(u_Color.rgb, vec3(0.7, 0.95, 1.0), 0.4) * ribbons * 0.9;
    col *= mix(vec3(1.0), clamp(u_Color.rgb, 0.0, 1.0), 0.35);
    fragColor = vec4(clamp(col, 0.0, 1.0), u_Alpha);
}

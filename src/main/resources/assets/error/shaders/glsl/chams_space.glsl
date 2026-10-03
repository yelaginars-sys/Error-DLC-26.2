#version 150 core

in vec3 vWorldDir;

uniform vec4 u_Color;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Alpha;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(p), hash(p + vec3(1.0, 0.0, 0.0)), f.x),
            mix(hash(p + vec3(0.0, 1.0, 0.0)), hash(p + vec3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash(p + vec3(0.0, 0.0, 1.0)), hash(p + vec3(1.0, 0.0, 1.0)), f.x),
            mix(hash(p + vec3(0.0, 1.0, 1.0)), hash(p + vec3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float a = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        a += amp * noise(p);
        p *= 2.1;
        amp *= 0.5;
    }
    return a;
}

void main() {
    vec3 dir = normalize(vWorldDir) * max(0.35, u_Scale);
    float t = u_Time * 0.35;
    vec3 th = u_Color.rgb;
    vec3 col = mix(vec3(0.008, 0.01, 0.045), th * 0.22, 0.78);
    float zen = dir.y * 0.5 + 0.5;
    col += mix(vec3(0.02, 0.03, 0.08), th * 0.14, 0.72) * pow(1.0 - zen, 2.5);
    float n = fbm(dir * 3.5 + vec3(t * 0.15, -t * 0.1, t * 0.08));
    col += th * (0.35 + 0.65 * n) * (0.25 + 0.55 * zen);
    float stars = pow(max(0.0, noise(dir * 80.0 + t) - 0.82), 8.0);
    col += vec3(1.0) * stars * 1.4;
    fragColor = vec4(clamp(col, 0.0, 1.0), u_Alpha);
}

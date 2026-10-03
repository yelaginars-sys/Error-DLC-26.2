#version 150 core

in vec3 vWorldDir;

uniform vec4 u_Color;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Alpha;

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
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    vec3 dir = normalize(vWorldDir);
    vec2 uv = vec2(atan(dir.x, dir.z), asin(clamp(dir.y, -1.0, 1.0))) * u_Scale;
    float t = u_Time * 0.25;
    float n = noise(uv * 2.5 + vec2(t, -t * 0.7));
    float petals = smoothstep(0.35, 0.85, n) * smoothstep(1.0, 0.55, length(uv) * 0.35);
    vec3 base = mix(u_Color.rgb * 0.35, u_Color.rgb, 0.55 + 0.45 * dir.y);
    vec3 blossom = mix(u_Color.rgb, vec3(1.0, 0.75, 0.85), 0.45);
    vec3 col = base + blossom * petals * 0.85;
    col *= mix(vec3(1.0), clamp(u_Color.rgb, 0.0, 1.0), 0.1);
    fragColor = vec4(clamp(col, 0.0, 1.0), u_Alpha);
}

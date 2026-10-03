#version 150 core

in vec3 vWorldDir;

uniform vec4 u_Color;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Alpha;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(233.3, 157.7));
    return fract(sin(dot(p, vec2(27.13, 67.17))) * 99139.3);
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
    float t = u_Time * 0.2;
    float clouds = noise(uv * 1.4 + vec2(t, t * 0.3));
    clouds = smoothstep(0.4, 0.85, clouds);
    float sun = pow(max(0.0, dir.y), 4.0);
    vec3 sky = mix(u_Color.rgb * 0.45, u_Color.rgb, 0.4 + 0.6 * (dir.y * 0.5 + 0.5));
    sky += vec3(1.0, 0.9, 0.55) * sun * 0.55;
    sky = mix(sky, vec3(1.0), clouds * 0.35);
    sky *= mix(vec3(1.0), clamp(u_Color.rgb, 0.0, 1.0), 0.15);
    fragColor = vec4(clamp(sky, 0.0, 1.0), u_Alpha);
}

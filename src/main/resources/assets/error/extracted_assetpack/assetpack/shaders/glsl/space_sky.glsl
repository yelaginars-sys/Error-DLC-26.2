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

float starLayer(vec2 uv, float density, float size, float cutoff, float seed, float t) {
    vec2 cell = floor(uv * density);
    vec2 local = fract(uv * density) - 0.5;
    vec2 jitter = vec2(hash21(cell + seed), hash21(cell + seed + 17.3)) - 0.5;
    local -= jitter * 0.38;
    float h = hash21(cell + seed * 13.17);
    float d = length(local);
    float core = 1.0 - smoothstep(size * 0.18, size, d);
    float halo = (1.0 - smoothstep(size * 0.65, size * 2.6, d)) * 0.16;
    float twinkle = 0.62 + 0.38 * sin(t * (0.85 + h * 2.3) + h * 41.0 + seed);
    return (core + halo) * step(cutoff, h) * twinkle * (0.42 + h * 1.12);
}

void main() {
    vec2 uv = fragCoord;
    float theta = uv.x * 6.2831853;
    float y = uv.y * 2.0 - 1.0;
    vec2 p = vec2(cos(theta), sin(theta)) * (1.15 + y * 0.18);
    p.y += y * 1.25;
    p *= max(0.35, u_Scale);

    float t = u_Time * 0.22;
    vec2 parallax = (uv - 0.5) * 0.55;

    vec3 zenith = vec3(0.003, 0.005, 0.016);
    vec3 midSky = vec3(0.008, 0.012, 0.032);
    vec3 horizon = mix(u_Color2 * 0.18, vec3(0.018, 0.024, 0.055), 0.62);
    vec3 col = mix(horizon, midSky, smoothstep(-1.0, 0.18, y));
    col = mix(col, zenith, smoothstep(0.02, 1.0, y));

    vec2 dustFar = p * 1.05 + vec2(t * 0.032, -t * 0.018);
    vec2 dustNear = p * 1.72 + parallax * 0.45 + vec2(-t * 0.048, t * 0.026);
    float n1 = fbm(dustFar);
    float n2 = fbm(dustNear + n1 * 0.35);
    float lane = 1.0 - abs(y * 0.52 + (n1 - 0.5) * 0.38);
    lane = smoothstep(0.12, 0.82, lane);
    float dust = smoothstep(0.30, 0.78, n1 * 0.52 + n2 * 0.58) * lane;
    vec3 dustTint = mix(u_Color.rgb, u_Color2, n2);
    col += dustTint * dust * 0.20;
    col += mix(u_Color.rgb, vec3(0.50, 0.62, 1.0), 0.42) * pow(dust, 2.2) * 0.10;

    vec2 starFar = p * 2.15 + vec2(t * 0.012, 0.0);
    vec2 starMid = p * 3.35 + parallax * 0.70 + vec2(-t * 0.022, t * 0.008);
    vec2 starNear = p * 4.85 + parallax * 1.20 + vec2(t * 0.034, -t * 0.014);

    float fine = starLayer(starFar, 46.0, 0.026, 0.976, 2.0, t);
    float mid = starLayer(starMid, 26.0, 0.036, 0.960, 8.0, t);
    float giant = starLayer(starNear, 14.0, 0.050, 0.982, 21.0, t);

    col += vec3(0.72, 0.80, 1.0) * fine * 0.52;
    col += mix(vec3(1.0, 0.94, 0.80), u_Color.rgb, 0.12) * mid * 0.92;
    col += mix(vec3(0.86, 0.92, 1.0), u_Color.rgb, 0.22) * giant * 1.18;

    col *= mix(1.0, 0.74, clamp(u_Night, 0.0, 1.0));
    col = col / (col + vec3(0.86));
    fragColor = vec4(pow(clamp(col, 0.0, 1.0), vec3(0.92)), 1.0);
}

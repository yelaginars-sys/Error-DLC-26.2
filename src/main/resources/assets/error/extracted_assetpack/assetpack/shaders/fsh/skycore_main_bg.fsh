#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 size;
uniform float time;
uniform float alpha;
uniform float progress;
uniform float exitProgress;
uniform vec3 themeColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += noise(p) * a;
        p = p * 2.0 + vec2(17.0, 31.0);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord;
    vec2 p = (uv - 0.5) * vec2(size.x / max(size.y, 1.0), 1.0);
    float t = time * 0.35;

    float n1 = fbm(p * 2.3 + vec2(t * 0.8, -t * 0.4));
    float n2 = fbm(p * 4.1 + vec2(-t * 1.2, t * 0.7));

    float glowA = exp(-length(p - vec2(-0.30 + 0.16 * sin(time * 0.50), 0.02)) * 2.7);
    float glowB = exp(-length(p - vec2(0.28 + 0.14 * cos(time * 0.42), -0.18)) * 3.0);

    vec3 base = vec3(0.035, 0.040, 0.055);
    vec3 cTheme = themeColor;
    vec3 cThemeDim = themeColor * 0.6;

    vec3 haze = cTheme * glowA * 0.22;
    haze += cThemeDim * glowB * 0.20;
    haze += cTheme * (n1 * 0.09 + n2 * 0.06);

    float centerLift = exp(-pow((uv.y - 0.5) * 4.6, 2.0)) * 0.06;
    float breathe = 0.90 + 0.10 * sin(time * 0.75);

    vec3 color = base + haze * breathe + vec3(centerLift);
    color += cTheme * (0.02 + progress * 0.02);

    float vignette = smoothstep(1.20, 0.16, length((uv - 0.5) * vec2(1.35, 1.0)));
    color *= 0.75 + 0.25 * vignette;

    float grain = (hash21(uv * size + vec2(time * 73.0, time * 41.0)) - 0.5) * 0.02;
    color += grain;
    color *= 1.0 - exitProgress * 0.08;

    fragColor = vec4(color, alpha);
}

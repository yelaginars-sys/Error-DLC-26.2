#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;
uniform float time;
uniform vec4 accentColor;
uniform vec4 accentColor2;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amp;
        p = p * 2.02 + vec2(14.3, 9.1);
        amp *= 0.5;
    }
    return value;
}

mat2 rotate2d(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

float lineBand(float value, float width) {
    return 1.0 - smoothstep(0.0, width, abs(value));
}

float diagonalGrid(vec2 p, float scale, float width) {
    vec2 g = abs(fract(p * scale) - 0.5);
    float line = min(g.x, g.y);
    return 1.0 - smoothstep(0.0, width, line);
}

float particleField(vec2 uv, float t) {
    vec2 grid = uv * vec2(44.0, 25.0);
    vec2 id = floor(grid);
    vec2 cell = fract(grid) - 0.5;
    float rnd = hash(id);
    vec2 drift = vec2(sin(t * 0.9 + rnd * 6.283), cos(t * 0.7 + rnd * 9.41)) * 0.23;
    float sparkle = 1.0 - smoothstep(0.018, 0.055, length(cell - drift));
    float gate = step(0.86, rnd);
    return sparkle * gate * max(0.0, 0.45 + 0.55 * sin(t * 2.0 + rnd * 16.0));
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(resolution, vec2(1.0));
    vec2 p = uv - 0.5;
    p.x *= resolution.x / max(resolution.y, 1.0);

    float t = time * 0.22;
    vec3 accentA = max(accentColor.rgb, vec3(0.08));
    vec3 accentB = max(accentColor2.rgb, vec3(0.08));
    vec3 glowColor = mix(accentA, accentB, 0.42);

    float vignette = 1.0 - smoothstep(0.14, 1.25, length(p));
    float vertical = 1.0 - smoothstep(0.04, 1.05, uv.y);
    float atmosphere = fbm(p * 2.0 + vec2(t * 0.09, -t * 0.06));
    vec3 base = mix(vec3(0.006, 0.008, 0.013), glowColor * 0.22, vertical * 0.55 + atmosphere * 0.24);

    vec2 r1 = rotate2d(-0.58) * (p + vec2(sin(t * 0.17) * 0.05, cos(t * 0.13) * 0.04));
    vec2 r2 = rotate2d(0.72) * (p + vec2(cos(t * 0.11) * 0.04, sin(t * 0.16) * 0.05));
    float ribbonA = lineBand(sin(r1.x * 4.8 + r1.y * 1.25 + t * 0.48) * 0.5, 0.020);
    float ribbonB = lineBand(sin(r2.x * 5.8 - r2.y * 1.55 - t * 0.36) * 0.5, 0.015);
    ribbonA *= 1.0 - smoothstep(0.0, 0.72, abs(r1.y + 0.12));
    ribbonB *= 1.0 - smoothstep(0.0, 0.64, abs(r2.y - 0.08));
    ribbonA = pow(ribbonA, 1.7);
    ribbonB = pow(ribbonB, 1.85);

    float logoAura = exp(-dot((p - vec2(0.0, -0.12)) / vec2(0.62, 0.38), (p - vec2(0.0, -0.12)) / vec2(0.62, 0.38)) * 1.65);
    float lowerAura = exp(-dot((p - vec2(-0.48, -0.38)) / vec2(0.62, 0.42), (p - vec2(-0.48, -0.38)) / vec2(0.62, 0.42)) * 1.85);

    vec2 gridP = rotate2d(-0.36) * (p + vec2(t * 0.025, -t * 0.018));
    float grid = diagonalGrid(gridP, 8.0, 0.012);
    grid *= (1.0 - smoothstep(0.18, 0.95, length(p))) * 0.36;

    float scan = 1.0 - smoothstep(0.0, 0.012, abs(fract((uv.y + uv.x * 0.12) * 18.0 - t * 0.42) - 0.5));
    scan *= 0.04;

    float particles = particleField(uv + vec2(t * 0.012, -t * 0.006), t);

    vec3 color = base;
    color += glowColor * logoAura * 0.46;
    color += mix(accentA, accentB, 0.25) * lowerAura * 0.22;
    color += accentA * ribbonA * 0.105;
    color += accentB * ribbonB * 0.085;
    color += glowColor * grid * 0.13;
    color += vec3(1.0) * particles * 0.18;
    color += glowColor * scan;

    color *= 0.48 + vignette * 0.86;
    color += (hash(gl_FragCoord.xy + time * 17.0) - 0.5) / 180.0;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

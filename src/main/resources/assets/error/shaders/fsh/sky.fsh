#version 330
precision mediump float;

layout(std140) uniform EffectData {
    vec4 color;
    vec4 color2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixel;
};

out vec4 fragColor;

float g_alpha = 1.0;

#define uRes screen.xy
#define uTime screen.z

float Hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float tri(float x) {
    return abs(fract(x) * 2.0 - 1.0);
}

float tris(float x) {
    float t = fract(x);
    return 1.0 - smoothstep(0.0, 1.0, abs(2.0 * t - 1.0));
}

float trisn(float x) {
    float t = fract(x);
    return 2.0 * (1.0 - smoothstep(0.0, 1.0, abs(2.0 * t - 1.0))) - 1.0;
}

float Star(vec2 uv, float flare) {
    float d = length(uv);
    float m = .05 / d;
    float rays = smoothstep(0., 1., 1. - abs(uv.x * uv.y * 1000.));
    m += rays * flare;

    uv *= mat2(0.7071, -0.7071, 0.7071, 0.7071);
    rays = smoothstep(0., 1., 1. - abs(uv.x * uv.y * 1000.));
    m += rays * .3 * flare;
    m *= smoothstep(1., .2, d);
    return m;
}

float iStarSpeed() {
    return uTime / 10.0;
}

vec3 StarLayer(vec2 uv) {
    vec3 col = vec3(0);
    vec2 gv = fract(uv) - .5;
    vec2 id = floor(uv);

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 si = id + offset;
            float seed = Hash21(si);
            float size = fract(seed * 345.32);
            float glossLocal = tri(iStarSpeed() / (3.0 * seed + 1.));
            float flareSize = smoothstep(.9, 1., size) * glossLocal;

            float brightnessMod = fract(seed * 1.5) * 0.5 + 0.5;
            vec3 base = color.rgb * brightnessMod;

            vec2 pad = vec2(tris(seed * 34. + uTime / 10.), tris(seed * 38. + uTime / 30.)) - .5;

            float star = Star(gv - offset - pad, flareSize);

            star *= trisn(uTime * 1. + seed * 6.2831) * .5 + 1.;
            col += star * size * base;
        }
    }
    return col;
}

vec3 EffectStars(vec2 p) {
    vec3 col = vec3(0.);

    for (float i = 0.; i < 1.; i += 1. / 4.) {
        float depth = fract(i + iStarSpeed());
        float scale = mix(20., .5, depth);
        float fade = depth * smoothstep(1., .9, depth);
        col += StarLayer(p * scale + i * 453.32) * fade;
    }
    return col;
}

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

#define OCTAVES 5
float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(st);
        st *= 2.1;
        amplitude *= 0.45;
    }
    return value;
}

vec3 EffectMatrix(vec2 pIn) {
    vec2 uv = pIn;

    float t = uTime * 0.35;

    vec2 q = vec2(fbm(uv * 1.2 + vec2(t * 0.2, -t * 0.3)), fbm(uv * 1.2 + vec2(1.2)));
    vec2 r = vec2(fbm(uv + q * 1.5 + vec2(t * 0.3)), fbm(uv + q * 1.5 + vec2(-t * 0.2)));
    float n = fbm(uv * 0.8 + r * 1.2);

    float pulse = sin(t * 1.5) * 0.5 + 0.5;

    float lines = sin((n * 9.0) + (t * 1.5));
    float contour = smoothstep(0.2, 0.8, abs(lines));
    contour = 1.0 - contour;
    contour = pow(contour, 3.0) * (0.8 + pulse * 0.4);

    vec2 grid_uv = gl_FragCoord.xy * 0.18;
    vec2 fpos = fract(grid_uv) - 0.5;

    float sparkle = random(floor(grid_uv) + floor(uTime * 5.0));
    float dot_size = contour * 0.55 * (0.8 + sparkle * 0.2);
    float dist = length(fpos);
    float dot_mask = 1.0 - smoothstep(dot_size - 0.07, dot_size + 0.07, dist);

    float core_glow = contour * (1.5 + sin(t * 4.0 + n * 6.0) * 0.5);
    vec3 spark_color = color.rgb * core_glow;

    vec3 col = vec3(0.0);
    col += vec3(dot_mask) * spark_color;
    col += vec3(contour * 0.08);

    return col;
}

float cyclic2D(vec2 p) {
    mat2 m2 = mat2(0.97367, 0.22798, -0.22798, 0.97367) * 1.5;
    float result = 0.;
    float z = .95;
    float distort = 2.1;
    float dstAmount = 0.09;
    for (int i = 0; i < 9; i++) {
        p += sin(p.yx * distort + uTime * 0.27 * pow(distort, 1.35)) * dstAmount;
        result += abs(dot(cos(p), sin(p)) * z);
        z *= 0.65;
        distort *= 1.35;
        p = p * m2;
    }
    return result;
}

vec3 EffectNoise(vec2 pIn) {
    vec2 p = pIn * 2.0 + uTime * 0.015;
    vec3 col = vec3(sin(vec3(1.15, 1.65, 2.25) * cyclic2D(p * 8) + 4.2)) * 0.5 + 0.5;
    vec3 effect = .05 / (col * col + 0.05);
    float glow = (effect.r + effect.g + effect.b) / 3.0;
    return color.rgb * glow;
}

#define TAU 6.28318530718
#define MAX_ITER 5

vec3 EffectWaves(vec2 pIn) {
    vec2 uv = pIn * 0.5 + 0.5;
    float time = uTime * .5 + 23.0;
    vec2 p = mod(uv * TAU, TAU) - 250.0;
    vec2 i = vec2(p);
    float c = 1.0;
    float inten = .005;

    for (int n = 0; n < MAX_ITER; n++) {
        float t = time * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }

    c /= float(MAX_ITER);
    c = 1.17 - pow(c, 1.4);

    float mask = clamp(pow(abs(c), 8.0), 0.0, 1.0);
    g_alpha = clamp(mask * 2.0, 0.0, 1.0);
    return mix(color.rgb, vec3(1.0), mask * 0.5);
}

#define f length(fract(q *= m *= 0.6 + 0.1 * d++) - 0.5)

vec3 EffectGrid(vec2 pIn) {
    float d = 0.0;
    vec3 q = vec3(pIn * 4.0, uTime * 0.2);
    mat3 m = mat3(-2.0, -1.0, 2.0, 3.0, -2.0, 1.0, -1.0, 1.0, 3.0);
    vec3 col = vec3(pow(min(min(f, f), f), 7.0) * 40.0);
    return clamp(col, 0.0, 1.0) * color.rgb;
}

void main() {
    vec2 screenPos = gl_FragCoord.xy / uRes * 2.0 - 1.0;
    float tanHalfFov = tan(params0.z * 0.5);
    vec3 ray = normalize(vec3(
        screenPos.x * tanHalfFov * uRes.x / uRes.y,
        screenPos.y * tanHalfFov,
        1.0
    ));

    float cy = cos(params0.x);
    float sy = sin(params0.x);
    float cp = cos(params0.y);
    float sp = sin(params0.y);
    ray = mat3(cy, 0.0, sy, 0.0, 1.0, 0.0, -sy, 0.0, cy)
        * mat3(1.0, 0.0, 0.0, 0.0, cp, sp, 0.0, -sp, cp) * ray;

    ray = vec3(ray.x, -ray.z, ray.y);

    float k = max(params1.x, 0.1);
    vec2 p = ray.xy * (0.5 / k) / (ray.z + 1.0);

    vec3 outColor;
    int idx = int(params0.w + 0.5);
    if (idx == 1) {
        outColor = EffectMatrix(p);
    } else if (idx == 2) {
        outColor = EffectNoise(p);
    } else if (idx == 3) {
        outColor = EffectWaves(p);
    } else if (idx == 4) {
        outColor = EffectGrid(p);
    } else {
        outColor = EffectStars(p);
    }

    fragColor = vec4(outColor, g_alpha);
}
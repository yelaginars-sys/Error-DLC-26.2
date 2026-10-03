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

in vec4 vertexColor;
out vec4 fragColor;

#define uRes screen.xy
#define uTime screen.z

#define NUM_LAYER 4.
#define STAR_COLOR_CUTOFF 0.2
#define MAT45 mat2(0.7071, -0.7071, 0.7071, 0.7071)
#define PERIOD 3.

float iStarSpeed = uTime / 10.0;

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

    uv *= MAT45;
    rays = smoothstep(0., 1., 1. - abs(uv.x * uv.y * 1000.));
    m += rays * .3 * flare;
    m *= smoothstep(1., .2, d);
    return m;
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
            float glossLocal = tri(iStarSpeed / (PERIOD * seed + 1.));
            float flareSize = smoothstep(.9, 1., size) * glossLocal;

            float brightnessMod = fract(seed * 1.5) * 0.5 + 0.5;
            vec3 base = vertexColor.rgb * brightnessMod;

            vec2 pad = vec2(tris(seed * 34. + uTime / 10.), tris(seed * 38. + uTime/30.)) - .5;

            float star = Star(gv - offset - pad, flareSize);

            star *= trisn(uTime * 1. + seed * 6.2831) * .5 + 1.;
            col += star * size * base;
        }
    }
    return col;
}

void main() {
    vec2 uv = (gl_FragCoord.xy - 0.5 * uRes.xy) / uRes.y;
    vec3 col = vec3(0.);

    for (float i = 0.; i < 1.; i += 1. / NUM_LAYER) {
        float depth = fract(i + iStarSpeed);
        float scale = mix(20., .5, depth);
        float fade = depth * smoothstep(1., .9, depth);
        col += StarLayer(uv * scale + i * 453.32) * fade;
    }
    fragColor = vec4(col * vertexColor.a, vertexColor.a);
}

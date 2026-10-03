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

#define TAU 6.28318530718
#define MAX_ITER 5

void main() {
    float time = uTime * .5 + 23.0;
    vec2 uv = gl_FragCoord.xy / uRes.xy;
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
    float finalAlpha = (0.03 + mask * 0.97) * vertexColor.a;
    vec3 color = mix(vertexColor.rgb, vec3(1.0), mask * 0.5);

    fragColor = vec4(color, finalAlpha);
}

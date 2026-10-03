#version 330
precision mediump float;

uniform sampler2D Sampler0;

layout(std140) uniform EffectData {
    vec4 uColor;
    vec4 uColor2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixelData;
};

in vec2 TexCoord;
out vec4 OutColor;

#define uTime screen.z
#define uRes screen.xy
#define alpha screen.w
#define colorMode params1.z
vec3 color;

#define TAU 6.28318530718
#define MAX_ITER 5

void main() {
    vec4 s0 = texture(Sampler0, TexCoord);
    float mask = s0.a;
    if (mask < 0.01) discard;

    vec3 rawColor = mix(uColor.rgb, s0.rgb, step(0.5, colorMode));
    float luma = dot(rawColor, vec3(0.299, 0.587, 0.114));
    color = mix(rawColor, clamp(luma + (rawColor - luma) * 6.0, 0.0, 1.0), step(0.5, colorMode));

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

    float maskVal = clamp(pow(abs(c), 8.0), 0.0, 1.0);
    float finalAlpha = (0.03 + maskVal * 0.97) * mask * alpha;
    vec3 finalColor = mix(color, vec3(1.0), maskVal * 0.5);

    OutColor = vec4(finalColor, finalAlpha);
}

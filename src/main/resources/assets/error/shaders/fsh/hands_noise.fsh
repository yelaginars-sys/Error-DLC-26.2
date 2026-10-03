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

float cyclic2D(vec2 p) {
    mat2 m2 = mat2(0.97367, 0.22798, -0.22798, 0.97367) * 1.5;
    float result = 0.;
    float z = .95;
    float distort = 2.1;
    float dstAmount = 0.09;
    for(int i = 0; i < 9; i++) {
        p += sin(p.yx * distort + uTime * 0.27 * pow(distort, 1.35)) * dstAmount;
        result += abs(dot(cos(p), sin(p)) * z);
        z *= 0.65;
        distort *= 1.35;
        p = p * m2;
    }
    return result;
}

void main() {
    vec4 s0 = texture(Sampler0, TexCoord);
    float mask = s0.a;
    if (mask < 0.01) discard;

    vec3 rawColor = mix(uColor.rgb, s0.rgb, step(0.5, colorMode));
    float luma = dot(rawColor, vec3(0.299, 0.587, 0.114));
    color = mix(rawColor, clamp(luma + (rawColor - luma) * 6.0, 0.0, 1.0), step(0.5, colorMode));

    vec2 p = (gl_FragCoord.xy - 0.5 * uRes.xy) / uRes.y + uTime * 0.015;
    vec3 col = vec3(sin(vec3(1.15, 1.65, 2.25) * cyclic2D(p * 8) + 4.2)) * 0.5 + 0.5;
    vec3 effect = .05 / (col * col + 0.05);
    float glow = (effect.r + effect.g + effect.b) / 3.0;
    float a = (0.05 + glow * 0.95) * mask * alpha;
    OutColor = vec4(color * glow, a);
}

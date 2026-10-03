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

mat2 rot(float g) {
    return mat2(cos(g), sin(g), -sin(g), cos(g));
}

#define f length(fract(q *= m *= 0.6 + 0.1 * d++) - 0.5)

void main() {
    vec4 s0 = texture(Sampler0, TexCoord);
    float mask = s0.a;
    if (mask < 0.01) discard;

    vec3 rawColor = mix(uColor.rgb, s0.rgb, step(0.5, colorMode));
    float luma = dot(rawColor, vec3(0.299, 0.587, 0.114));
    color = mix(rawColor, clamp(luma + (rawColor - luma) * 6.0, 0.0, 1.0), step(0.5, colorMode));

    float d = 0.0;
    vec3 q = vec3(gl_FragCoord.xy / uRes.yy - 13.0, uTime * 0.2);
    mat3 m = mat3(-2.0, -1.0, 2.0, 3.0, -2.0, 1.0, -1.0, 1.0, 3.0);

    float b = clamp(pow(min(min(f, f), f), 7.0) * 40.0, 0.0, 1.0);

    OutColor = vec4(color * b, b * mask * alpha);
}

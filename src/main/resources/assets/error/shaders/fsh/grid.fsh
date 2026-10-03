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

#define f length(fract(q *= m *= 0.6 + 0.1 * d++) - 0.5)

void main() {
    float d = 0.0;
    vec3 q = vec3(gl_FragCoord.xy / uRes.yy - 13.0, uTime * 0.2);
    mat3 m = mat3(-2.0, -1.0, 2.0, 3.0, -2.0, 1.0, -1.0, 1.0, 3.0);

    float b = clamp(pow(min(min(f, f), f), 7.0) * 40.0, 0.0, 1.0);

    vec3 clientCol = vertexColor.rgb;

    fragColor = vec4(clientCol * b * vertexColor.a, vertexColor.a * b);
}
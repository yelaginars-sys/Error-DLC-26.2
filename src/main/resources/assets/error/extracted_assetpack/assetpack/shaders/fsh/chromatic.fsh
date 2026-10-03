#version 330

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

#define strength params0.x
#define falloff params0.y

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec2 mirror(vec2 uv) {
    uv = mod(abs(uv), 2.0);
    return 1.0 - abs(uv - 1.0);
}

void main() {
    vec2 center = vec2(0.5);
    vec2 dir = TexCoord - center;
    vec3 base = texture(Sampler0, TexCoord).rgb;

    float aspect = screen.x / max(screen.y, 1.0);
    vec2 scaled = vec2(dir.x * aspect, dir.y);
    float radius = clamp(length(scaled) / length(vec2(0.5 * aspect, 0.5)), 0.0, 1.0);

    float amount = pow(radius, max(falloff, 0.001)) * strength * 0.004;
    if (amount < 1e-5) {
        OutColor = vec4(base, 1.0);
        return;
    }

    vec2 offset = dir * 2.0 * amount;

    float r = texture(Sampler0, mirror(TexCoord + offset)).r;
    float b = texture(Sampler0, mirror(TexCoord - offset)).b;

    vec3 shift = vec3(r - base.r, 0.0, b - base.b);
    shift -= dot(shift, LUMA);

    OutColor = vec4(clamp(base + shift, 0.0, 1.0), 1.0);
}

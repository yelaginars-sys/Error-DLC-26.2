#version 150

#define TAPS 24

layout(std140) uniform Uniforms {
    vec4 uTexel;
    vec4 uParams;
};

uniform sampler2D Sampler0;

in vec2 vUV;

out vec4 fragColor;

void main() {
    float radius = uParams.x;
    if (radius < 0.5) {
        fragColor = texture(Sampler0, vUV);
        return;
    }

    vec2 dir = uTexel.zw * uTexel.xy;
    vec2 step = dir * (radius / float(TAPS));

    vec3 acc = vec3(0.0);
    float wsum = 0.0;

    for (int i = -TAPS; i <= TAPS; ++i) {
        float fi = float(i) / float(TAPS);
        float w = exp(-(fi * fi) * 2.47);
        vec2 uv = clamp(vUV + step * float(i), vec2(0.0001), vec2(0.9999));
        acc += texture(Sampler0, uv).rgb * w;
        wsum += w;
    }

    fragColor = vec4(acc / wsum, 1.0);
}

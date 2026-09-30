#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }

    float time = FillParams.x;

    float scanline = sin((uv.y + time * 0.1) * 220.0) * 0.5 + 0.5;
    scanline = pow(scanline, 2.0);

    float beam = sin((uv.y * 3.0 - time * 0.7)) * 0.5 + 0.5;
    beam = pow(beam, 6.0) * 1.2;

    float glitch = random(vec2(floor(uv.y * 35.0), floor(time * 12.0))) * 0.25;

    vec3 holoColor = FillColor.rgb * (0.6 + scanline * 0.5 + beam + glitch);
    holoColor += vec3(0.2, 0.2, 0.2) * scanline;

    finalColor = vec4(clamp(holoColor, 0.0, 1.0), clamp(mask * FillColor.a * (0.7 + scanline * 0.3), 0.0, 1.0));
}
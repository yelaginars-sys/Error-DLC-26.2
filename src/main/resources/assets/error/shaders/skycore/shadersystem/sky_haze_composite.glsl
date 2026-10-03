#version 150 core

uniform sampler2D DepthSampler;
uniform sampler2D HazeSampler;

uniform vec4 SkyParams;

in vec2 fragCoord;
out vec4 fragColor;

bool depthIsSky(float depth) {
    bool reverseZ = SkyParams.w > 0.5;
    return reverseZ ? (depth <= 0.000001) : (depth >= 0.99999);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float depth = texture(DepthSampler, uv).r;
    if (depthIsSky(depth)) {
        fragColor = vec4(texture(HazeSampler, uv).rgb, 1.0);
    } else {
        discard;
    }
}

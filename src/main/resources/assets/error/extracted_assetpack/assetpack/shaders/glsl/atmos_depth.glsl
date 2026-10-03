#version 150 core

in vec2 fragCoord;

uniform sampler2D depthTexture;
uniform float nearPlane;
uniform float farPlane;

out vec4 fragColor;

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

void main() {
    // Same UV convention as other skycore fullscreen shaders.
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float raw = texture(depthTexture, uv).r;
    float linear = linearizeDepth(raw);
    // Pack: R = linear depth (clamped), G = raw depth, B = sky mask
    float sky = step(0.9999, raw);
    fragColor = vec4(min(linear, farPlane), raw, sky, 1.0);
}

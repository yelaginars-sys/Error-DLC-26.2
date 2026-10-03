#version 150 core

in vec2 fragCoord;

uniform sampler2D depthTexture;
uniform mat4 InvViewProjection;
uniform float nearPlane;
uniform float farPlane;
uniform float reverseZ;

out vec4 fragColor;

bool isSkyDepth(float depth) {
    if (reverseZ > 0.5) {
        return depth <= 0.000001;
    }
    return depth >= 0.99999;
}

vec3 reconstruct(vec2 uv, float depth) {
    float clipZ = reverseZ > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 ndc = vec4(uv * 2.0 - 1.0, clipZ, 1.0);
    vec4 world = InvViewProjection * ndc;
    return world.xyz / max(world.w, 1e-6);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float raw = texture(depthTexture, uv).r;
    float sky = isSkyDepth(raw) ? 1.0 : 0.0;
    float linear;
    if (sky > 0.5) {
        linear = farPlane;
    } else {
        // Approximate eye distance from reconstructed world offset vs camera-at-origin ray length.
        // InvVP without camera translation gives direction*depth-ish; use NDC far vs sample.
        vec3 p = reconstruct(uv, raw);
        vec3 farP = reconstruct(uv, reverseZ > 0.5 ? 0.0 : 1.0);
        float maxLen = max(length(farP), 1.0);
        linear = clamp(length(p) / maxLen * farPlane, nearPlane, farPlane);
        // Prefer absolute length when matrices include camera (world-space reconstruct).
        linear = clamp(length(p), nearPlane, farPlane);
    }
    fragColor = vec4(linear, raw, sky, 1.0);
}

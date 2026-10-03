#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 Transform;
uniform mat4 InvViewMat;
uniform mat4 InvProjMat;
uniform vec3 CameraPos;
uniform vec3 Center;
uniform float Radius;
uniform float BandWidth;
uniform float GlowWidth;
uniform float Fade;
uniform float GlowBoost;
uniform float ReverseZ;
uniform vec4 RingColor;

in vec2 fragCoord;
out vec4 fragColor;

bool isSkyDepth(float depth) {
    if (ReverseZ > 0.5) {
        return depth <= 0.000001;
    }
    return depth >= 0.99999;
}

vec3 worldPosFromDepth(vec2 uv, float depth) {
    float z = ReverseZ > 0.5 ? depth : (depth * 2.0 - 1.0);
    vec4 clipPos = vec4(uv * 2.0 - 1.0, z, 1.0);
    vec4 viewPos = InvProjMat * clipPos;
    viewPos /= max(viewPos.w, 0.0001);
    vec4 worldPos = InvViewMat * viewPos;
    return CameraPos + worldPos.xyz;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float depth = texture(DepthSampler, uv).r;
    if (isSkyDepth(depth)) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 worldPos = worldPosFromDepth(uv, depth);
    float dist = distance(worldPos, Center);
    float delta = abs(dist - Radius);

    float coreSigma = max(BandWidth * 0.35, 0.012);
    float glowSigma = max(GlowWidth, BandWidth * 1.8);

    float core = exp(-(delta * delta) / (2.0 * coreSigma * coreSigma));
    float glow = exp(-(delta * delta) / (2.0 * glowSigma * glowSigma));

    float intensity = core * 1.35 + glow * (0.55 + GlowBoost * 0.75);
    intensity *= Fade;

    if (intensity <= 0.004) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 color = RingColor.rgb * intensity;
    float alpha = clamp(intensity * RingColor.a, 0.0, 1.0);
    fragColor = vec4(color, alpha);
}

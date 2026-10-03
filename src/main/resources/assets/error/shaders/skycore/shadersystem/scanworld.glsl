#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 Transform;
uniform mat4 InvViewMat;
uniform mat4 InvProjMat;
uniform vec3 CameraPos;
uniform vec3 Center;
uniform float Radius;
uniform float BandWidth;
uniform float Fade;
uniform float Time;
uniform float ReverseZ;
uniform vec4 OuterColor;
uniform vec4 MidColor;
uniform vec4 InnerColor;
uniform vec4 ScanlineColor;

in vec2 fragCoord;
out vec4 fragColor;

const float sharpness = 10.0;

float scanlines(float y, float time) {
    return sin(y + time * 10.0) * 0.5 + 0.5;
}

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
    if (dist < Radius && dist > Radius - BandWidth) {
        float diff = 1.0 - (Radius - dist) / max(BandWidth, 0.0001);
        vec4 edge = mix(MidColor, OuterColor, pow(diff, sharpness));
        vec4 color = mix(InnerColor, edge, diff);
        float lines = scanlines(gl_FragCoord.y, Time);
        color += lines * ScanlineColor;
        color *= diff;
        color *= Fade;
        fragColor = color;
    } else {
        fragColor = vec4(0.0);
    }
}

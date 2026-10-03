#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 InvProjection;
uniform mat4 InvView;
uniform vec4 Tint;
uniform vec4 Params;
uniform vec4 CameraTime;
uniform vec4 Wind;

in vec2 fragCoord;
out vec4 fragColor;

float pseudoRand2(vec2 p) {
    return fract(sin(dot(p, vec2(91.345, 47.291))) * 28421.7313);
}

float latticeNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 fracPart = fract(p);
    fracPart = fracPart * fracPart * (3.0 - 2.0 * fracPart);
    float corners[4];
    corners[0] = pseudoRand2(cell);
    corners[1] = pseudoRand2(cell + vec2(1.0, 0.0));
    corners[2] = pseudoRand2(cell + vec2(0.0, 1.0));
    corners[3] = pseudoRand2(cell + vec2(1.0, 1.0));
    float rowA = mix(corners[0], corners[1], fracPart.x);
    float rowB = mix(corners[2], corners[3], fracPart.x);
    return mix(rowA, rowB, fracPart.y);
}

float layeredNoise(vec2 p) {
    float accum = 0.0;
    float weight = 0.55;
    vec2 coord = p;
    for (int octave = 0; octave < 3; octave++) {
        accum += latticeNoise(coord) * weight;
        coord = coord * 2.13 + vec2(17.3, 9.1);
        weight *= 0.5;
    }
    return accum;
}

bool depthIsSky(float depth) {
    bool reverseZ = Params.w > 0.5;
    return reverseZ ? (depth <= 0.000001) : (depth >= 0.99999);
}

vec3 unprojectView(vec2 uv, float depth) {
    float clipZ = Params.w > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 view = InvProjection * vec4(uv * 2.0 - 1.0, clipZ, 1.0);
    return view.xyz / max(view.w, 1e-6);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    float rawDepth = texture(DepthSampler, uv).r;

    float farDepth = Params.w > 0.5 ? 0.0 : 0.999999;
    vec3 viewDirPoint = unprojectView(uv, farDepth);
    vec3 rayDirection = normalize((InvView * vec4(viewDirPoint, 0.0)).xyz);

    float maxDistance = max(Params.z, 8.0);
    float sceneDistance = depthIsSky(rawDepth)
        ? maxDistance
        : length(unprojectView(uv, rawDepth));
    float marchEnd = min(sceneDistance, maxDistance);

    float time = CameraTime.w;
    vec3 cam = CameraTime.xyz;

    float density = 0.0;
    const float sampleCount = 8.0;
    for (int i = 0; i < 8; i++) {
        float sampleFrac = (float(i) + 0.5) / sampleCount;
        float marchT = marchEnd * sampleFrac;
        vec3 samplePos = cam + rayDirection * marchT;
        vec2 noiseCoord = (samplePos.xz - Wind.xy) * 0.09
            + vec2(samplePos.y * 0.05, -samplePos.y * 0.12 - time * 0.55)
            + vec2(time * 0.08, time * 0.05);
        float noiseSample = layeredNoise(noiseCoord);
        float heightBias = smoothstep(cam.y - 4.0, cam.y + 28.0, samplePos.y);
        density += smoothstep(0.38, 0.88, noiseSample) * mix(0.65, 1.25, heightBias);
    }
    density /= sampleCount;

    float depthFactor = 1.0 - exp(-marchEnd * 0.045);
    float upLook = clamp(rayDirection.y * 0.5 + 0.5, 0.0, 1.0);
    float gustWave = 0.7 + 0.3 * sin(time * 0.9 + dot(rayDirection.xz, Wind.zw) * 2.0);
    float alpha = clamp(
        density * depthFactor * Params.x * Params.y * gustWave
            * mix(0.75, 1.2, upLook)
            + 0.04 * Params.x * Params.y * depthFactor,
        0.0,
        0.88
    );
    if (alpha <= 0.003) {
        discard;
    }

    vec3 mistColor = mix(Tint.rgb * 0.75, Tint.rgb, density);
    fragColor = vec4(mistColor, alpha);
}

#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;

uniform vec4 SkyParams;
uniform vec2 SkyTexel;

in vec2 fragCoord;
out vec4 fragColor;

float scramble1(float p) {
    p = fract(p * 0.2171);
    p *= p + 19.19;
    p *= p + p;
    return fract(p);
}

vec3 scramble3(vec3 p3) {
    p3 = fract(p3 * vec3(0.2171, 0.1917, 0.2531));
    p3 += dot(p3, p3.yxz + 19.19);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float latticeNoise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float c000 = scramble3(cell).x;
    float c100 = scramble3(cell + vec3(1.0, 0.0, 0.0)).x;
    float c010 = scramble3(cell + vec3(0.0, 1.0, 0.0)).x;
    float c110 = scramble3(cell + vec3(1.0, 1.0, 0.0)).x;
    float c001 = scramble3(cell + vec3(0.0, 0.0, 1.0)).x;
    float c101 = scramble3(cell + vec3(1.0, 0.0, 1.0)).x;
    float c011 = scramble3(cell + vec3(0.0, 1.0, 1.0)).x;
    float c111 = scramble3(cell + vec3(1.0, 1.0, 1.0)).x;
    float bx0 = mix(c000, c100, f.x);
    float bx1 = mix(c010, c110, f.x);
    float bx2 = mix(c001, c101, f.x);
    float bx3 = mix(c011, c111, f.x);
    return mix(mix(bx0, bx1, f.y), mix(bx2, bx3, f.y), f.z);
}

float fractalNoise(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.52;
    float frequency = 1.0;
    for (int octave = 0; octave < 5; octave++) {
        sum += amplitude * latticeNoise3(p * frequency);
        frequency *= 2.11;
        amplitude *= 0.47;
    }
    return sum;
}

bool depthIsSky(float depth) {
    return SkyParams.w > 0.5 ? (depth <= 0.000001) : (depth >= 0.99999);
}

bool hasSkyNeighbor(vec2 coords) {
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            vec2 offset = vec2(float(dx), float(dy)) * SkyTexel * 2.0;
            if (depthIsSky(texture(DepthSampler, coords + offset).r)) {
                return true;
            }
        }
    }
    return false;
}

vec3 viewRayDirection(vec2 coords) {
    vec2 ndc = coords * 2.0 - 1.0;
    float farZ = SkyParams.w > 0.5 ? 0.0 : -1.0;
    vec4 nearClip = InvViewProjection * vec4(ndc, 1.0, 1.0);
    vec4 farClip = InvViewProjection * vec4(ndc, farZ, 1.0);
    return normalize(farClip.xyz / farClip.w - nearClip.xyz / nearClip.w);
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    if (!hasSkyNeighbor(uv)) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 direction = viewRayDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    // Soft volumetric haze — no hard great-circle stripe (old bandNormal ring).
    float haze = fractalNoise(direction * 2.7 + vec3(time * 0.012, time * 0.004, 0.0));
    float haze2 = fractalNoise(direction * 5.1 - vec3(time * 0.008, 0.0, time * 0.006));
    float veil = smoothstep(0.25, 0.85, haze * 0.65 + haze2 * 0.45);
    vec3 themeBlend = mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5);
    vec3 cloud = vec3(0.005, 0.007, 0.014) + themeBlend * veil * (0.12 + 0.28 * haze);
    fragColor = vec4(cloud, veil * 0.85 + 0.15);
}

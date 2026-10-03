#version 150 core

// Ported 1:1 from SkycoreRecode world/sky/clouds_deep_space.fsh

uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;
// x = time, y = intensity, z = speed, w = depth is 0..1 (reverse-Z)
uniform vec4 SkyParams;
uniform vec2 SkyTexel;

in vec2 fragCoord;
out vec4 fragColor;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash33(i).x;
    float n100 = hash33(i + vec3(1.0, 0.0, 0.0)).x;
    float n010 = hash33(i + vec3(0.0, 1.0, 0.0)).x;
    float n110 = hash33(i + vec3(1.0, 1.0, 0.0)).x;
    float n001 = hash33(i + vec3(0.0, 0.0, 1.0)).x;
    float n101 = hash33(i + vec3(1.0, 0.0, 1.0)).x;
    float n011 = hash33(i + vec3(0.0, 1.0, 1.0)).x;
    float n111 = hash33(i + vec3(1.0)).x;
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 6; i++) {
        sum += amplitude * valueNoise(p * frequency);
        frequency *= 2.02;
        amplitude *= 0.5;
    }
    return sum;
}

bool isSkyDepth(float depth) {
    if (SkyParams.w > 0.5) {
        return depth <= 0.000001;
    }
    return depth >= 0.99999;
}

bool skyNearby(vec2 coords) {
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float depth = texture(DepthSampler, coords + vec2(float(x), float(y)) * SkyTexel * 2.0).r;
            if (isSkyDepth(depth)) {
                return true;
            }
        }
    }
    return false;
}

vec3 skyDirection(vec2 coords) {
    vec2 ndc = coords * 2.0 - 1.0;
    float farZ = SkyParams.w > 0.5 ? 0.0 : -1.0;
    vec4 nearPoint = InvViewProjection * vec4(ndc, 1.0, 1.0);
    vec4 farPoint = InvViewProjection * vec4(ndc, farZ, 1.0);
    return normalize(farPoint.xyz / farPoint.w - nearPoint.xyz / nearPoint.w);
}

void main() {
    // FBO attachments are GL bottom-origin; fragCoord.y=0 is screen top.
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    if (!skyNearby(uv)) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec3 bandNormal = normalize(vec3(0.30, 0.62, 0.72));
    float band = exp(-pow(dot(direction, bandNormal) * 2.2, 2.0));
    float haze = fbm(direction * 3.0 + vec3(time * 0.01, 0.0, 0.0));
    vec3 cloud = vec3(0.006, 0.008, 0.016)
            + mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5) * band * (0.10 + 0.28 * haze);
    fragColor = vec4(cloud, band);
}

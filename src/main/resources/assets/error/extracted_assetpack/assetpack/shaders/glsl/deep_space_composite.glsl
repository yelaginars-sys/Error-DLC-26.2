#version 150 core

// Ported 1:1 from SkycoreRecode world/sky/deep_space.fsh

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;
// x = time, y = intensity, z = speed, w = depth is 0..1 (reverse-Z)
uniform vec4 SkyParams;

in vec2 fragCoord;
out vec4 fragColor;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float starGlow(vec3 direction, float scale, float density, float time) {
    vec3 p = direction * scale;
    vec3 cell = floor(p);
    float result = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 id = cell + vec3(x, y, z);
                if (length(p - id - 0.5) > 1.35) {
                    continue;
                }
                vec3 h = hash33(id);
                if (h.x > density) {
                    continue;
                }
                vec3 center = id + 0.2 + 0.6 * h;
                float distanceToStar = length(p - center);
                float brightness = pow(hash11(h.y + 1.7), 4.0);
                float twinkle = 0.6 + 0.4 * sin(time * 2.0 + h.z * 50.0);
                float core = smoothstep(0.06, 0.0, distanceToStar);
                float halo = exp(-distanceToStar * 12.0) * 0.5;
                result += (core + halo) * (0.4 + brightness * 2.6) * twinkle;
            }
        }
    }
    return result;
}

vec3 ditherRgb(vec2 fc) {
    return vec3((hash21(fc) - 0.5) / 255.0);
}

bool isSkyDepth(float depth) {
    if (SkyParams.w > 0.5) {
        return depth <= 0.000001;
    }
    return depth >= 0.99999;
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
    float depth = texture(DepthSampler, uv).r;
    if (!isSkyDepth(depth)) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 cloud = texture(CloudSampler, uv);
    float stars = starGlow(direction, 300.0, 0.06, time)
            + starGlow(direction * 1.8 + 5.0, 160.0, 0.03, time * 1.3) * 1.3;
    vec3 color = cloud.rgb + vec3(0.90, 0.93, 1.0) * stars * (0.7 + 0.9 * cloud.a);
    color *= SkyParams.y;
    color = color / (1.0 + color * 0.8);
    color = pow(color, vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

#version 150 core

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;

uniform vec4 SkyParams;

in vec2 fragCoord;
out vec4 fragColor;

float scramble1(float p) {
    p = fract(p * 0.2171);
    p *= p + 19.19;
    p *= p + p;
    return fract(p);
}

float scramble2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.2171, 0.1917, 0.2531));
    q += dot(q, q.yzx + 19.19);
    return fract((q.x + q.y) * q.z);
}

vec3 scramble3(vec3 p) {
    p = fract(p * vec3(0.2171, 0.1917, 0.2531));
    p += dot(p, p.yxz + 19.19);
    return fract((p.xxy + p.yxx) * p.zyx);
}

float starCluster(vec3 direction, float scale, float density, float time) {
    vec3 gridPos = direction * scale;
    vec3 cell = floor(gridPos);
    float brightness = 0.0;

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 cellId = cell + vec3(float(x), float(y), float(z));
                if (length(gridPos - cellId - 0.5) > 1.4) {
                    continue;
                }
                vec3 h = scramble3(cellId);
                if (h.x > density) {
                    continue;
                }
                vec3 starCenter = cellId + 0.15 + 0.7 * h;
                float dist = length(gridPos - starCenter);
                float lum = pow(scramble1(h.y + 2.3), 3.5);
                float twinkle = 0.55 + 0.45 * sin(time * 1.7 + h.z * 63.0);
                float core = smoothstep(0.055, 0.0, dist);
                float halo = exp(-dist * 10.5) * 0.45;
                brightness += (core + halo) * (0.35 + lum * 2.4) * twinkle;
            }
        }
    }
    return brightness;
}

vec3 pixelDither(vec2 fc) {
    return vec3((scramble2(fc) - 0.5) / 255.0);
}

bool depthIsSky(float depth) {
    return SkyParams.w > 0.5 ? (depth <= 0.000001) : (depth >= 0.99999);
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
    float depth = texture(DepthSampler, uv).r;
    if (!depthIsSky(depth)) {
        discard;
    }

    vec3 direction = viewRayDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 cloud = texture(CloudSampler, uv);

    float stars = starCluster(direction, 260.0, 0.055, time)
        + starCluster(direction * 1.65 + 4.2, 140.0, 0.028, time * 1.2) * 1.25;

    vec3 color = cloud.rgb + vec3(0.88, 0.92, 1.0) * stars * (0.65 + 0.95 * cloud.a);
    color *= SkyParams.y;
    color = color / (1.0 + color * 0.85);
    color = pow(color, vec3(0.9)) + pixelDither(gl_FragCoord.xy);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

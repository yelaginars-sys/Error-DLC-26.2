#version 150 core

uniform sampler2D DepthSampler;
uniform mat4 InvViewProjection;
uniform vec4 PrimaryColor;
uniform vec4 SecondaryColor;
uniform vec4 SkyParams;
uniform vec2 SkyTexel;

in vec2 fragCoord;
out vec4 fragColor;

// Custom hash / ridged noise for SkyCore pulsar sky.
float scramble(vec3 p) {
    vec3 q = fract(p * vec3(0.2173, 0.1917, 0.2531));
    q += dot(q, q.yzx + 19.19);
    return fract((q.x + q.y) * q.z * 97.0);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    // Quintic fade — different from classic smoothstep hermite.
    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float n000 = scramble(i);
    float n100 = scramble(i + vec3(1.0, 0.0, 0.0));
    float n010 = scramble(i + vec3(0.0, 1.0, 0.0));
    float n110 = scramble(i + vec3(1.0, 1.0, 0.0));
    float n001 = scramble(i + vec3(0.0, 0.0, 1.0));
    float n101 = scramble(i + vec3(1.0, 0.0, 1.0));
    float n011 = scramble(i + vec3(0.0, 1.0, 1.0));
    float n111 = scramble(i + vec3(1.0, 1.0, 1.0));
    float x0 = mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y);
    float x1 = mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y);
    return mix(x0, x1, f.z);
}

float ridgedFbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.55;
    float freq = 1.0;
    for (int o = 0; o < 4; o++) {
        float n = 1.0 - abs(valueNoise(p * freq) * 2.0 - 1.0);
        n = n * n;
        sum += n * amp;
        freq *= 2.17;
        amp *= 0.48;
        p += vec3(1.7, 3.1, 2.3);
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
    return normalize(farClip.xyz / max(farClip.w, 1e-6) - nearClip.xyz / max(nearClip.w, 1e-6));
}

vec3 scatterStars(vec3 rd, float t) {
    const float GRID = 97.0;
    vec3 cell = floor(rd * GRID);
    float h = scramble(cell + 11.3);
    if (h < 0.985) {
        return vec3(0.0);
    }
    vec3 jitter = vec3(
        scramble(cell + 4.1),
        scramble(cell + 8.7),
        scramble(cell + 13.9)
    ) - 0.5;
    vec3 pos = normalize((cell + 0.5 + jitter * 0.7) / GRID);
    float d = acos(clamp(dot(rd, pos), -1.0, 1.0));
    float twinkle = 0.5 + 0.5 * sin(t * 2.7 + h * 91.0);
    float bright = exp(-d * d * 180000.0) * twinkle * (0.25 + 0.75 * scramble(cell + 21.0));
    vec3 tint = vec3(0.92, 0.95, 1.0);
    float ch = scramble(cell + 29.5);
    if (ch < 0.18) tint = vec3(0.55, 0.72, 1.0);
    else if (ch < 0.32) tint = vec3(1.0, 0.78, 0.55);
    else if (ch < 0.38) tint = vec3(1.0, 0.48, 0.52);
    return tint * bright;
}

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    if (!hasSkyNeighbor(uv)) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 rd = viewRayDirection(uv);
    rd.y = -rd.y;
    float t = SkyParams.x;
    float sc = max(0.35, SkyParams.y);
    float intensity = max(0.1, SkyParams.z);
    vec3 theme = PrimaryColor.rgb;
    vec3 themeDark = SecondaryColor.rgb;

    // Two warped noise layers with different scales than original n1/n2 recipe.
    float warp = valueNoise(rd * sc * 0.55 + vec3(t * 0.004, t * 0.001, -t * 0.002));
    float layerA = ridgedFbm(rd * sc * 0.52 + vec3(warp * 0.8) + vec3(t * 0.0025, 0.0, 0.0));
    float layerB = ridgedFbm(rd * sc * 1.15 - vec3(0.0, t * 0.0018, warp));

    vec3 nebula = mix(themeDark * 0.06, theme * 0.26, layerA);
    nebula = mix(nebula, mix(theme, themeDark, 0.55) * 0.16, layerB * layerA);
    float dust = smoothstep(0.28, 0.78, layerA * 0.5 + layerB * 0.5);
    vec3 col = mix(nebula, themeDark * 0.03, dust * 0.8);
    col += scatterStars(rd, t);

    vec3 axis = normalize(vec3(0.42, 0.78, -0.31));
    float facing = smoothstep(0.0, 0.45, dot(rd, axis));
    if (facing > 0.0) {
        vec3 tx = normalize(cross(axis, vec3(0.12, 1.0, 0.08)));
        vec3 ty = cross(tx, axis);
        vec2 disk = vec2(dot(rd, tx), dot(rd, ty));
        float rad = length(disk);

        float beat = 0.92 + 0.08 * sin(t * 6.5);
        float core = smoothstep(0.05 * beat, 0.0, rad);
        float flare = ridgedFbm(rd * 9.5 + vec3(t * 0.18, 0.0, t * 0.05));
        float halo = exp(-rad * 14.0) * 2.8 + exp(-rad * 2.8) * 0.85 * (0.65 + 0.35 * flare);

        float spin = t * 4.8;
        vec2 jetAxis = vec2(sin(spin * 0.15), cos(spin * 0.15));
        float along = dot(disk, jetAxis);
        float ripple = sin(abs(along) * 62.0 - t * 28.0) * 0.0045 * abs(along);
        float jetGap = length(disk - along * jetAxis) - ripple;
        float jet = (exp(-jetGap * 280.0) * 2.6 + exp(-jetGap * 32.0) * 0.7)
            * exp(-abs(along) * 0.9);
        float flicker = 0.78 + 0.22 * sin(t * 97.0) * cos(t * 131.0);

        vec3 haloCol = mix(vec3(0.22, 0.5, 1.0), theme, 0.6);
        vec3 jetCol = mix(vec3(0.4, 0.72, 1.0), theme, 0.5);
        col += (vec3(1.0) * core + haloCol * halo + jetCol * jet * flicker) * facing;
    }

    col *= (0.5 + intensity * 0.6);
    fragColor = vec4(clamp(col, 0.0, 4.0), 1.0);
}

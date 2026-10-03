#version 120

uniform sampler2D originalTexture;
uniform sampler2D blurredTexture;
uniform sampler2D sceneTexture;
uniform sampler2D entityIdTexture;
uniform vec4 multiplier;
uniform vec2 viewOffset;
uniform vec2 resolution;
uniform float reflect;
uniform float noiseValue;
uniform float clarity;
uniform float stability;
uniform float depth;
uniform float time;
uniform vec3 cameraOffset;
uniform float cells;
uniform float facetStrength;
uniform float minTargetDistance;

#define TAU 6.28318530718
#define CLOSE_FADE_DIST 1.5
#define CLOSE_FADE_MIN 0.7

const vec2 C = vec2(1.0/6.0, 1.0/3.0);
const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
const float n_ = 1.0 / 7.0;

vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;
    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(
        i.z + vec4(0.0, i1.z, i2.z, 1.0)) +
        i.y + vec4(0.0, i1.y, i2.y, 1.0)) +
        i.x + vec4(0.0, i1.x, i2.x, 1.0));
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float decodeEntityId(vec4 idTex) {
    return idTex.r * 255.0 + idTex.g * 255.0 * 256.0 + idTex.b * 255.0 * 65536.0;
}

void main() {
    vec4 srcColor = texture2D(originalTexture, gl_TexCoord[0].xy);
    if (srcColor.a < 0.01) discard;

    vec2 uv = gl_TexCoord[0].xy;
    float entityId = decodeEntityId(texture2D(entityIdTexture, uv));
    float volumeFactor = entityId > 0.0 ? 1.0 : 0.0;

    vec2 pos = gl_FragCoord.xy + viewOffset;
    vec2 reflectedUV = vec2(pos.x / resolution.x, 1.0 - (pos.y / resolution.y));

    float verticalCoord = pos.y / resolution.y * reflect + cameraOffset.y * 0.02;
    float noise = snoise(vec3(verticalCoord, time * 0.001, 1.0));
    vec2 distortedUV = reflectedUV + vec2(noise * noiseValue, noise * noiseValue * 0.5);
    vec2 sampleUV = mix(distortedUV, reflectedUV, stability);

    float scale = max(cells, 1.0) * 0.08;
    vec3 seed = vec3(reflectedUV.x * scale, reflectedUV.y * scale, time * 0.15);
    vec2 facetOffset = vec2(snoise(seed), snoise(seed + vec3(47.0, 113.0, 17.0))) * facetStrength * 0.055;
    vec2 facetUV = sampleUV + facetOffset;

    vec4 blurredColor = texture2D(blurredTexture, facetUV);
    vec4 sceneColor = texture2D(sceneTexture, facetUV);
    float volClarity = mix(clarity, 1.0, volumeFactor * 0.4);
    float volDepth = mix(depth, 0.0, volumeFactor * 0.5);
    vec4 glassColor = mix(blurredColor, sceneColor, volClarity);
    vec4 finalColor = mix(glassColor, sceneColor, volDepth);

    vec3 tinted = finalColor.rgb * multiplier.rgb;
    vec3 resultRgb = mix(finalColor.rgb, tinted, multiplier.a);
    float closeFade = clamp(minTargetDistance / CLOSE_FADE_DIST, CLOSE_FADE_MIN, 1.0);
    gl_FragColor = vec4(resultRgb, srcColor.a * closeFade);
}

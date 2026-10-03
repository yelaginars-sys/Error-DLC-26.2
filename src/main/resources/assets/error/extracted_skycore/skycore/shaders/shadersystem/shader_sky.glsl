#version 120

uniform float time;
uniform float partialTicks;
uniform vec2 yawPitch;
uniform float intensity;
uniform float auroraScale;
uniform float stars;
uniform float glow;
uniform float horizon;
uniform vec3 primaryColor;
uniform vec3 accentColor;
uniform int mode;

const float PI = 3.14159265359;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += noise(p) * a;
        p = p * 2.03 + vec2(17.2, 9.4);
        a *= 0.5;
    }
    return v;
}

float lineGlow(float v, float width) {
    return smoothstep(width, 0.0, abs(v));
}

float rayGlow(vec2 p, vec2 origin, vec2 direction, float width) {
    vec2 d = normalize(direction);
    vec2 v = p - origin;
    float along = dot(v, d);
    float side = abs(v.x * d.y - v.y * d.x);
    return smoothstep(width, 0.0, side) * smoothstep(-0.18, 0.45, along);
}

float softCircle(vec2 p, vec2 center, float radius, float width) {
    return smoothstep(width, 0.0, abs(length(p - center) - radius));
}

float starField(vec2 uv, float t) {
    vec2 cell = floor(uv * 190.0);
    vec2 local = fract(uv * 190.0) - 0.5;
    float h = hash(cell);
    float star = smoothstep(0.034, 0.0, length(local)) * step(0.986, h);
    return star * (0.7 + 0.3 * sin(t * 2.0 + h * 40.0));
}

vec3 rotateY(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
}

vec2 sphereUv(vec3 d) {
    return vec2(atan(d.z, d.x) / (PI * 2.0) + 0.5, asin(clamp(d.y, -1.0, 1.0)) / PI + 0.5);
}

vec2 realSkyUv(vec3 d) {
    vec2 xzA = vec2(0.84, 0.54);
    vec2 xzB = vec2(-0.54, 0.84);
    return vec2(dot(d.xz, xzA), d.y * 0.72 + dot(d.xz, xzB) * 0.28) * 0.5 + 0.5;
}

float realStarLayer(vec2 uv, float density, float size, float cutoff, float seed, float t) {
    vec2 cell = floor(uv * density);
    vec2 local = fract(uv * density) - 0.5;
    vec2 jitter = vec2(hash(cell + seed + 4.7), hash(cell + seed + 9.2)) - 0.5;
    float h = hash(cell + seed * 13.17);
    float d = length(local - jitter * 0.42);
    float core = 1.0 - smoothstep(size * 0.22, size, d);
    float halo = (1.0 - smoothstep(size * 0.70, size * 2.5, d)) * 0.13;
    float tinyBloom = (1.0 - smoothstep(size * 1.4, size * 4.0, d)) * 0.022;
    float twinkle = 0.68 + 0.32 * sin(t * (0.9 + h * 2.2) + h * 37.0 + seed);
    return (core + halo + tinyBloom) * step(cutoff, h) * twinkle * (0.45 + h * 1.05);
}

vec3 starProjectionWeights(vec3 d) {
    vec3 w = abs(d);
    w = w * w;
    w = w * w * w;
    return w / max(w.x + w.y + w.z, 0.0001);
}

float realStarLayer3D(vec3 d, float density, float size, float cutoff, float seed, float t) {
    vec3 w = starProjectionWeights(d);
    vec2 offX = mix(vec2(seed * 0.47 + 9.0, seed * 0.23 + 4.0), vec2(seed * 0.17, seed * 0.31), step(0.0, d.x));
    vec2 offY = mix(vec2(seed * 0.37 + 2.0, seed * 0.41 + 7.0), vec2(seed * 0.29 + 5.0, seed * 0.19), step(0.0, d.y));
    vec2 offZ = mix(vec2(seed * 0.13 + 6.0, seed * 0.53 + 1.0), vec2(seed * 0.43, seed * 0.11 + 8.0), step(0.0, d.z));
    float sx = realStarLayer(d.yz * 0.5 + 0.5 + offX, density, size, cutoff, seed + 1.0, t);
    float sy = realStarLayer(d.xz * 0.5 + 0.5 + offY, density, size, cutoff, seed + 2.0, t);
    float sz = realStarLayer(d.xy * 0.5 + 0.5 + offZ, density, size, cutoff, seed + 3.0, t);
    return sx * w.x + sy * w.y + sz * w.z;
}

float realSparkle(vec2 uv, float density, float cutoff, float seed, float t) {
    vec2 cell = floor(uv * density);
    vec2 local = fract(uv * density) - 0.5;
    vec2 jitter = vec2(hash(cell + seed + 11.0), hash(cell + seed + 19.0)) - 0.5;
    local -= jitter * 0.18;
    float h = hash(cell + seed * 5.31);
    float angle = h * PI * 2.0;
    float c = cos(angle);
    float s = sin(angle);
    vec2 r = vec2(local.x * c - local.y * s, local.x * s + local.y * c);
    float pulse = 0.42 + 0.58 * pow(0.5 + 0.5 * sin(t * 1.8 + h * 29.0), 4.0);
    float core = 1.0 - smoothstep(0.010, 0.030, length(local));
    float longRay = (1.0 - smoothstep(0.003, 0.012, abs(r.y))) * (1.0 - smoothstep(0.028, 0.105, abs(r.x)));
    float shortRay = (1.0 - smoothstep(0.003, 0.011, abs(r.x))) * (1.0 - smoothstep(0.024, 0.075, abs(r.y)));
    return (core * 0.78 + longRay * 0.075 + shortRay * 0.045) * step(cutoff, h) * pulse;
}

float realCrater(vec2 p, vec2 center, float radius) {
    float d = length(p - center);
    float bowl = 1.0 - smoothstep(radius * 0.18, radius, d);
    float rim = smoothstep(radius * 0.60, radius, d) * (1.0 - smoothstep(radius, radius * 1.34, d));
    return bowl * 0.75 + rim * 0.28;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    return length(pa - ba * h);
}

float constellationLine(vec2 p, vec2 a, vec2 b, float width) {
    float d = segmentDistance(p, a, b);
    float body = 1.0 - smoothstep(width, width * 2.7, d);
    float fadeA = smoothstep(0.0, 0.020, length(p - a));
    float fadeB = smoothstep(0.0, 0.020, length(p - b));
    return body * fadeA * fadeB;
}

float constellationStar(vec2 p, vec2 center, float radius) {
    vec2 d = p - center;
    float core = 1.0 - smoothstep(radius * 0.18, radius, length(d));
    float halo = (1.0 - smoothstep(radius, radius * 3.6, length(d))) * 0.32;
    float rayA = (1.0 - smoothstep(radius * 0.035, radius * 0.18, abs(d.y))) * (1.0 - smoothstep(radius * 0.75, radius * 3.6, abs(d.x)));
    float rayB = (1.0 - smoothstep(radius * 0.035, radius * 0.18, abs(d.x))) * (1.0 - smoothstep(radius * 0.75, radius * 2.6, abs(d.y)));
    return core + halo + (rayA + rayB) * 0.11;
}

vec2 constellationCoords(vec3 dir, vec3 center, vec3 up, float scale, out float visible) {
    vec3 right = normalize(cross(up, center));
    up = normalize(cross(center, right));
    visible = smoothstep(0.80, 0.965, dot(dir, center));
    return vec2(dot(dir, right), dot(dir, up)) / scale;
}

float constellationHunter(vec3 dir, vec3 center, vec3 up, float scale, float t) {
    float visible;
    vec2 p = constellationCoords(dir, center, up, scale, visible);
    float pulse = 0.80 + 0.20 * sin(t * 0.26 + 0.7);

    vec2 shoulderL = vec2(-0.34, 0.34);
    vec2 shoulderR = vec2(0.28, 0.30);
    vec2 hipL = vec2(-0.24, -0.32);
    vec2 hipR = vec2(0.30, -0.28);
    vec2 beltA = vec2(-0.12, 0.05);
    vec2 beltB = vec2(0.02, 0.01);
    vec2 beltC = vec2(0.16, -0.04);
    vec2 swordA = vec2(0.03, -0.14);
    vec2 swordB = vec2(0.01, -0.30);

    float lines = 0.0;
    lines += constellationLine(p, shoulderL, shoulderR, 0.0065);
    lines += constellationLine(p, shoulderL, hipL, 0.0060);
    lines += constellationLine(p, shoulderR, hipR, 0.0060);
    lines += constellationLine(p, hipL, hipR, 0.0058);
    lines += constellationLine(p, beltA, beltB, 0.0052);
    lines += constellationLine(p, beltB, beltC, 0.0052);
    lines += constellationLine(p, beltB, swordA, 0.0046);
    lines += constellationLine(p, swordA, swordB, 0.0042);

    float starsInSign = 0.0;
    starsInSign += constellationStar(p, shoulderL, 0.036);
    starsInSign += constellationStar(p, shoulderR, 0.029);
    starsInSign += constellationStar(p, hipL, 0.026);
    starsInSign += constellationStar(p, hipR, 0.033);
    starsInSign += constellationStar(p, beltA, 0.019);
    starsInSign += constellationStar(p, beltB, 0.021);
    starsInSign += constellationStar(p, beltC, 0.019);
    starsInSign += constellationStar(p, swordA, 0.013);
    starsInSign += constellationStar(p, swordB, 0.012);

    return (lines * 0.15 + starsInSign * 0.62) * visible * pulse;
}

float constellationCrown(vec3 dir, vec3 center, vec3 up, float scale, float t) {
    float visible;
    vec2 p = constellationCoords(dir, center, up, scale, visible);
    float pulse = 0.78 + 0.22 * sin(t * 0.22 + 2.1);

    vec2 a = vec2(-0.48, -0.10);
    vec2 b = vec2(-0.30, 0.13);
    vec2 c = vec2(-0.08, 0.25);
    vec2 d = vec2(0.16, 0.20);
    vec2 e = vec2(0.36, 0.02);
    vec2 f = vec2(0.46, -0.24);

    float lines = 0.0;
    lines += constellationLine(p, a, b, 0.0052);
    lines += constellationLine(p, b, c, 0.0056);
    lines += constellationLine(p, c, d, 0.0062);
    lines += constellationLine(p, d, e, 0.0056);
    lines += constellationLine(p, e, f, 0.0048);

    float starsInSign = 0.0;
    starsInSign += constellationStar(p, a, 0.020);
    starsInSign += constellationStar(p, b, 0.024);
    starsInSign += constellationStar(p, c, 0.040);
    starsInSign += constellationStar(p, d, 0.027);
    starsInSign += constellationStar(p, e, 0.022);
    starsInSign += constellationStar(p, f, 0.017);

    return (lines * 0.13 + starsInSign * 0.66) * visible * pulse;
}

float constellationSwan(vec3 dir, vec3 center, vec3 up, float scale, float t) {
    float visible;
    vec2 p = constellationCoords(dir, center, up, scale, visible);
    float pulse = 0.76 + 0.24 * sin(t * 0.25 + 4.6);

    vec2 tail = vec2(-0.03, -0.46);
    vec2 body = vec2(0.02, -0.10);
    vec2 neck = vec2(0.05, 0.20);
    vec2 head = vec2(0.12, 0.44);
    vec2 wingL = vec2(-0.42, 0.05);
    vec2 wingR = vec2(0.44, 0.02);
    vec2 featherL = vec2(-0.25, -0.18);
    vec2 featherR = vec2(0.28, -0.20);

    float lines = 0.0;
    lines += constellationLine(p, tail, body, 0.0058);
    lines += constellationLine(p, body, neck, 0.0060);
    lines += constellationLine(p, neck, head, 0.0052);
    lines += constellationLine(p, wingL, body, 0.0054);
    lines += constellationLine(p, body, wingR, 0.0054);
    lines += constellationLine(p, featherL, body, 0.0042);
    lines += constellationLine(p, body, featherR, 0.0042);

    float starsInSign = 0.0;
    starsInSign += constellationStar(p, tail, 0.026);
    starsInSign += constellationStar(p, body, 0.037);
    starsInSign += constellationStar(p, neck, 0.023);
    starsInSign += constellationStar(p, head, 0.019);
    starsInSign += constellationStar(p, wingL, 0.025);
    starsInSign += constellationStar(p, wingR, 0.025);
    starsInSign += constellationStar(p, featherL, 0.014);
    starsInSign += constellationStar(p, featherR, 0.014);

    return (lines * 0.12 + starsInSign * 0.62) * visible * pulse;
}

float constellationLantern(vec3 dir, vec3 center, vec3 up, float scale, float t) {
    float visible;
    vec2 p = constellationCoords(dir, center, up, scale, visible);
    float pulse = 0.82 + 0.18 * sin(t * 0.20 + 6.0);

    vec2 top = vec2(0.00, 0.40);
    vec2 left = vec2(-0.28, 0.10);
    vec2 right = vec2(0.28, 0.08);
    vec2 lowL = vec2(-0.18, -0.25);
    vec2 lowR = vec2(0.18, -0.25);
    vec2 ember = vec2(0.00, -0.42);

    float lines = 0.0;
    lines += constellationLine(p, top, left, 0.0048);
    lines += constellationLine(p, top, right, 0.0048);
    lines += constellationLine(p, left, lowL, 0.0048);
    lines += constellationLine(p, right, lowR, 0.0048);
    lines += constellationLine(p, lowL, lowR, 0.0044);
    lines += constellationLine(p, lowL, ember, 0.0038);
    lines += constellationLine(p, lowR, ember, 0.0038);

    float starsInSign = 0.0;
    starsInSign += constellationStar(p, top, 0.031);
    starsInSign += constellationStar(p, left, 0.021);
    starsInSign += constellationStar(p, right, 0.024);
    starsInSign += constellationStar(p, lowL, 0.019);
    starsInSign += constellationStar(p, lowR, 0.019);
    starsInSign += constellationStar(p, ember, 0.015);

    return (lines * 0.10 + starsInSign * 0.58) * visible * pulse;
}

float realComet(vec3 dir, vec3 head, vec3 previousHead, float cometLength, float width, float fade) {
    vec3 tailDir = previousHead - head * dot(previousHead, head);
    tailDir = normalize(tailDir + vec3(0.0001, 0.0002, 0.0003));
    vec3 sideDir = normalize(cross(head, tailDir));
    vec3 delta = dir - head;
    float along = dot(delta, tailDir);
    float side = abs(dot(delta, sideDir));
    float headDistance = length(vec2(along * 0.78, side));
    float core = 1.0 - smoothstep(width * 0.10, width * 0.58, headDistance);
    float coma = (1.0 - smoothstep(width * 0.45, width * 3.5, headDistance)) * 0.72;
    float wake = step(0.0, along) * (1.0 - smoothstep(0.0, cometLength, along));
    float taper = 1.0 - clamp(along / max(cometLength, 0.001), 0.0, 1.0) * 0.30;
    float ionWidth = width * mix(0.55, 1.50, clamp(along / cometLength, 0.0, 1.0));
    float dustWidth = width * mix(1.15, 4.30, clamp(along / cometLength, 0.0, 1.0));
    float ionTail = (1.0 - smoothstep(ionWidth * 0.22, ionWidth, side)) * wake * exp(-along * 2.4 / cometLength);
    float dustTail = (1.0 - smoothstep(dustWidth * 0.18, dustWidth, side)) * wake * exp(-along * 1.15 / cometLength);
    float broken = 0.78 + 0.22 * fbm(vec2(along * 12.0 - time * 0.22, side * 80.0));
    float tailSpark = realStarLayer(vec2(along * 8.0 + side * 22.0, side * 34.0 + time * 0.05), 30.0, 0.034, 0.952, 17.0, time) * dustTail;
    float frontMask = 1.0 - smoothstep(0.05, 1.25, abs(side) + max(-along, 0.0) * 0.5);
    return (core * 1.55 + coma + ionTail * 0.70 + dustTail * broken * 0.42 + tailSpark * 0.30) * fade * frontMask * taper;
}

vec3 realSkyMode(vec2 plane, vec3 dir, float y, float t, out float amount) {
    float altitude = dir.y;
    float aboveHorizon = smoothstep(-0.12, 0.10, altitude);
    float highSky = smoothstep(-0.03, 0.90, altitude);
    vec3 slowDir = rotateY(dir, t * (0.006 + auroraScale * 0.002));
    vec2 uv = realSkyUv(slowDir);

    vec3 horizonColor = vec3(0.027, 0.038, 0.078);
    vec3 midColor = vec3(0.008, 0.014, 0.038);
    vec3 zenithColor = vec3(0.0015, 0.0045, 0.018);
    vec3 col = mix(horizonColor, midColor, smoothstep(-0.18, 0.35, altitude));
    col = mix(col, zenithColor, highSky);

    float hazeNoise = fbm(vec2(plane.x * 1.4 + t * 0.010, plane.y * 0.85 - t * 0.006));
    float horizonBand = (1.0 - smoothstep(0.00, 0.34, abs(altitude))) * (0.58 + hazeNoise * 0.42);
    col += vec3(0.055, 0.070, 0.120) * horizonBand * (0.35 + horizon * 0.70);
    col += mix(primaryColor, accentColor, 0.5) * horizonBand * 0.030;

    vec3 galaxyNormal = normalize(vec3(0.28, 0.60, 0.75));
    float galaxyDistance = abs(dot(slowDir, galaxyNormal));
    float galaxyBand = 1.0 - smoothstep(0.030, 0.300, galaxyDistance);
    float galaxyDust = fbm(vec2(uv.x * 5.2 + uv.y * 1.6 - t * 0.008, uv.y * 5.8 - uv.x * 1.1 + t * 0.005));
    float darkLane = (1.0 - smoothstep(0.040, 0.180, abs(dot(slowDir, normalize(vec3(0.36, 0.54, 0.76)))))) * noise(uv * 7.0 + 3.0);
    float milkyWay = galaxyBand * smoothstep(0.24, 0.76, galaxyDust) * aboveHorizon;
    col += vec3(0.090, 0.115, 0.190) * milkyWay * (0.68 + glow * 0.16);
    col += mix(primaryColor, accentColor, 0.30) * milkyWay * 0.055;
    col *= 1.0 - darkLane * galaxyBand * 0.13;

    float lonelyVeil = (1.0 - smoothstep(0.018, 0.230, abs(dot(slowDir, normalize(vec3(-0.42, 0.52, 0.74))))))
                     * smoothstep(-0.02, 0.65, altitude)
                     * (0.68 + 0.32 * noise(uv * 4.8 + vec2(t * 0.010, -t * 0.007)));
    col += vec3(0.030, 0.050, 0.100) * lonelyVeil * (0.22 + glow * 0.08);

    float starMask = aboveHorizon * stars;
    vec3 starDirA = slowDir;
    vec3 starDirB = rotateY(slowDir, 1.18);
    vec3 starDirC = rotateY(slowDir, -1.64);
    float fineStars = realStarLayer3D(starDirA, 238.0, 0.0135, 0.986, 2.0, t);
    float brightStars = realStarLayer3D(starDirB, 108.0, 0.0185, 0.978, 8.0, t);
    float blueGiants = realStarLayer3D(starDirC, 62.0, 0.0210, 0.989, 23.0, t);
    float amberGiants = realStarLayer3D(rotateY(slowDir, 2.48), 52.0, 0.0195, 0.991, 31.0, t);
    float cleanTwinkle = pow(max(brightStars + blueGiants * 0.72 + amberGiants * 0.62, 0.0), 1.55);
    col += vec3(0.70, 0.80, 1.0) * fineStars * starMask * 0.60;
    col += vec3(1.0, 0.94, 0.80) * brightStars * starMask * 1.18;
    col += vec3(0.58, 0.76, 1.0) * blueGiants * starMask * 0.78;
    col += vec3(1.0, 0.75, 0.52) * amberGiants * starMask * 0.46;
    col += vec3(0.86, 0.94, 1.0) * cleanTwinkle * starMask * 0.18;

    if (starMask > 0.02) {
        float constellations = 0.0;
        constellations += constellationHunter(slowDir, normalize(vec3(-0.68, 0.38, 0.62)), vec3(0.0, 1.0, 0.0), 0.32, t);
        constellations += constellationCrown(slowDir, normalize(vec3(0.58, 0.50, -0.64)), vec3(0.0, 1.0, 0.0), 0.26, t) * 0.82;
        constellations += constellationSwan(slowDir, normalize(vec3(0.12, 0.70, 0.70)), vec3(0.0, 1.0, 0.0), 0.29, t) * 0.72;
        col += vec3(0.44, 0.58, 0.92) * constellations * starMask * 0.13;
        col += vec3(0.92, 0.95, 1.0) * constellations * starMask * 0.20;
    }

    vec3 moonBase = normalize(vec3(-0.42, 0.38, 0.82));
    vec3 moonDir = rotateY(moonBase, t * 0.007);
    vec3 moonUp = normalize(vec3(0.0, 1.0, 0.0) - moonDir * moonDir.y);
    vec3 moonRight = normalize(cross(moonUp, moonDir));
    float moonAngle = acos(clamp(dot(dir, moonDir), -1.0, 1.0));
    float moonRadius = 0.062;
    vec2 moonLocal = vec2(dot(dir, moonRight), dot(dir, moonUp)) / moonRadius;
    float moonDisc = 1.0 - smoothstep(moonRadius * 0.92, moonRadius * 1.04, moonAngle);
    float moonGlow = (1.0 - smoothstep(moonRadius * 1.6, moonRadius * 5.6, moonAngle)) * aboveHorizon;
    col += vec3(0.22, 0.31, 0.52) * moonGlow * (0.22 + glow * 0.09);
    if (moonGlow > 0.002) {
        float limb = 1.0 - smoothstep(0.68, 1.04, length(moonLocal)) * 0.32;
        float phaseShade = smoothstep(-0.52, 0.42, moonLocal.x + moonLocal.y * 0.14 + 0.16);
        float maria = noise(moonLocal * 4.2 + vec2(1.4, -0.6)) * 0.20 + noise(moonLocal * 8.5 - 2.1) * 0.08;
        float craters = realCrater(moonLocal, vec2(-0.28, 0.25), 0.20)
                      + realCrater(moonLocal, vec2(0.30, -0.18), 0.16)
                      + realCrater(moonLocal, vec2(0.07, 0.42), 0.12)
                      + realCrater(moonLocal, vec2(-0.12, -0.36), 0.13);
        vec3 moonColor = mix(vec3(0.53, 0.56, 0.62), vec3(1.0, 0.96, 0.84), phaseShade);
        moonColor *= limb;
        moonColor -= vec3(0.18, 0.17, 0.16) * (maria + craters * 0.42);
        col = mix(col, moonColor * (1.02 + glow * 0.04), moonDisc);
    }

    float cometCycleA = fract(t * 0.055 + 0.18);
    float cometFadeA = smoothstep(0.04, 0.18, cometCycleA) * (1.0 - smoothstep(0.72, 0.98, cometCycleA));
    float cometCycleB = fract(t * 0.095 + 0.63);
    float cometFadeB = smoothstep(0.02, 0.10, cometCycleB) * (1.0 - smoothstep(0.34, 0.52, cometCycleB));

    if (cometFadeA > 0.002) {
        float cometAngleA = mix(2.85, 3.85, cometCycleA);
        float cometPrevA = mix(2.85, 3.85, max(0.0, cometCycleA - 0.075));
        vec3 cometHeadA = normalize(vec3(cos(cometAngleA) * 0.94, 0.24 + sin(cometCycleA * PI) * 0.42, sin(cometAngleA) * 0.94));
        vec3 cometTailA = normalize(vec3(cos(cometPrevA) * 0.94, 0.24 + sin(max(0.0, cometCycleA - 0.075) * PI) * 0.42, sin(cometPrevA) * 0.94));
        col += vec3(0.68, 0.86, 1.0) * realComet(dir, cometHeadA, cometTailA, 0.34, 0.0058, cometFadeA) * aboveHorizon;
    }
    if (cometFadeB > 0.002) {
        float cometAngleB = mix(5.10, 4.44, cometCycleB);
        float cometPrevB = mix(5.10, 4.44, max(0.0, cometCycleB - 0.060));
        vec3 cometHeadB = normalize(vec3(cos(cometAngleB) * 0.88, 0.52 + sin(cometCycleB * PI) * 0.18, sin(cometAngleB) * 0.88));
        vec3 cometTailB = normalize(vec3(cos(cometPrevB) * 0.88, 0.52 + sin(max(0.0, cometCycleB - 0.060) * PI) * 0.18, sin(cometPrevB) * 0.88));
        col += vec3(0.90, 0.96, 1.0) * realComet(dir, cometHeadB, cometTailB, 0.20, 0.0042, cometFadeB) * aboveHorizon * 0.78;
    }

    amount = 1.0;
    return col * intensity;
}

vec3 auroraMode(vec2 plane, float y, float t, out float amount) {
    vec2 p = vec2(plane.x * 1.24 + plane.y * 0.32, y * 1.92 + plane.y * 0.10) * auroraScale;
    float drift = t * 0.075;
    float warpA = fbm(vec2(p.x * 0.84 + drift, p.y * 0.72 - drift * 0.35));
    float warpB = fbm(vec2(p.x * 1.42 - drift * 0.45, p.y * 1.10 + warpA));
    float lowBand = smoothstep(0.16, 0.66, y) * (1.0 - smoothstep(0.86, 1.0, y));
    float sheet = smoothstep(0.18, 0.72, warpA * 0.62 + warpB * 0.55);
    float folds = pow(0.5 + 0.5 * sin(p.x * 9.0 + warpB * 6.0 + t * 0.18), 2.4);
    float drape = smoothstep(0.70, 0.20, abs(y - (0.46 + (warpA - 0.5) * 0.30)));
    float topMist = smoothstep(0.55, 1.0, y) * fbm(vec2(p.x * 0.9 + warpB, p.y * 1.7 - t * 0.06)) * 0.36;
    amount = (sheet * drape * (0.62 + folds * 0.32) + topMist) * lowBand * intensity;
    return mix(primaryColor, accentColor, smoothstep(0.15, 1.0, warpB + folds * 0.22));
}

vec3 smokeMode(vec2 plane, float y, float t, out float amount) {
    vec2 p = vec2(plane.x * 1.15 + t * 0.025, plane.y * 0.85 + y * 0.75 - t * 0.035) * auroraScale;
    float n1 = fbm(p * 1.15);
    float n2 = fbm(p * 2.05 + vec2(n1 * 0.6, -n1 * 0.35));
    float body = smoothstep(0.34, 0.78, n1 * 0.72 + n2 * 0.48);
    float holes = smoothstep(0.46, 0.82, fbm(p * 3.2 + 8.0));
    float cloud = body * (1.0 - holes * 0.38);
    float vertical = smoothstep(horizon, 0.72, y) * (1.0 - smoothstep(0.98, 1.0, y) * 0.35);
    amount = cloud * vertical * intensity * 0.68;
    return mix(primaryColor * 0.65, accentColor, n2 * 0.65);
}

vec3 plasmaMode(vec2 plane, float y, float t, out float amount) {
    vec2 p = vec2(plane.x * 1.18 + plane.y * 0.26, plane.y * 0.76 + (y - 0.52) * 1.06) * auroraScale;
    float n = fbm(p * 1.45 + vec2(t * 0.035, -t * 0.025));
    vec2 q = p + vec2(n * 0.42, fbm(p * 2.0 - t * 0.028) * 0.34);
    float field1 = sin(q.x * 4.1 + q.y * 2.2 + t * 0.22);
    float field2 = sin(q.x * -2.7 + q.y * 4.8 - t * 0.18);
    float field3 = sin((q.x + q.y) * 3.2 + n * 3.0);
    float veins = lineGlow(field1 + field2 * 0.62, 0.23)
                + lineGlow(field2 - field3 * 0.55, 0.20) * 0.75
                + lineGlow(field1 + field3 * 0.48, 0.18) * 0.55;
    float glowMass = smoothstep(0.34, 0.92, n + veins * 0.18);
    float vertical = smoothstep(0.12, 0.94, y);
    amount = (veins * 0.50 + glowMass * 0.30) * vertical * intensity;
    return mix(primaryColor, accentColor, smoothstep(0.12, 1.05, veins + n * 0.28));
}

vec3 raysMode(vec2 plane, float y, float t, out float amount) {
    vec2 p = vec2(plane.x * 1.16 + plane.y * 0.18, y * 1.12 + plane.y * 0.26) * auroraScale;
    float vertical = smoothstep(horizon, 0.72, y);
    float n = fbm(vec2(p.x * 0.82 + t * 0.020, p.y * 1.18 - t * 0.018));

    float diagonalA = lineGlow(sin((p.x * 1.55 + p.y * 2.10) + n * 2.4 + t * 0.09), 0.34);
    float diagonalB = lineGlow(sin((p.x * -1.25 + p.y * 1.72) + n * 2.1 - t * 0.07), 0.42);
    float wideFan = smoothstep(0.22, 0.90, fbm(vec2(p.x * 1.05 + n * 0.35, p.y * 0.95 + t * 0.016)));
    float glowCloud = smoothstep(0.36, 0.96, n + wideFan * 0.42);
    float fineVeins = pow(max(0.0, sin(p.x * 6.0 + p.y * 2.6 + n * 5.0)), 5.0) * 0.22;

    float rays = (diagonalA * 0.44 + diagonalB * 0.34 + glowCloud * 0.46 + fineVeins) * vertical;
    amount = rays * intensity * 1.25;
    return mix(primaryColor, accentColor, smoothstep(0.18, 1.0, glowCloud + diagonalA * 0.35 + fineVeins));
}

vec3 nebulaMode(vec2 plane, float y, float t, out float amount) {
    vec2 p = vec2(plane.x * 1.1 + t * 0.035, plane.y * 1.1 + y * 0.8 - t * 0.03) * auroraScale;
    float n1 = fbm(p * 1.45);
    float n2 = fbm(p * 2.35 + n1);
    float swirl = smoothstep(0.24, 0.86, n1 * 0.7 + n2 * 0.55);
    amount = swirl * smoothstep(horizon, 1.0, y) * intensity * 0.85;
    return mix(primaryColor, accentColor, n2);
}

void main() {
    vec3 dir = normalize(gl_Color.rgb * 2.0 - 1.0);
    float y = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec2 plane = vec2(dot(dir.xz, vec2(0.86, 0.50)), dot(dir.xz, vec2(-0.50, 0.86)));
    float t = time;

    vec3 lowSky = mix(primaryColor * 0.12, vec3(0.015, 0.085, 0.055), 0.45);
    vec3 highSky = vec3(0.0, 0.025, 0.035);
    vec3 col = mix(lowSky, highSky, smoothstep(0.0, 1.0, y));

    float amount = 0.0;
    vec3 effectColor;
    if (mode == 5) {
        effectColor = realSkyMode(plane, dir, y, t, amount);
        col = effectColor;
    } else if (mode == 1) {
        effectColor = smokeMode(plane, y, t, amount);
    } else if (mode == 2) {
        effectColor = plasmaMode(plane, y, t, amount);
    } else if (mode == 3) {
        effectColor = raysMode(plane, y, t, amount);
    } else if (mode == 4) {
        effectColor = nebulaMode(plane, y, t, amount);
    } else {
        effectColor = auroraMode(plane, y, t, amount);
    }

    if (mode != 5) {
        col += effectColor * amount * glow;
        col += effectColor * amount * amount * 0.55;
    }

    float starUvX = plane.x * 0.38 + plane.y * 0.19 + 0.5;
    float s = starField(vec2(starUvX, y), t) * stars * smoothstep(0.0, 0.75, y);
    if (mode != 5) {
        col += vec3(s);
    }

    vec2 vp = vec2(plane.x * 1.2, (y - 0.5) * 1.15);
    float vignette = smoothstep(1.45, 0.25, length(vp));
    if (mode == 5) {
        col *= 0.88 + 0.12 * vignette;
        col = pow(max(col, 0.0), vec3(0.92));
    } else {
        col *= 0.72 + 0.28 * vignette;
        col = pow(max(col, 0.0), vec3(0.82));
    }

    gl_FragColor = vec4(col, 1.0);
}

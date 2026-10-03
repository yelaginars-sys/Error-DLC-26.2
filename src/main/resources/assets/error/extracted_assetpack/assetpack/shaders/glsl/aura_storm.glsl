#version 120

uniform float time;
uniform float speed;
uniform float intensity;
uniform float rainStrength;
uniform float aspectRatio;
uniform float tanHalfFov;
uniform float horizonFog;
uniform float enableThunder;
uniform float enableLightning;

uniform vec3 cameraForward;
uniform vec3 cameraUp;
uniform vec3 cameraRight;
uniform vec3 fogColor;

varying vec2 screenUv;

float hash11(float n) {
    return fract(sin(n) * 43758.5453123);
}

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

// Плавный 1D noise для излома ствола
float noise1D(float y, float seed) {
    float i = floor(y);
    float f = fract(y);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash21(vec2(i, seed)), hash21(vec2(i + 1.0, seed)), f);
}

float boltPathX(float y, float seed) {
    float x = (noise1D(y * 5.0, seed) - 0.5) * 0.11;
    x += (noise1D(y * 12.0, seed + 2.1) - 0.5) * 0.045;
    x += (noise1D(y * 28.0, seed + 5.4) - 0.5) * 0.018;
    return x;
}

// Тонкая цельная молния с ветками; widthMul — случайная толщина ствола
float lightningBolt(vec2 uv, float seed, float progress, float widthMul) {
    float y = clamp(uv.y, 0.0, 1.0);
    float x = boltPathX(y, seed);
    float d = abs(uv.x - x);
    float w = mix(0.0052, 0.011, clamp(widthMul, 0.0, 1.0));

    float core = smoothstep(w, 0.0, d);
    float mid = smoothstep(w * 2.8, 0.0, d) * 0.4;
    float mask = smoothstep(0.0, 0.06, y) * smoothstep(1.02, 0.9, y);
    float bolt = (core * 1.55 + mid) * mask;

    for (int b = 0; b < 5; b++) {
        float fi = float(b);
        float bh = hash11(seed * 3.1 + fi * 7.7);
        float by0 = 0.2 + bh * 0.55;
        float side = (hash11(seed + fi + 11.0) > 0.5) ? 1.0 : -1.0;
        float along = (by0 - y) * mix(4.5, 7.0, hash11(seed + fi));
        float bMask = smoothstep(0.0, 0.08, along) * smoothstep(1.0, 0.35, along);
        float bx = boltPathX(by0, seed) + side * along * (0.08 + bh * 0.1);
        bx += (noise1D(along * 8.0, seed + fi + 4.0) - 0.5) * 0.03;
        float bd = abs(uv.x - bx);
        float bw = w * 0.75;
        bolt = max(bolt, smoothstep(bw, 0.0, bd) * bMask * 0.75);
    }

    float flicker = 0.8 + 0.2 * hash11(seed * 7.0 + floor(progress * 28.0));
    float env = smoothstep(0.0, 0.025, progress) * smoothstep(0.7, 0.15, progress);
    return bolt * flicker * env;
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 w = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float x00 = mix(n000, n100, w.x);
    float x10 = mix(n010, n110, w.x);
    float x01 = mix(n001, n101, w.x);
    float x11 = mix(n011, n111, w.x);
    return mix(mix(x00, x10, w.y), mix(x01, x11, w.y), w.z);
}

float fbm3(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        s += a * noise3(p);
        p *= 2.03;
        a *= 0.5;
    }
    return s;
}

// Мягкая засветка туч у выхода молнии (как гром, не белый круг)
float lightningOriginGlow(vec3 rd, vec3 originDir, float progress, float cMask) {
    float ang = max(dot(rd, originDir), 0.0);
    float falloff = pow(ang, 14.0) * 0.55 + pow(ang, 6.0) * 0.35;
    falloff *= smoothstep(0.35, 0.85, ang);

    float soft = fbm3(rd * 2.4 + originDir * 1.5);
    soft = smoothstep(0.28, 0.72, soft);
    float detail = fbm3(rd * 5.5 + originDir * 2.0);
    detail = mix(0.65, 1.1, detail);

    float env = smoothstep(0.0, 0.035, progress) * smoothstep(0.5, 0.1, progress);
    float flicker = 0.7 + 0.3 * hash11(floor(progress * 20.0) * 4.1);

    float cloudSoft = mix(0.25, 1.0, cMask);
    cloudSoft *= smoothstep(0.05, 0.35, rd.y);

    return falloff * soft * detail * env * flicker * cloudSoft;
}

vec3 skyWorldDir() {
    vec2 ndc = screenUv * 2.0 - 1.0;
    vec3 viewDir = normalize(vec3(ndc.x * aspectRatio * tanHalfFov, ndc.y * tanHalfFov, -1.0));
    return normalize(viewDir.x * cameraRight + viewDir.y * cameraUp - viewDir.z * cameraForward);
}

// Вращение вокруг оси Y — для воронки
vec3 rotateY(vec3 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
}

mat2 rot2(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, -s, s, c);
}

float sdCapsule(vec2 p, vec2 a, vec2 b, float r) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-5), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

// Силуэт ворона/журавля как на референсе — крылья M/V, тело, голова, хвост
float birdSilhouette(vec2 p, float flap, float kind) {
    float f = clamp(flap, -1.0, 1.0);
    p.x *= mix(0.95, 1.15, kind);

    float d = sdCapsule(p, vec2(-0.18, 0.0), vec2(0.16, 0.02), mix(0.07, 0.095, kind));
    d = min(d, length(p - vec2(0.26, 0.04)) - mix(0.05, 0.065, kind));
    d = min(d, sdCapsule(p, vec2(0.30, 0.035), vec2(0.45, -0.015), 0.02));
    d = min(d, sdCapsule(p, vec2(-0.16, 0.0), vec2(-0.42, 0.0), 0.032));
    d = min(d, sdCapsule(p, vec2(-0.18, 0.0), vec2(-0.40, 0.08), 0.02));
    d = min(d, sdCapsule(p, vec2(-0.18, 0.0), vec2(-0.40, -0.08), 0.02));

    float lift = 0.22 + f * 0.48;
    float dip = -0.02 + f * 0.12;

    d = min(d, sdCapsule(p, vec2(-0.02, 0.05), vec2(-0.45, lift), 0.07));
    d = min(d, sdCapsule(p, vec2(-0.45, lift), vec2(-0.80, lift * 0.4 + dip), 0.048));
    d = min(d, sdCapsule(p, vec2(-0.74, lift * 0.5), vec2(-1.05, dip - 0.1), 0.026));
    d = min(d, sdCapsule(p, vec2(-0.74, lift * 0.55), vec2(-1.08, lift * 0.2), 0.02));
    d = min(d, sdCapsule(p, vec2(-0.70, lift * 0.35), vec2(-1.0, -0.12 + dip), 0.018));

    float liftR = lift * mix(0.9, 1.1, fract(kind * 3.7));
    d = min(d, sdCapsule(p, vec2(0.04, 0.05), vec2(0.42, liftR), 0.07));
    d = min(d, sdCapsule(p, vec2(0.42, liftR), vec2(0.78, liftR * 0.4 + dip), 0.048));
    d = min(d, sdCapsule(p, vec2(0.72, liftR * 0.5), vec2(1.03, dip - 0.1), 0.026));
    d = min(d, sdCapsule(p, vec2(0.72, liftR * 0.55), vec2(1.06, liftR * 0.2), 0.02));
    d = min(d, sdCapsule(p, vec2(0.68, liftR * 0.35), vec2(0.98, -0.12 + dip), 0.018));

    return smoothstep(0.035, -0.012, d);
}

void main() {
    vec3 rd = skyWorldDir();
    float y = rd.y;
    float t = time * max(speed, 0.15);
    float storm = clamp(intensity, 0.35, 3.0);

    // Естественная десатурированная палитра (как на референсе)
    vec3 charcoal = vec3(0.05, 0.055, 0.06);
    vec3 ash = vec3(0.14, 0.135, 0.13);
    vec3 mud = vec3(0.22, 0.18, 0.14);
    vec3 haze = vec3(0.42, 0.40, 0.36);
    vec3 beam = vec3(0.92, 0.90, 0.82);

    // Базовый градиент неба — тяжёлая пасмурность
    float grad = smoothstep(-0.35, 0.85, y);
    vec3 col = mix(mix(mud, ash, 0.55), charcoal, grad);

    // Общая дымка атмосферы
    float atm = pow(clamp(1.0 - abs(y) * 0.85, 0.0, 1.0), 1.4);
    col = mix(col, haze * 0.55, atm * 0.45);

    // Верхние грозовые облака (живут и ползут)
    vec3 wind = vec3(t * 0.07, t * 0.018, t * 0.045);
    vec3 cp = rd * (1.55 + 0.35 * storm) + wind;
    float boil = fbm3(cp * 1.8 + vec3(t * 0.05));
    float cloudsHi = fbm3(cp + vec3(boil * 0.55, -boil * 0.3, boil * 0.2));
    float cloudsLo = fbm3(cp * 1.35 - wind * 0.5 + vec3(11.0, 3.0, 7.0));
    float clouds = pow(clamp(cloudsHi * 0.58 + cloudsLo * 0.5, 0.0, 1.0), 1.05);
    float cMask = smoothstep(0.34, 0.72, clouds) * smoothstep(-0.4, 0.5, y);
    vec3 cDark = charcoal * 1.1;
    vec3 cSoft = mix(ash, haze * 0.7, 0.35);
    col = mix(col, mix(cDark, cSoft, smoothstep(0.4, 0.85, clouds)), cMask * 0.95);

    // Воронка / смерч — крутящаяся масса пыли слева-впереди на сфере
    vec3 funnelAxis = normalize(vec3(-0.55, 0.05, 0.75));
    float along = dot(rd, funnelAxis);
    vec3 radial = rd - funnelAxis * along;
    float funnelAng = atan(radial.x, radial.z) + t * 1.15;
    float radius = length(radial);
    float funnelProfile = smoothstep(0.55, 0.08, radius) * smoothstep(-0.15, 0.65, along);
    funnelProfile *= smoothstep(1.1, 0.15, along + 0.35);
    // закрученные волокна
    vec3 fp = rotateY(rd * 3.2, t * 1.4) + vec3(0.0, -t * 0.35, 0.0);
    float swirl = fbm3(fp * 2.2 + vec3(funnelAng * 0.3, along * 4.0, t * 0.2));
    float debris = fbm3(fp * 4.5 - wind);
    float funnel = funnelProfile * (0.55 + 0.45 * swirl);
    funnel *= 0.7 + 0.3 * storm;
    vec3 funnelCol = mix(charcoal, mud * 0.7, debris);
    col = mix(col, funnelCol, clamp(funnel * 1.15, 0.0, 0.92));
    // пыль у основания
    float baseDust = funnelProfile * smoothstep(0.35, -0.05, y) * (0.4 + 0.6 * debris);
    col = mix(col, mix(mud, ash, 0.4), baseDust * 0.75);

    // Солнце выше на небосводе + лучи преимущественно вниз к горизонту
    vec3 sunDir = normalize(vec3(0.38, 0.55, 0.72));
    float toSun = max(dot(rd, sunDir), 0.0);
    float breakInClouds = 1.0 - smoothstep(0.52, 0.88, clouds);
    float cloudEdge = smoothstep(0.35, 0.65, clouds);

    vec3 sunTint = mix(haze, beam, 0.4);
    sunTint = mix(sunTint, ash * 1.4, 0.25);

    float sunDisk = smoothstep(0.9975, 0.9994, toSun);
    float sunCore = smoothstep(0.9990, 0.9997, toSun);
    float corona = pow(toSun, 55.0) * 0.55 + pow(toSun, 18.0) * 0.18;
    float sunVis = breakInClouds * (0.55 + 0.45 * (1.0 - cloudEdge * 0.5));
    col += sunTint * (sunDisk * 0.55 + sunCore * 0.35 + corona * 0.4) * sunVis;

    vec3 sT = cross(sunDir, vec3(0.0, 1.0, 0.0));
    if (length(sT) < 0.2) {
        sT = cross(sunDir, vec3(1.0, 0.0, 0.0));
    }
    sT = normalize(sT);
    vec3 sB = cross(sunDir, sT);
    vec3 perp = rd - sunDir * toSun;
    float sunAng = atan(dot(perp, sB), dot(perp, sT));
    float dist = length(perp);

    float angNoise = fbm3(vec3(cos(sunAng) * 1.4, sin(sunAng) * 1.4, 2.5));
    float spokes = smoothstep(0.42, 0.78, angNoise);
    spokes *= 0.55 + 0.45 * fbm3(vec3(cos(sunAng * 0.7) * 2.0, sin(sunAng * 0.7) * 2.0, t * 0.05));

    float alongRay = pow(toSun, 5.5);
    float lengthFade = exp(-dist * 3.2);
    float nearSun = smoothstep(0.15, 0.55, toSun);
    float rayBody = alongRay * lengthFade * nearSun * spokes;

    // лучи в основном вниз (ниже солнца), вверх почти гасим
    float downBias = smoothstep(sunDir.y + 0.08, sunDir.y - 0.55, y);
    float upCut = smoothstep(sunDir.y + 0.02, sunDir.y + 0.35, y);
    rayBody *= mix(0.12, 1.0, downBias);
    rayBody *= (1.0 - upCut * 0.92);

    float vol = fbm3(rd * 4.0 + sunDir * 3.0 + vec3(t * 0.06));
    rayBody *= mix(0.65, 1.05, vol);
    rayBody *= breakInClouds;
    rayBody *= smoothstep(-0.08, 0.25, y);
    rayBody *= 0.45 + 0.25 * storm;

    col += sunTint * rayBody;
    col += sunTint * pow(toSun, 10.0) * cloudEdge * breakInClouds * 0.12;

    // Стая птиц — естественные силуэты воронов (как на референсе), не ромбы
    float birds = 0.0;
    for (int i = 0; i < 14; i++) {
        float fi = float(i);
        float seed = fi * 17.913 + 3.1;
        float r0 = hash11(seed);
        float r1 = hash11(seed + 1.7);
        float r2 = hash11(seed + 4.2);
        float r3 = hash11(seed + 8.9);
        float r4 = hash11(seed + 12.4);

        float yaw = (r0 * 2.0 - 1.0) * 2.9;
        float pitch = 0.12 + r1 * 0.5;
        vec3 center = normalize(vec3(sin(yaw) * cos(pitch), sin(pitch), cos(yaw) * cos(pitch)));

        float fly = t * (0.4 + r2 * 0.5) + seed;
        center = normalize(center + vec3(sin(fly * 0.65) * 0.05, cos(fly * 0.5) * 0.025, cos(fly * 0.55) * 0.05));

        vec3 right = normalize(cross(center, vec3(0.0, 1.0, 0.0)));
        if (length(right) < 0.15) {
            right = normalize(cross(center, vec3(1.0, 0.0, 0.0)));
        }
        vec3 up = normalize(cross(right, center));

        // случайный поворот силуэта в плоскости
        float birdRot = (r4 * 2.0 - 1.0) * 0.85 + sin(fly * 0.3) * 0.15;
        float ca = cos(birdRot);
        float sa = sin(birdRot);
        vec3 ax = right * ca + up * sa;
        vec3 ay = -right * sa + up * ca;

        vec3 toRay = rd - center;
        if (dot(toRay, toRay) <= 0.018) {
            float size = mix(0.0045, 0.0095, r3);
            vec2 p = vec2(dot(toRay, ax), dot(toRay, ay)) / size;

            float flapSpeed = mix(3.2, 6.5, r2);
            float flap = sin(fly * flapSpeed) * mix(0.45, 1.0, r1);
            if (r0 > 0.72) {
                flap *= 0.25;
            }

            float sil = birdSilhouette(p, flap, r3);
            sil *= smoothstep(0.05, 0.2, y);
            sil *= smoothstep(0.14, 0.02, length(toRay));
            birds = max(birds, sil);
        }
    }
    col = mix(col, charcoal * 0.25, birds * 0.58);

    // Мелкая пыль / песок в воздухе
    float grit = fbm3(rd * 22.0 + vec3(t * 0.6, t * 0.25, -t * 0.4));
    grit = smoothstep(0.62, 0.9, grit) * atm * 0.12;
    col += haze * grit;

    // Гром (засветка туч) и молнии — рандомно чередуются, без клетчатого floor(rd)
    float eventPeriod = 1.35;
    float eventT = floor(t / eventPeriod);
    float eventLife = fract(t / eventPeriod);
    float eventRoll = hash11(eventT * 13.7);
    float eventKind = hash11(eventT * 5.3);
    float eventActive = step(0.55, eventRoll);

    // Учитываем мультисеттинг: если включён только один тип — форсим его
    bool wantThunder = enableThunder > 0.5;
    bool wantBolt = enableLightning > 0.5;
    if (wantThunder && !wantBolt) {
        eventKind = 0.0;
    } else if (wantBolt && !wantThunder) {
        eventKind = 1.0;
    } else if (!wantThunder && !wantBolt) {
        eventActive = 0.0;
    }

    vec3 boltCol = vec3(0.72, 0.82, 1.0);
    float thunder = 0.0;
    float bolt = 0.0;

    if (eventActive > 0.5) {
        if (eventKind < 0.42 && wantThunder) {
            // ГРОМ — мягкая вспышка по облакам (fbm, без сетки)
            float env = smoothstep(0.0, 0.04, eventLife) * smoothstep(0.35, 0.08, eventLife);
            float soft = fbm3(rd * 2.2 + vec3(eventT * 0.7));
            soft = smoothstep(0.35, 0.75, soft);
            float pulse = 0.65 + 0.35 * hash11(eventT * 2.1 + floor(eventLife * 25.0));
            thunder = env * soft * pulse * cMask;
            // второй короткий «раскат»
            float env2 = smoothstep(0.12, 0.16, eventLife) * smoothstep(0.28, 0.18, eventLife);
            thunder = max(thunder, env2 * soft * 0.55 * cMask);
        } else if (wantBolt) {
            // МОЛНИЯ — случайная длина (до земли / короче) и толщина
            float seed = eventT * 3.17 + 1.4;
            float boltAz = (hash11(seed) - 0.5) * 2.4;
            float widthMul = hash11(seed + 6.2);
            float lengthMul = hash11(seed + 14.8); // 0..1 → длина ствола
            float az = atan(rd.x, rd.z);
            float dAz = atan(sin(az - boltAz), cos(az - boltAz));
            float topY = 0.82;
            // иногда до горизонта, иногда заметно короче (примерно до x0.5)
            float botY = mix(0.38, -0.02, lengthMul);
            float boltSpan = max(topY - botY, 0.15);
            float boltY = clamp((topY - y) / boltSpan, 0.0, 1.0);
            // ниже конца ствола молнию не рисуем
            float lengthMask = step(botY - 0.02, y) * step(y, topY + 0.05);
            vec2 luv = vec2(dAz * 4.5, 1.0 - boltY);
            bolt = lightningBolt(luv, seed, eventLife, widthMul) * lengthMask;

            vec3 originDir = normalize(vec3(sin(boltAz) * cos(topY * 1.2), topY, cos(boltAz) * cos(topY * 1.2)));
            float originFlash = lightningOriginGlow(rd, originDir, eventLife, cMask);

            if (hash11(seed + 8.0) > 0.62) {
                float boltAz2 = (hash11(seed + 2.5) - 0.5) * 2.4;
                float widthMul2 = hash11(seed + 9.1);
                float lengthMul2 = hash11(seed + 17.3);
                float botY2 = mix(0.38, -0.02, lengthMul2);
                float boltSpan2 = max(topY - botY2, 0.15);
                float boltY2 = clamp((topY - y) / boltSpan2, 0.0, 1.0);
                float lengthMask2 = step(botY2 - 0.02, y) * step(y, topY + 0.05);
                float dAz2 = atan(sin(az - boltAz2), cos(az - boltAz2));
                vec2 luv2 = vec2(dAz2 * 4.5, 1.0 - boltY2);
                bolt = max(bolt, lightningBolt(luv2, seed + 4.0, eventLife, widthMul2) * lengthMask2 * 0.7);
                vec3 originDir2 = normalize(vec3(sin(boltAz2) * cos(topY * 1.2), topY, cos(boltAz2) * cos(topY * 1.2)));
                originFlash = max(originFlash, lightningOriginGlow(rd, originDir2, eventLife, cMask) * 0.85);
            }

            bolt *= smoothstep(0.0, 0.12, y) * smoothstep(0.95, 0.7, y);
            thunder = bolt * cMask * 0.35;
            vec3 flashCol = mix(haze * 1.2, vec3(0.82, 0.88, 1.0), 0.55);
            col += flashCol * originFlash * 0.95;
            col = mix(col, flashCol, originFlash * cMask * 0.35);
        }
    }

    col += boltCol * bolt * (1.15 + 0.35 * storm);
    col += mix(haze, boltCol, 0.5) * thunder * (0.7 + 0.4 * storm);

    // Дождевая пелена (мягче, без жёсткой сетки)
    vec2 rainUv = vec2(rd.x * 1.1 + rd.z * 0.9, y * 2.0);
    rainUv.y += t * 3.8;
    rainUv.x += t * 0.55 + fbm3(vec3(rainUv * 0.5, t * 0.1)) * 0.55;
    float streak = abs(fract(rainUv.x * 11.0 + fbm3(vec3(rainUv.y, t, 0.0)) * 1.5) - 0.5);
    float rain = smoothstep(0.05, 0.0, streak) * smoothstep(0.5, -0.15, y);
    rain *= 0.16 + 0.1 * storm;
    col = mix(col, mix(col, ash, 0.4), rain);

    // Контраст и лёгкая десатурация «плёнки»
    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(luma), col, 0.35);
    col *= mix(0.92, 1.08, clamp(storm * 0.25, 0.0, 1.0));
    col *= (1.0 - rainStrength * 0.25);
    col = clamp(col, 0.0, 1.2);

    // Цельный переход в туман у горизонта
    float toFog = smoothstep(0.48, -0.12, y) * clamp(horizonFog, 0.0, 1.0);
    vec3 naturalFog = mix(mud * 0.85, fogColor, 0.35);
    if (horizonFog < 0.1) {
        naturalFog = mix(mud, ash, 0.5);
        toFog = smoothstep(0.35, -0.15, y) * 0.55;
    }
    col = mix(col, naturalFog, toFog);

    gl_FragColor = vec4(col, 1.0);
}

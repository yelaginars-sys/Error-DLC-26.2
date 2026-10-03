#version 150

// Порт FRAG_SRC из faduba 1.16.5 BlockOutline (#version 120 -> 150 core).
// Эффект рисуется по UV каждой грани блока [0,1]; uMode выбирает один из 4
// процедурных шейдеров, плюс анимированная рамка по краю грани.

in vec2 vUv;

uniform float iTime;
uniform float uAlpha;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform float uParticles;
uniform float uThickness;
uniform int uMode;
uniform int uIterA;
uniform int uIterB;

out vec4 OutColor;

float hash(float n) { return fract(sin(n) * 43758.5453123); }
float hash2(vec2 n) { return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453); }
float hash3(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }

// ===== Shader 1: Particles =====
vec2 getPos1(float id, float t) {
    float h1 = hash(id);
    float h2 = hash(id + 50.0);
    float speed = 0.05 + h1 * 0.15;
    vec2 dir = vec2(sin(h1 * 6.28), cos(h2 * 6.28));
    vec2 pos = dir * t * speed + (vec2(h1, h2) * 2.0 - 1.0);
    return fract(pos * 0.5 + 0.5) * 2.4 - 1.2;
}
vec3 shaderParticles(vec2 uv, float t) {
    vec3 col = vec3(0.0);
    for (int i = 0; i < 50; i++) {
        if (i >= uIterA) break;
        float fi = float(i);
        float h = hash(fi);
        vec2 p = getPos1(fi, t);
        vec2 dv = uv - p;
        float d2 = dot(dv, dv);
        float d = sqrt(d2);
        float sharp = 0.0006 / (d + 0.0001);
        float strongGlow = 0.008 / (d2 + 0.0004);
        float wideBloom = 0.02 / (d + 0.08);
        vec3 c = mix(uColor1, uColor2, h);
        float pulse = 0.7 + 0.3 * sin(t * 3.0 + fi);
        col += c * (sharp * pulse + strongGlow * 0.5 * pulse + wideBloom * 0.2);
    }
    col = pow(col, vec3(0.8));
    col *= 1.2 - length(uv);
    return col;
}

// ===== Shader 2: Clouds (Aiekick) =====
float pn3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(p);
    float n100 = hash3(p + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(p + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(p + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(p + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(p + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(p + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(p + vec3(1.0, 1.0, 1.0));
    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);
    return -1.0 + 2.4 * mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
}
float df2(vec3 p, float t) {
    float n0 = pn3(p * 0.26);
    float n = n0 * 2.60 + pn3(p * 1.17) * 0.39;
    return abs(p.y) + cos(p.z + t + n * 4.0) + cos((p.y + t + n) * 2.0) + n - 3.0;
}
vec3 cam2(vec2 uv, vec3 ro, vec3 cu, vec3 cv, float fov) {
    vec3 rov = normalize(cv - ro);
    vec3 u = normalize(cross(cu, rov));
    vec3 v = normalize(cross(rov, u));
    return normalize(rov + fov * u * uv.x + fov * v * uv.y);
}
vec3 shaderClouds(vec2 uv, float t) {
    vec3 f = vec3(0.0, 0.15, 0.32);
    vec3 ro = vec3(0.0, 0.0, t * 1.5);
    vec3 rd = cam2(uv, ro, vec3(0.0, 1.0, 0.0), ro + vec3(0.0, 0.0, 1.0), 5.0);
    vec3 s = vec3(1.0), h = vec3(0.16, 0.008, 0.032), w = vec3(0.0);
    float d = 1.0, dl = 0.0, td = 0.0;
    vec3 p = ro;
    for (int i = 0; i < 32; i++) {
        if (i >= uIterB || s.x < 0.01 || d > 35.0 || td > 0.92) break;
        float fi = float(i);
        s = vec3(df2(p, t)) * 0.1 * fi / vec3(107.0, 160.0, 72.0);
        w = (1.0 - td) * (h - s) * fi / vec3(61.0, 27.0, 54.0) * step(s, h);
        f += w;
        td += w.x + 0.01;
        dl += 1.0 - exp(-0.001 * log(d));
        s = max(s, 0.396);
        d += s.x;
        p = ro + rd * d;
    }
    dl += 2.52;
    f /= dl / 7.04;
    f = mix(f, vec3(0.0), 1.0 - exp(-0.0017 * d * d));
    float lum = (f.r + f.g + f.b) / 3.0;
    return mix(uColor1, uColor2, clamp(lum * 1.5, 0.0, 1.0)) * (0.3 + lum * 2.0);
}

// ===== Shader 3: Warped fBM (pink noise) =====
float n2(vec2 p) {
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u * u * (3.0 - 2.0 * u);
    float r = mix(
        mix(hash2(ip), hash2(ip + vec2(1.0, 0.0)), u.x),
        mix(hash2(ip + vec2(0.0, 1.0)), hash2(ip + vec2(1.0, 1.0)), u.x),
        u.y);
    return r * r;
}
float fbm3(vec2 p, float t) {
    mat2 m = mat2(0.80, 0.60, -0.60, 0.80);
    float f = 0.0;
    f += 0.5 * n2(p + t); p = m * p * 2.02;
    f += 0.25 * n2(p); p = m * p * 2.03;
    f += 0.125 * n2(p); p = m * p * 2.01;
    f += 0.0625 * n2(p); p = m * p * 2.04;
    f += 0.015625 * n2(p + sin(t));
    return f / 0.96875;
}
float pattern3(vec2 p, float t) {
    return fbm3(p + fbm3(p + fbm3(p, t), t), t);
}
vec3 shaderWarp(vec2 uv01, float t) {
    float shade = pattern3(uv01 * 3.0, t);
    vec3 col = mix(uColor1, uColor2, clamp(shade, 0.0, 1.0));
    return col * (0.3 + shade * 1.5);
}

// ===== Shader 4: Octgrams =====
mat2 oct_rot(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}
float oct_sdBox(vec3 p, vec3 b) {
    vec3 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}
float oct_box(vec3 pos, float scale) {
    pos *= scale;
    float base = oct_sdBox(pos, vec3(0.4, 0.4, 0.1)) / 1.5;
    pos.xy *= 5.0;
    pos.y -= 3.5;
    pos.xy *= oct_rot(0.75);
    return -base;
}
float oct_boxSet(vec3 pos, float gTime) {
    vec3 po = pos;
    float sx = sin(gTime * 0.4) * 2.5;
    float scale = 2.0 - abs(sin(gTime * 0.4)) * 1.5;

    pos = po; pos.y += sx; pos.xy *= oct_rot(0.8);
    float b1 = oct_box(pos, scale);
    pos = po; pos.y -= sx; pos.xy *= oct_rot(0.8);
    float b2 = oct_box(pos, scale);
    pos = po; pos.x += sx; pos.xy *= oct_rot(0.8);
    float b3 = oct_box(pos, scale);
    pos = po; pos.x -= sx; pos.xy *= oct_rot(0.8);
    float b4 = oct_box(pos, scale);
    pos = po; pos.xy *= oct_rot(0.8);
    float b5 = oct_box(pos, 0.5) * 6.0;
    pos = po;
    float b6 = oct_box(pos, 0.5) * 6.0;

    return max(max(max(max(max(b1, b2), b3), b4), b5), b6);
}
vec3 shaderOctgrams(vec2 p, float iT) {
    vec3 ro = vec3(0.0, -0.2, iT * 4.0);
    vec3 ray = normalize(vec3(p, 1.5));
    ray.xy = ray.xy * oct_rot(sin(iT * 0.03) * 5.0);
    ray.yz = ray.yz * oct_rot(sin(iT * 0.05) * 0.2);
    float t = 0.1;
    float ac = 0.0;
    for (int i = 0; i < 40; i++) {
        if (i >= uIterB || t > 22.0 || ac > 60.0) break;
        vec3 pos = ro + ray * t;
        pos = mod(pos - 2.0, 4.0) - 2.0;
        float gTime = iT - float(i) * 0.01;
        float d = oct_boxSet(pos, gTime);
        d = max(abs(d), 0.01);
        ac += exp(-d * 23.0);
        t += d * 0.55;
    }
    vec3 col = vec3(ac * 0.02);
    vec3 tint = mix(uColor1, uColor2, 0.5 + 0.5 * sin(iT));
    col += tint * (0.25 + 0.2 * abs(sin(iT)));
    float fade = clamp(1.0 - t * (0.02 + 0.02 * sin(iT)), 0.0, 1.0);
    return col * fade * (0.6 + tint * 0.4);
}

void main() {
    vec2 uv = vUv * 2.0 - 1.0;
    float t = iTime * 0.8;
    vec3 inner = vec3(0.0);

    if (uParticles > 0.001) {
        if (uMode == 1) {
            inner = shaderParticles(uv, t);
        } else if (uMode == 2) {
            inner = shaderClouds(uv, t);
        } else if (uMode == 3) {
            inner = shaderWarp(vUv, t);
        } else {
            inner = shaderOctgrams(uv, t);
        }
        inner *= uParticles;
    }

    float edge = min(min(vUv.x, 1.0 - vUv.x), min(vUv.y, 1.0 - vUv.y));
    float edgeLine = smoothstep(uThickness, 0.0, edge);
    vec3 outlineCol = mix(uColor1, uColor2, 0.5 + 0.5 * sin(t * 2.0));
    vec3 finalCol = inner + outlineCol * edgeLine * 2.2;
    OutColor = vec4(finalCol * uAlpha, 1.0);
}

#version 150

// Ambilance / «Море» — райтрейс отражающей водной плоскости и процедурного неба
// с солнцем и облаками. Оригинал брал шум из iChannel0 (RGBA noise 256);
// канала нет, поэтому noise3 честно процедурный (hash-based value noise).
// Плотность облаков (ufCloudCover) вынесена в uniform Cover.

in vec2 TexCoord;
out vec4 OutColor;

uniform float Time;
uniform float Alpha;
uniform float Bright;
uniform float Sat;
uniform float Cover;      // облачность 0..1
uniform vec3  Tint;
uniform float TintMix;
uniform vec3  Look;
uniform vec3  Right;
uniform vec3  Up;
uniform float TanHalf;
uniform float Aspect;
uniform float WorldLock;  // 1 = луч берётся у камеры, 0 = собственная камера шейдера

vec2 ndcPos() { return TexCoord * 2.0 - 1.0; }

vec3 grade(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(c, Tint * l * 1.25, TintMix);
    l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, Sat);
    return clamp(c * Bright, 0.0, 1.0);
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash13(p + vec3(0.0, 0.0, 0.0)), hash13(p + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash13(p + vec3(0.0, 1.0, 0.0)), hash13(p + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash13(p + vec3(0.0, 0.0, 1.0)), hash13(p + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash13(p + vec3(0.0, 1.0, 1.0)), hash13(p + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float map(vec3 p) {
    vec3 q = p + 0.2 * vec3(3.0, 0.3, 5.0) * mod(Time, 3600.0) * 2.0;
    float n = 0.0, f = 0.5;
    n += f * noise3(q); q *= 3.001; f *= 0.333;
    n += f * noise3(q); q *= 3.002; f *= 0.332;
    n += f * noise3(q);
    return n;
}

float scene(vec3 p) {
    return p.y + 2.0 - 0.003 * map(vec3(p.x, 0.0, p.z));
}

vec3 normalAt(vec3 p, float d) {
    float e = 0.05;
    float dx = scene(vec3(e, 0.0, 0.0) + p) - d;
    float dy = scene(vec3(0.0, e, 0.0) + p) - d;
    float dz = scene(vec3(0.0, 0.0, e) + p) - d;
    return normalize(vec3(dx, dy, dz));
}

vec3 shadeBg(vec3 nml) {
    vec3 bgLight = normalize(vec3(
        sin(Time * 0.5) * 0.1,
        cos(Time * 0.1) * 0.6 - 0.3,
        -1.0
    ));
    float sunD = dot(bgLight, nml) > 0.995 ? 1.0 : 0.0;
    vec3 sun = vec3(6.5, 3.5, 2.0);
    float skyPow = dot(nml, vec3(0.0, -1.0, 0.0));
    float horizonPow = pow(1.0 - abs(skyPow), 3.0) * 5.0;
    float sunPow = dot(nml, bgLight);
    float sp = max(sunPow, 0.0);
    float scattering = clamp(1.0 - abs(2.0 * (-bgLight.y)), 0.0, 1.0);
    vec3 bgCol = max(0.0, skyPow) * 2.0 * vec3(0.8);
    bgCol += 0.5 * vec3(0.8) * horizonPow;
    bgCol += sun * (sunD + pow(sp, max(128.0, abs(bgLight.y) * 512.0)));
    bgCol += vec3(0.4, 0.2, 0.15) * (pow(sp, 8.0) + pow(sp, max(8.0, abs(bgLight.y) * 128.0)));
    bgCol *= mix(vec3(0.7, 0.85, 0.95), vec3(1.0, 0.45, 0.1), scattering);
    bgCol *= 1.0 - clamp(bgLight.y * 3.0, 0.0, 0.6);

    float cloudFac = pow(abs(skyPow), 0.8);

    // Кривая ufCloudCover -> cc из оригинала, один в один.
    float cc;
    if (Cover < 0.1)       cc = 0.0 + 0.2 * (Cover - 0.0) / 0.1;
    else if (Cover < 0.2)  cc = 0.2 + 0.1 * (Cover - 0.1) / 0.1;
    else if (Cover < 0.3)  cc = 0.3 + 0.1 * (Cover - 0.2) / 0.1;
    else if (Cover < 0.5)  cc = 0.4 + 0.1 * (Cover - 0.3) / 0.3;
    else if (Cover < 0.75) cc = 0.5 + 0.2 * pow((Cover - 0.5) / 0.25, 2.0);
    else                   cc = 0.7 + 0.75 * pow((Cover - 0.75) / 0.25, 2.0);

    // nml/nml.y на горизонте улетает в бесконечность — зажимаем знаменатель.
    float ny = sign(nml.y + 1e-9) * max(abs(nml.y), 0.02);
    float cloud = 0.0;
    cloud += min(1.0, (1.0 - smoothstep(0.0, cc, map(nml / ny))) ) * 0.4;
    cloud += min(1.0, (1.0 - smoothstep(0.0, cc, map(nml * 1.03 / ny))) ) * 0.4;
    cloud += min(1.0, (1.0 - smoothstep(0.0, cc, map(nml * 3.0 / ny))) ) * 0.3;
    bgCol *= 1.0 + cloudFac * cloud;

    return pow(max(vec3(0.0), bgCol), vec3(2.6));
}

mat3 rotationXY(vec2 angle) {
    float cp = cos(angle.x);
    float sp = sin(angle.x);
    float cy = cos(angle.y);
    float sy = sin(angle.y);
    return mat3( cy, -sy, 0.0,
                 sy,  cy, 0.0,
                0.0, 0.0, 1.0) *
           mat3( cp, 0.0, -sp,
                0.0, 1.0, 0.0,
                 sp, 0.0,  cp);
}

void main() {
    vec2 uv = ndcPos() * vec2(Aspect, 1.0);

    // Экранный режим — собственная качающаяся камера шейдера.
    // Режим «Мир» — луч из реальных векторов камеры игры, небо стоит на месте.
    mat3 rot = rotationXY(vec2(0.2 + 0.2 * cos(0.5 * Time), -0.15 * sin(0.5 + 0.5 * Time)));
    vec3 own = rot * normalize(vec3(uv, 1.0));
    vec3 cam = normalize(Look + TanHalf * (ndcPos().x * Aspect * Right + ndcPos().y * Up));
    vec3 d = normalize(mix(own, cam, WorldLock));

    vec3 p = vec3(uv * -2.0, -9.5);
    vec3 tr = vec3(1.0);
    if (d.y < 0.0) {
        float dist = -2.0 / d.y - p.y / d.y;
        p += d * dist;
        vec3 nml = normalAt(p, 0.0);
        float f = pow(1.0 - dot(d, -vec3(0.0, 1.0, 0.0)), 5.0);
        nml = mix(nml, vec3(0.0, 1.0, 0.0), f);
        d = reflect(d, nml);
        tr *= mix(0.5 * vec3(0.5, 0.9, 0.75), vec3(1.0), f);
    }

    vec3 col = tr * shadeBg(-d);

    float dither = (hash13(vec3(gl_FragCoord.xy, 1.0)) - 0.5) / 64.0;
    vec3 outc = pow(max(vec3(0.0), vec3(dither) + (1.0 - exp(-1.3 * col))), vec3(1.3));

    OutColor = vec4(grade(outc), Alpha);
}

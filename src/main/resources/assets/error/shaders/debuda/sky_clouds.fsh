#version 150

// Ambilance / «Облака» — тот же шейдер, что был в twoпроект (2D value-noise fbm,
// спроецированный на горизонтальный слой). Небо привязано к миру: камера его
// осматривает, а не тащит за собой.

in vec2 TexCoord;
out vec4 OutColor;

uniform float Time;       // накопленное время (скорость применена на стороне Java)
uniform float Alpha;      // прозрачность
uniform float Bright;     // яркость
uniform float Sat;        // насыщенность
uniform float Scale;      // масштаб рисунка
uniform float Detail;     // число октав
uniform float Cover;      // плотность облаков
uniform vec3  Tint;       // цвет подкраски
uniform float TintMix;    // сила подкраски 0..1
uniform vec3  Color1;     // цвет неба у горизонта
uniform vec3  Color2;     // цвет неба в зените
uniform vec3  Look;       // мировые векторы камеры
uniform vec3  Right;
uniform vec3  Up;
uniform float TanHalf;    // tan(fov/2)
uniform float Aspect;     // w/h
uniform float WorldLock;  // 1 = привязка к миру, 0 = экранные координаты
uniform float Dome;       // 1 = купол (без сплющивания), 0 = плоский слой

vec2 ndcPos() { return TexCoord * 2.0 - 1.0; }

vec3 rayDir() {
    vec2 n = ndcPos();
    return normalize(Look + TanHalf * (n.x * Aspect * Right + n.y * Up));
}

// «Плоскость» — исходный вариант: луч кладётся на горизонтальный слой облаков.
// У горизонта d.y -> 0, знаменатель упирается в зажим, а d.xz -> 1: картинка
// растягивается вбок и сплющивается в полосу — то самое «кривое сжатое вдали».
vec2 flatUv(vec3 d) { return (d.xz / max(abs(d.y) + 0.15, 0.15)) * 0.6; }

// «Купол» — стереографическая проекция полусферы неба. Знаменатель никогда не
// меньше 1, проекция конформная: рисунок нигде не сплющивается, у горизонта
// детали лишь плавно крупнеют. Множитель 1.2 держит прежний размер шума на
// середине высоты, чтобы «Масштаб» из настроек значил то же, что и раньше.
vec2 domeUv(vec3 d) { return (d.xz / (1.0 + abs(d.y))) * 1.2; }

vec2 planeUv(vec3 d) { return mix(flatUv(d), domeUv(d), Dome); }

vec3 grade(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(c, Tint * l * 1.25, TintMix);
    l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, Sat);
    return clamp(c * Bright, 0.0, 1.0);
}

vec2 hash1(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float noise1(vec2 p) {
    const float K1 = 0.366025404;
    const float K2 = 0.211324865;
    vec2 i = floor(p + (p.x + p.y) * K1);
    vec2 a = p - i + (i.x + i.y) * K2;
    vec2 o = (a.x > a.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec2 b = a - o + K2;
    vec2 cc = a - 1.0 + 2.0 * K2;
    vec3 hh = max(0.5 - vec3(dot(a, a), dot(b, b), dot(cc, cc)), 0.0);
    vec3 n = hh * hh * hh * hh * vec3(dot(a, hash1(i + 0.0)), dot(b, hash1(i + o)), dot(cc, hash1(i + 1.0)));
    return dot(n, vec3(70.0));
}

float fbm1(vec2 n) {
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    float total = 0.0, amplitude = 0.1;
    for (int i = 0; i < 7; i++) {
        total += noise1(n) * amplitude;
        n = m * n;
        amplitude *= 0.4;
    }
    return total;
}

void main() {
    vec3 d = rayDir();
    vec2 base = mix(ndcPos() * vec2(Aspect, 1.0), planeUv(d), WorldLock) * Scale;
    float horizon = clamp(mix(TexCoord.y, d.y * 0.6 + 0.4, WorldLock), 0.0, 1.0);

    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    float cloudscale = 1.1;
    float clouddark = 0.5;
    float cloudlight = 0.3;
    float cloudalpha = 8.0;
    float skytint = 0.5;

    int octaves = int(max(Detail, 2.0));

    vec2 uv = base;
    float time = Time * 0.03;
    float q = fbm1(uv * cloudscale * 0.5);

    float r = 0.0;
    uv *= cloudscale;
    uv -= q - time;
    float weight = 0.8;
    for (int i = 0; i < octaves; i++) {
        r += abs(weight * noise1(uv));
        uv = m * uv + time;
        weight *= 0.7;
    }

    float f = 0.0;
    uv = base;
    uv *= cloudscale;
    uv -= q - time;
    weight = 0.7;
    for (int i = 0; i < octaves; i++) {
        f += weight * noise1(uv);
        uv = m * uv + time;
        weight *= 0.6;
    }
    f *= r + f;

    float c = 0.0;
    time = Time * 0.03 * 2.0;
    uv = base;
    uv *= cloudscale * 2.0;
    uv -= q - time;
    weight = 0.4;
    for (int i = 0; i < octaves - 1; i++) {
        c += weight * noise1(uv);
        uv = m * uv + time;
        weight *= 0.6;
    }

    float c1 = 0.0;
    time = Time * 0.03 * 3.0;
    uv = base;
    uv *= cloudscale * 3.0;
    uv -= q - time;
    weight = 0.4;
    for (int i = 0; i < octaves - 1; i++) {
        c1 += abs(weight * noise1(uv));
        uv = m * uv + time;
        weight *= 0.6;
    }
    c += c1;

    vec3 skycolour = mix(Color2, Color1, horizon);
    vec3 cloudcolour = vec3(1.1, 1.1, 0.9) * clamp((clouddark + cloudlight * c), 0.0, 1.0);
    f = Cover + cloudalpha * f * r;
    vec3 col = mix(skycolour, clamp(skytint * skycolour + cloudcolour, 0.0, 1.0), clamp(f + c, 0.0, 1.0));

    OutColor = vec4(grade(col), Alpha);
}

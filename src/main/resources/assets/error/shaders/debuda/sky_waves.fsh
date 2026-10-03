#version 150

// Ambilance / «Волны» — shadertoy-шейдер с итеративным искривлением uv косинусами
// и делением на abs(sin(...)): тонкие светящиеся жилы, ползущие по экрану.

in vec2 TexCoord;
out vec4 OutColor;

uniform float Time;
uniform float Alpha;
uniform float Bright;
uniform float Sat;
uniform float Scale;
uniform float Detail;     // число итераций искривления (оригинал = 10)
uniform vec3  Tint;
uniform float TintMix;
uniform vec3  Look;
uniform vec3  Right;
uniform vec3  Up;
uniform float TanHalf;
uniform float Aspect;
uniform float WorldLock;
uniform float Dome;       // 1 = купол (без сплющивания у горизонта), 0 = плоский слой

vec2 ndcPos() { return TexCoord * 2.0 - 1.0; }

vec3 rayDir() {
    vec2 n = ndcPos();
    return normalize(Look + TanHalf * (n.x * Aspect * Right + n.y * Up));
}

// Плоский слой (исходный) против стереографического купола — см. sky_clouds.fsh.
vec2 flatUv(vec3 d) { return (d.xz / max(abs(d.y) + 0.15, 0.15)) * 0.6; }
vec2 domeUv(vec3 d) { return (d.xz / (1.0 + abs(d.y))) * 1.2; }
vec2 planeUv(vec3 d) { return mix(flatUv(d), domeUv(d), Dome); }

vec3 grade(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(c, Tint * l * 1.25, TintMix);
    l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, Sat);
    return clamp(c * Bright, 0.0, 1.0);
}

void main() {
    // Оригинал: uv = (2*frag - res) / min(res.x, res.y) — это ndc, растянутый
    // по длинной оси. В режиме «Мир» вместо ndc берём проекцию луча взгляда.
    vec2 uv = mix(ndcPos() * vec2(Aspect, 1.0), planeUv(rayDir()) * 1.8, WorldLock) * Scale;

    float iters = max(Detail, 2.0);
    for (float i = 1.0; i < iters; i++) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + Time);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + Time);
    }

    // abs(sin(...)) уходит в ноль -> оригинал улетает в бесконечность; зажимаем
    // знаменатель, иначе на слабой точности получаются белые артефакты-точки.
    float den = max(abs(sin(Time - uv.y - uv.x)), 0.02);
    vec3 col = vec3(0.1) / den;

    OutColor = vec4(grade(col), Alpha);
}

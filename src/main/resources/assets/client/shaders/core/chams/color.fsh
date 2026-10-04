#version 330

// Стили 3D-чамса по Type из UBO SadnesChams: 0 = Flat, 1 = Glass, 2 = Blur,
// 3 = Cosmic. Sampler0 — родная текстура модели: alpha даёт cutout (прозрачные
// части скина не заливаются), luma — объём. Sampler1 — сцена БЕЗ сущностей
// (копия кадра до их рендера, см. capturePreEntityScene): Glass преломляет её,
// Blur размывает то, что ЗА моделькой (морозное стекло). localPos — координаты
// относительно сущности: узоры приклеены к мобу и не плывут при движении.
// Альфа (Intensity, vertexColor.a) растворяет модель ЦЕЛИКОМ: пиксель пишется
// заменой как mix(сцена позади, стиль, alpha) — при низкой альфе сквозь модель
// видно мир, а не ванильную модельку под заливкой.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
// Та же сцена, но размытая dual-filter цепочкой (для Blur); иначе = Sampler1.
uniform sampler2D Sampler2;

layout(std140) uniform SadnesChams {
    float Time;
    float Speed;
    float Scale;
    float Type;
};

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;
in vec3 viewNormal;
in vec3 localPos;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
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
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        v += amp * noise(p);
        p *= 2.0;
        amp *= 0.5;
    }
    return v;
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float ndv = clamp(abs(dot(n, v)), 0.0, 1.0);
    float fresnel = pow(1.0 - ndv, 2.5);
    float lum = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
    float t = Time * Speed;
    int type = int(Type + 0.5);

    vec2 sceneUv = gl_FragCoord.xy / vec2(textureSize(Sampler1, 0));
    vec3 sceneBase = texture(Sampler1, sceneUv).rgb;

    vec3 col;
    if (type == 1) {
        // Glass: сквозь модель видно тонированный мир позади (без сущностей);
        // нормали дают преломление (Scale = сила), кромка — белый френель + спекуляр.
        vec2 bend = n.xy * 0.018 * Scale;
        vec3 scene = texture(Sampler1, clamp(sceneUv + bend, 0.0, 1.0)).rgb;
        vec3 r = reflect(-v, n);
        float spec = pow(max(dot(r, normalize(vec3(0.35, 0.75, 0.55))), 0.0), 48.0);
        col = scene * mix(vec3(1.0), vertexColor.rgb, 0.75);
        col += vec3(fresnel * 0.35 + spec * 0.5);
    } else if (type == 2) {
        // Blur: морозное стекло — модель прозрачна, а то, что ЗА ней, заранее
        // размыто dual-filter цепочкой в Sampler2 (Scale = сила, бандинга нет).
        vec3 frosted = texture(Sampler2, sceneUv).rgb;
        col = frosted * mix(vec3(1.0), vertexColor.rgb, 0.35);
        col += vec3(fresnel * 0.15);
    } else if (type == 3) {
        // Cosmic: объёмная туманность в ЛОКАЛЬНЫХ координатах сущности — узор у
        // каждого моба свой и едет вместе с ним, камера на него не влияет.
        // Проекция по доминантной оси нормали грани (модели MC — боксы, швы
        // прячутся на рёбрах). Domain-warp fbm даёт дымные вихри; внутренний слой
        // чуть сдвигается по взгляду (параллакс) — псевдо-глубина вместо «обоев».
        vec3 fn = abs(normalize(cross(dFdx(localPos), dFdy(localPos))));
        bool topFace = fn.y > max(fn.x, fn.z);
        vec2 p = topFace ? localPos.xz : (fn.x > fn.z ? localPos.zy : localPos.xy);
        p *= 2.0 * Scale;

        vec2 drift = vec2(t * 0.12, -t * 0.08);
        vec2 warp = vec2(fbm(p * 1.4 + drift * 2.0), fbm(p * 1.4 - drift)) * 1.6;
        float nearLayer = fbm(p + warp + drift);
        vec2 par = (topFace ? v.xz : (fn.x > fn.z ? v.zy : v.xy)) * 0.35;
        float deepLayer = fbm(p * 0.55 + warp * 0.5 - par + drift * 0.6);
        float density = pow(clamp(nearLayer * 0.65 + deepLayer * 0.55, 0.0, 1.0), 1.7);

        vec3 deep = vec3(0.02, 0.01, 0.10);
        vec3 dusk = vec3(0.16, 0.05, 0.42);
        vec3 rose = vec3(0.78, 0.22, 0.62);
        vec3 glow = vec3(0.45, 0.75, 1.00);
        col = mix(deep, dusk, smoothstep(0.05, 0.55, density));
        col = mix(col, rose, smoothstep(0.45, 0.85, density));
        col += glow * pow(max(density - 0.75, 0.0) * 4.0, 2.0) * 0.6;

        col *= 0.45 + 0.55 * lum;
        col = mix(col, col * vertexColor.rgb, 0.45);
        col += vertexColor.rgb * fresnel * 0.5;
    } else {
        // Flat: ровная заливка одним цветом.
        col = vertexColor.rgb;
    }

    // Replace вместо бленда: альфа гасит модель и шейдер одновременно, замешивая
    // пиксель к миру позади сущности (за стеной «позади» = сама стена).
    fragColor = vec4(mix(sceneBase, col, vertexColor.a), 1.0);
}

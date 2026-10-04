#version 330

layout(std140) uniform SadnesSky {
    float Time;
    int Variant;
    float Brightness;
};

in vec3 skyDir;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);
    return mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * valueNoise3(p);
        p *= 2.02;
        a *= 0.5;
    }
    return v;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Звёзды: редкая сетка ячеек по направлению, мерцание во времени
float starField(vec3 d, float density, float twinkle) {
    vec3 g = d * density;
    vec3 id = floor(g);
    float h = hash31(id);
    if (h < 0.95) return 0.0;
    vec3 f = fract(g) - 0.5;
    float core = smoothstep(0.08, 0.0, length(f));
    float tw = 0.55 + 0.45 * sin(Time * twinkle + h * 60.0);
    return core * tw * (h - 0.95) / 0.05;
}

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    // Всё считается ТОЛЬКО от 3D-направления d -> паттерн непрерывен по всей сфере, без швов на гранях/меридиане.
    vec3 d = normalize(skyDir);
    vec3 col = vec3(0.0);

    if (Variant == 1) {
        // Nebula — космические облака со звёздами
        float n = fbm(d * 2.5 + vec3(0.0, 0.0, Time * 0.04));
        float n2 = fbm(d * 5.0 - vec3(Time * 0.03));
        float dens = smoothstep(0.4, 0.95, n * 0.7 + n2 * 0.5);
        float hue = 0.72 + 0.18 * sin(n * 6.0 + Time * 0.2);
        col = hsv2rgb(vec3(hue, 0.65, dens));
        col += vec3(0.01, 0.0, 0.03);
        col += starField(d, 90.0, 4.0);
    } else if (Variant == 3) {
        // Чёрная дыра крупным планом — тень горизонта, яркое фотонное кольцо,
        // широкий наклонённый аккреционный диск над тёмным космо-фоном.
        vec3 bhDir = normalize(vec3(0.0, 0.05, 1.0));
        vec3 up = abs(bhDir.y) < 0.95 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        vec3 tx = normalize(cross(up, bhDir));
        vec3 ty = cross(bhDir, tx);
        vec2 p = vec2(dot(d, tx), dot(d, ty));            // координата на касательной плоскости
        float front = dot(d, bhDir);
        float ang = acos(clamp(front, -1.0, 1.0));        // угловое расстояние до центра, рад
        vec2 dir2 = ang > 1e-4 ? normalize(p) : vec2(1.0, 0.0);
        // p одинаков для направления к дыре и от неё -> диск рисуется дважды; маска полусферы убирает дубль
        float mask = smoothstep(-0.02, 0.10, front);

        float rEH = 0.26;        // радиус тени (горизонт событий)
        float rPhoton = 0.275;   // фотонное кольцо

        // Новый фон: глубокий тёмный космос (фиолет->чёрный), дымка-туманность, редкие резкие звёзды
        float depth = clamp(d.y * 0.5 + 0.5, 0.0, 1.0);
        vec3 bg = mix(vec3(0.020, 0.016, 0.045), vec3(0.004, 0.005, 0.016), depth);
        float haze = fbm(d * 1.7 + vec3(Time * 0.02));
        bg += vec3(0.06, 0.025, 0.08) * smoothstep(0.55, 1.0, haze) * 0.6;
        // линза: тянем звёзды к центру у дыры -> звёздное кольцо вокруг тени
        vec3 ls = normalize(d + bhDir * (0.10 / (ang + 0.12)) * mask);
        bg += starField(ls, 70.0, 3.0) * 0.9;
        bg += starField(ls, 30.0, 1.2) * 0.5;
        col = bg;

        // Широкое тёплое гало
        col += vec3(1.0, 0.45, 0.18) * exp(-pow((ang - rPhoton) * 1.5, 2.0)) * 0.16 * mask;

        // Аккреционный диск: вращается с дифференциальным сдвигом (внутри быстрее), два слоя турбулентности = объём
        vec2 pd = vec2(p.x, p.y / 0.33);                      // координаты в плоскости наклонённого диска
        float diskR = length(pd);
        float spin = Time * (0.22 + 0.18 / max(diskR, 0.2));  // кеплеровский сдвиг -> закрутка в спираль
        float cs = cos(spin), sn = sin(spin);
        vec2 ps = vec2(pd.x * cs - pd.y * sn, pd.x * sn + pd.y * cs);
        float t1 = fbm(vec3(ps * 5.0, Time * 0.25));
        float t2 = fbm(vec3(ps * 10.0, -Time * 0.5));         // встречный слой мельче -> глубина
        float churn = clamp(0.25 + 0.6 * t1 + 0.35 * t2, 0.0, 1.4);
        float pulse = 0.9 + 0.1 * sin(Time * 0.7);            // лёгкое «дыхание» яркости
        vec3 diskCol = mix(vec3(1.0, 0.78, 0.48), vec3(1.0, 0.42, 0.14), smoothstep(0.28, 0.72, diskR));
        diskCol = mix(diskCol, vec3(1.0, 0.96, 0.82), smoothstep(0.8, 1.2, churn) * 0.5); // горячие сгустки белее
        float diskBand = exp(-pow((diskR - 0.52) * 2.4, 2.0));
        float innerGlow = exp(-pow((diskR - 0.34) * 5.0, 2.0));   // горячий внутренний край -> объём
        col += diskCol * diskBand * churn * 1.1 * pulse * mask;
        col += vec3(1.0, 0.9, 0.72) * innerGlow * churn * 0.7 * pulse * mask;
        // Линзированная дальняя кромка — дуга над тенью
        float arc = exp(-pow((ang - rPhoton - 0.04) * 7.0, 2.0)) * smoothstep(0.0, 0.5, dir2.y) * mask;
        col += diskCol * arc * churn * 0.5;

        // Фотонное кольцо — ровная яркая окружность у тени
        float ring = exp(-pow((ang - rPhoton) / 0.013, 2.0)) * mask;
        col += vec3(1.0, 0.83, 0.62) * ring * 2.2 * pulse * (1.0 + 0.2 * (1.0 - smoothstep(-0.8, 0.2, dir2.y)));

        // Тень горизонта в самом конце — гасит ВСЮ эмиссию внутри -> ровный чёрный диск без швов
        col *= smoothstep(rEH - 0.012, rEH + 0.012, ang);
    } else {
        // Starfield — звёздное небо; млечный путь = полоса у наклонённой плоскости (по dot(d,axis), seamless)
        vec3 axis = normalize(vec3(0.25, 1.0, 0.4));
        float lat = dot(d, axis);
        float band = exp(-pow(lat * 2.6, 2.0));
        float milky = band * (0.4 + 0.6 * fbm(d * 3.0 + vec3(Time * 0.02)));
        float hue = 0.6 + 0.08 * fbm(d * 1.5);
        col = hsv2rgb(vec3(hue, 0.45, milky * 0.5));
        col += vec3(0.012, 0.012, 0.03);
        col += starField(d, 120.0, 5.0) * 1.2;
        col += starField(d, 55.0, 2.0) * 0.7;
    }

    col = max(col, vec3(0.0));
    fragColor = vec4(col * max(Brightness, 0.0), 1.0);
}

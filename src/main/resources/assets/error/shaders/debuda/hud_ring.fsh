#version 150

// self-contained (без #moj_import — include ломался на части GPU)

in vec2 FragCoord; // нормализованная координата фрагмента внутри примитива
in vec4 FragColor; // глобальный множитель (тинт/альфа)

uniform vec2 Size;       // размер прямоугольника
uniform vec4 Radius;     // Radius.x = показатель суперэллипса N (>=2); форма — сквиркл
uniform float Thickness; // >0 — толщина кольца; <0 — сплошная заливка сквиркла
uniform float Progress;  // доля заполнения 0..1
uniform vec4 Color;      // цвет прогресса / заливки
uniform vec4 TrackColor; // цвет трека под прогрессом
uniform float Aa;        // ширина AA-кромки в GUI-px (= 1/guiScale ≈ fwidth) — без производных,
                         // грузится/рисуется на любой видюхе (fwidth на слабых GPU врёт → кольцо пропадало)

out vec4 OutColor;

const float PI = 3.14159265;

void main() {
    // суперэллипс (сквиркл): |x|^N + |y|^N = A^N, A — полразмера; N берём из Radius.x
    float N = max(Radius.x, 2.0);
    vec2 center = Size * 0.5;
    vec2 p = FragCoord * Size - center;
    float A = center.x - 1.0;
    vec2 ap = abs(p);
    float se = pow(pow(ap.x, N) + pow(ap.y, N), 1.0 / N);
    // нормировка на модуль градиента → равномерная толщина кольца (иначе углы толще граней)
    vec2 g = vec2(pow(ap.x, N - 1.0), pow(ap.y, N - 1.0));
    float glen = max(pow(max(se, 1e-4), 1.0 - N) * length(g), 1e-4);
    float dist = (se - A) / glen;               // ~истинный signed distance до контура
    float aa = max(Aa, 0.0001);                 // derivative-free (было fwidth(dist))

    // режим сплошной заливки (для обложки/аватара-подложки)
    if (Thickness < 0.0) {
        float fill = 1.0 - smoothstep(-aa, aa, dist);
        if (fill <= 0.0) discard;
        OutColor = vec4(Color.rgb, Color.a * fill) * FragColor;
        if (OutColor.a <= 0.0) discard;
        return;
    }

    // кольцо-обводка: |dist| <= Thickness/2, сглаживание кромки через Aa (без fwidth)
    float halfT = Thickness * 0.5;
    float ring = 1.0 - smoothstep(halfT - aa * 0.5, halfT + aa * 0.5, abs(dist));
    if (ring <= 0.0) {
        discard;
    }

    // угол против часовой от верх-центра (заполнение справа-налево), нормализованный 0..1
    vec2 rel = FragCoord - vec2(0.5);
    float ang = atan(-rel.x, -rel.y);
    float frac = ang / (2.0 * PI);
    if (frac < 0.0) frac += 1.0;

    // прогресс со скруглёнными торцами (капсула), чтобы концы дуги не были «острыми углами»
    float R = A;
    float lead = (frac - Progress) * 2.0 * PI * R;    // >0 — в треке за передним торцом
    float startBack = (1.0 - frac) * 2.0 * PI * R;    // назад от старта (frac=0)
    float mLead = 1.0 - smoothstep(halfT - 1.0, halfT, lead);
    float mStart = (Progress < 1.0) ? (1.0 - smoothstep(halfT - 1.0, halfT, startBack)) : 1.0;
    float m = (frac <= Progress) ? 1.0 : max(mLead, mStart);

    vec4 col = mix(TrackColor, Color, m);
    OutColor = vec4(col.rgb, col.a * ring) * FragColor;
    if (OutColor.a <= 0.0) {
        discard;
    }
}

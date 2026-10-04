#version 330

uniform sampler2D Sampler0; // захват руки с предметом: rgb = цвет модели, a = покрытие (прямая альфа)

layout(std140) uniform SadnesWarp {
    float Amplitude; // макс. сдвиг сэмпла по X в UV-долях экрана
    float Waves;     // число волн вдоль клинка (по вертикали кадра)
    float Phase;     // фаза бегущей волны (анимация)
    float Envelope;  // 0..1 сила изгиба (0 в покое -> меч прямой)
};

in vec2 texCoord0;
in vec4 tint; // не используется (белый) — цвет несёт сам захват

out vec4 fragColor;

const float TAU = 6.2831853;

// S-изгиб: горизонтальный сдвиг сэмпла зависит от высоты пикселя — разные участки клинка
// уезжают в разные стороны, складываясь в волну. Envelope гасит эффект в покое.
void main() {
    float raw = sin(texCoord0.y * Waves * TAU + Phase);
    // «Больше вверх, чем вниз»: верхнюю полуволну (raw>0) держим полной, нижнюю поджимаем до 40% —
    // клинок уходит вверх сильнее. Если направление перепутано — заменить raw > 0.0 на raw < 0.0.
    float wave = raw > 0.0 ? raw : raw * 0.4;
    float bend = wave * Amplitude * Envelope;
    vec2 uv = vec2(texCoord0.x - bend, texCoord0.y);
    if (uv.x < 0.0 || uv.x > 1.0) discard; // за кромкой кадра не тянем крайний пиксель
    vec4 hand = texture(Sampler0, uv);
    if (hand.a <= 0.0005) discard;         // вне силуэта руки — пропускаем пиксель кадра
    fragColor = hand;                      // прямая альфа -> TRANSLUCENT-бленд из pipeline
}

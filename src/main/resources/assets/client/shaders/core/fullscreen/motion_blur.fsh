#version 330

uniform sampler2D Sampler0;

layout(std140) uniform SadnesMotionBlur {
    vec4 Velocity; // xy = вектор следа в framebuffer-UV, zw = паддинг
};

in vec2 texCoord0;

out vec4 fragColor;

// Направленный блюр по дельте поворота камеры: тапы идут вдоль вектора скорости,
// центрированно (-0.5..0.5), поэтому изображение не сдвигается — только размазывается.
// При нулевой скорости все тапы совпадают -> точная копия кадра.
const int SAMPLES = 32;

// Дешёвый псевдослучайный дизеринг старта: размывает бандинг в тонкий шум, чтобы
// длинный след читался как непрерывная смазь, а не набор призрачных копий кадра.
float dither(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 vel = Velocity.xy;

    // Сдвиг старта на долю шага по позиции пикселя — заполняет промежутки между тапами.
    float jitter = (dither(gl_FragCoord.xy) - 0.5) / float(SAMPLES - 1);

    vec3 sum = vec3(0.0);
    for (int i = 0; i < SAMPLES; i++) {
        float t = float(i) / float(SAMPLES - 1) - 0.5 + jitter;
        vec2 uv = clamp(texCoord0 + vel * t, 0.0, 1.0);
        sum += texture(Sampler0, uv).rgb;
    }

    fragColor = vec4(sum / float(SAMPLES), 1.0);
}

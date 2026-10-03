#version 150

// Эффект «пар от рук» (тумблер «Пар»). Маска силуэта — по глубине (Sampler0 =
// depth-attachment). Рука — эмиттер: близость к силуэту радиально в пределах
// Radius (мягкое гало), по которому пускается анимированный fBm-дым, тянущий вверх.

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform float Time;
uniform vec2 Resolution;
uniform vec2 Texel;       // 1.0 / resolution
uniform float Density;    // плотность дыма
uniform float Speed;      // скорость подъёма
uniform float Radius;     // радиус эмиссии вокруг руки/предмета (px)
uniform vec3 SteamColor;
uniform float Opacity;

out vec4 OutColor;

float hmask(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return step(0.01, max(c.a, max(c.r, max(c.g, c.b))));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.0 + vec2(19.0);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = TexCoord;
    float here = hmask(uv);

    float probe = here;
    probe = max(probe, hmask(uv + vec2(0.0,  Radius * 1.6 * Texel.y)));
    probe = max(probe, hmask(uv + vec2(0.0, -Radius * Texel.y)));
    probe = max(probe, hmask(uv + vec2( Radius * Texel.x, 0.0)));
    probe = max(probe, hmask(uv + vec2(-Radius * Texel.x, 0.0)));
    probe = max(probe, hmask(uv + vec2( Radius * 0.7 * Texel.x,  Radius * 0.9 * Texel.y)));
    probe = max(probe, hmask(uv + vec2(-Radius * 0.7 * Texel.x,  Radius * 0.9 * Texel.y)));
    if (probe <= 0.004) discard;

    float emit = 0.0;
    const int DIRS = 16;
    const int RINGS = 6;
    for (int i = 0; i < DIRS; i++) {
        float ang = 6.2831 * float(i) / float(DIRS);
        vec2 dir = vec2(cos(ang), sin(ang));
        float rad = Radius * (1.0 + 0.6 * max(dir.y, 0.0));
        for (int r = 1; r <= RINGS; r++) {
            float dist = float(r) / float(RINGS);
            float a = hmask(uv + dir * Texel * rad * dist);
            float fall = 1.0 - dist;
            emit += a * fall * fall;
        }
    }
    emit /= float(DIRS) * 0.5;
    emit = clamp(emit, 0.0, 1.0);
    emit = smoothstep(0.0, 1.0, emit);

    emit *= (1.0 - here);
    if (emit <= 0.005) discard;

    vec2 sp = uv * vec2(Resolution.x / Resolution.y, 1.0) * 3.0;
    float n = fbm(sp + vec2(0.0, -Time * Speed));
    n = fbm(sp + vec2(n * 0.6, -Time * Speed * 1.2));

    float cloud = smoothstep(0.15, 0.95, n);
    cloud = cloud * cloud;
    float smoke = cloud * Density * emit;
    OutColor = vec4(SteamColor, clamp(smoke * Opacity, 0.0, 1.0));
}

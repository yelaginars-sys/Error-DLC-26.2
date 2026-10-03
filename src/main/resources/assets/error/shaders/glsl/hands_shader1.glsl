#version 120

// by niorze — deep cosmos nebula overlay
// screen-space pattern → continuous over any shape/face; bright cores; star field
// optimised: ~3-4× fewer noise ops than the original, identical visual style

uniform sampler2D originalTexture;
uniform float time;
uniform float alpha;
uniform float speed;
uniform vec3  accent;
uniform vec2  screenSize;

// ─────────────── hash & noise ───────────────
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i),                  hash21(i + vec2(1.0, 0.0)), u.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

// 4-octave fbm (was 7) — still rich enough for screen-size visuals
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.07 + vec2(2.1, 5.3);
        a *= 0.5;
    }
    return v;
}

// 3-octave ridged fbm (was 6) — used only as a subtle filament overlay
float fbmRidged(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        float n = vnoise(p);
        n = 1.0 - abs(n * 2.0 - 1.0);
        n *= n;
        v += a * n;
        p = p * 2.05 + vec2(3.7, 8.1);
        a *= 0.5;
    }
    return v;
}

// 2-stage domain warp (was 3-stage) — keeps the swirling look at half the cost
float nebulaDensity(vec2 p, float t) {
    vec2 a = vec2(
        fbm(p + vec2(0.0,  t * 0.12)),
        fbm(p + vec2(5.2, -t * 0.10))
    );
    vec2 b = vec2(
        fbm(p + 3.0 * a + vec2(1.7, 9.2)),
        fbm(p + 3.0 * a + vec2(8.3, 2.8))
    );
    return fbm(p + 2.2 * b);
}

// nebula palette — fully derived from the accent (theme) colour
vec3 nebulaPalette(float t, vec3 acc) {
    float m = max(max(acc.r, acc.g), max(acc.b, 0.001));
    vec3 base = acc / m;
    vec3 deep = base * vec3(0.45, 0.45, 0.70);

    vec3 c1 = deep * 0.020;
    vec3 c2 = base * 0.110;
    vec3 c3 = base * 0.380;
    vec3 c4 = base * 0.900;
    vec3 c5 = mix(base, vec3(1.0), 0.55);
    vec3 c6 = vec3(1.0);

    t = clamp(t, 0.0, 1.0);
    if (t < 0.18) return mix(c1, c2, t / 0.18);
    if (t < 0.38) return mix(c2, c3, (t - 0.18) / 0.20);
    if (t < 0.60) return mix(c3, c4, (t - 0.38) / 0.22);
    if (t < 0.82) return mix(c4, c5, (t - 0.60) / 0.22);
    return mix(c5, c6, (t - 0.82) / 0.18);
}

float starShape(vec2 sf, float h) {
    vec2 d = abs(sf - 0.5) * 2.0;
    float dist = length(d);
    float core  = pow(max(0.0, 1.0 - smoothstep(0.0, 0.22, dist)), 1.5);
    float flrX  = pow(max(0.0, 1.0 - d.x * 6.0), 4.0) * smoothstep(0.65, 0.0, d.y);
    float flrY  = pow(max(0.0, 1.0 - d.y * 6.0), 4.0) * smoothstep(0.65, 0.0, d.x);
    float size  = 0.6 + 0.4 * h;
    return (core + (flrX + flrY) * 0.55) * size;
}

float starField(vec2 uv, float density, float t) {
    vec2 g = floor(uv);
    vec2 f = fract(uv);
    float h = hash21(g);
    if (h < density) return 0.0;
    vec2 offs = vec2(hash21(g + 1.7), hash21(g + 3.3));
    float s   = starShape(f - offs * 0.3 - 0.35 + 0.5, h);
    float tw  = 0.30 + 0.70 * sin(t * 2.6 + h * 47.0);
    float w   = smoothstep(density, 1.0, h);
    return s * max(tw, 0.0) * w;
}

void main() {
    vec2 uv  = gl_TexCoord[0].xy;
    vec4 src = texture2D(originalTexture, uv);

    if (src.a < 0.01) {
        gl_FragColor = src;
        return;
    }

    // screen-space pattern with aspect correction
    vec2 sRes = max(screenSize, vec2(1.0));
    vec2 sUv  = gl_FragCoord.xy / sRes;
    sUv.x *= sRes.x / sRes.y;

    float t = time * speed;

    // two layered nebulae — the variety is what sells the look
    vec2 p = sUv * 4.5;
    float n1 = nebulaDensity(p,                t      );
    float n2 = nebulaDensity(p * 1.8 + 11.3,  -t * 0.6);
    float n  = mix(n1, n2, 0.45);

    // subtle ridged filaments
    float ridge = fbmRidged(sUv * 7.0 + vec2(t * 0.10, -t * 0.07));
    n = mix(n, n * 0.55 + ridge * 0.6, 0.40);

    // sharper density curve
    float d = smoothstep(0.18, 0.95, n);
    d = pow(d, 1.05);
    float voids = smoothstep(0.35, 0.05, n);
    d *= (1.0 - voids * 0.85);

    // palette
    vec3 col = nebulaPalette(d, accent);

    // saturation boost (luminance-preserving)
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.25);

    // bright filament glow
    float fil = smoothstep(0.58, 0.92, d);
    col += nebulaPalette(min(1.0, d + 0.10), accent) * fil * 0.55;

    // white-hot core flashes
    float hot = smoothstep(0.85, 0.99, d);
    col += vec3(1.0) * hot * 0.7;

    // ─── star fields — three parallax layers ───
    float maxA   = max(max(accent.r, accent.g), max(accent.b, 0.001));
    vec3  accN   = accent / maxA;
    vec3  starT  = mix(vec3(1.0), accN, 0.12);
    float stars  = 0.0;
    stars += starField(sUv *  90.0,             0.955, t      ) * 1.4;
    stars += starField(sUv *  42.0 +  7.0,      0.975, t * 0.7) * 2.4;
    stars += starField(sUv *  18.0 + 19.4,      0.992, t * 0.5) * 3.5;
    col   += starT * stars;

    // drifting cosmic dust — single cheap fbm, accent-tinted
    float dust = fbm(sUv * 12.0 + t * 0.40);
    vec3  dustT = mix(accN, vec3(1.0), 0.40);
    col += dustT * smoothstep(0.50, 0.85, dust) * 0.12;

    // gentle pulse
    col *= 0.94 + 0.06 * sin(t * 1.3 + n * 5.0);

    // Reinhard tonemap
    col = col / (1.0 + col * 0.22);
    col *= 1.15;

    // 1-texel accent rim — keeps silhouette readable
    float sStep = 1.0 / 16.0;
    float minA = 1.0;
    minA = min(minA, texture2D(originalTexture, uv + vec2( sStep, 0.0)).a);
    minA = min(minA, texture2D(originalTexture, uv + vec2(-sStep, 0.0)).a);
    minA = min(minA, texture2D(originalTexture, uv + vec2(0.0,  sStep)).a);
    minA = min(minA, texture2D(originalTexture, uv + vec2(0.0, -sStep)).a);
    float rim = (1.0 - smoothstep(0.0, 0.7, minA));
    col += accent * rim * 0.55;

    // final blend
    vec3 finalRGB = mix(src.rgb, col, alpha);
    gl_FragColor = vec4(clamp(finalRGB, 0.0, 1.0), src.a);
}

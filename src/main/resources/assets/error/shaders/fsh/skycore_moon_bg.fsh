#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 size;
uniform float time;
uniform float alpha;
uniform vec3 themeColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 74.7);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = m * p + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

// Soft vertical light pillar from top-center (Moon god rays)
float shaft(vec2 p, float ang, float sharp) {
    float c = cos(ang);
    float s = sin(ang);
    // Origin near top of screen
    float lat = abs(p.x * s - (p.y - 0.78) * c);
    float fall = smoothstep(1.35, -0.35, p.y);
    return exp(-lat * sharp) * fall;
}

void main() {
    vec2 uv = texCoord;
    vec2 p = (uv - 0.5) * vec2(size.x / max(size.y, 1.0), 1.0);
    float t = time * 0.18;
    vec3 theme = max(themeColor, vec3(0.08));

    // Strong central ray + side beams
    float rays = 0.0;
    rays += shaft(p, 0.00 + 0.025 * sin(t * 0.7), 5.2) * 1.55;
    rays += shaft(p, 0.085 + 0.02 * cos(t * 0.45), 9.5) * 0.75;
    rays += shaft(p, -0.09 + 0.02 * sin(t * 0.55), 9.5) * 0.70;
    rays += shaft(p, 0.17, 15.0) * 0.38;
    rays += shaft(p, -0.18, 15.0) * 0.36;
    rays *= 0.82 + 0.18 * sin(t * 1.2 + p.y * 6.0);

    float smoke = fbm(p * 1.15 + vec2(t * 0.08, -t * 0.05));
    smoke = mix(smoke, fbm(p * 2.4 + vec2(-t * 0.06, t * 0.07)), 0.35);
    float smokeMask = smoothstep(-0.05, 1.0, uv.y) * 0.85;
    smokeMask += smoothstep(0.2, 1.1, abs(p.x)) * 0.35;

    // Subtle film grain / dither dots (Moon texture)
    float cell = 9.0;
    vec2 gF = fract((uv * size) / cell) - 0.5;
    float dots = 1.0 - smoothstep(0.08, 0.26, length(gF));
    float grid = dots * smoothstep(1.4, 0.2, length(p * vec2(0.9, 1.1))) * 0.07;

    float bloom = exp(-length(vec2(p.x * 1.8, p.y - 0.62)) * 1.7) * 0.42;
    float lift = exp(-length(p * vec2(1.0, 1.25)) * 1.4) * 0.08;

    // Transparent darken so panorama shows through
    float edgeVig = 1.0 - smoothstep(0.35, 1.35, length((uv - 0.5) * vec2(1.25, 1.05)));
    float baseDark = mix(0.55, 0.18, edgeVig); // darker edges, clearer center

    vec3 color = vec3(0.0);
    color += vec3(0.85, 0.87, 0.92) * rays * 1.05;
    color += theme * rays * 0.10;
    color += vec3(0.70, 0.72, 0.78) * bloom;
    color += vec3(0.12, 0.13, 0.15) * smoke * smokeMask * 0.55;
    color += vec3(grid);
    color += vec3(lift);

    float grain = (hash21(uv * size + vec2(time * 50.0, time * 21.0)) - 0.5) * 0.022;
    color += grain;

    // Alpha: let landscape read clearly; rays add light on top
    float cover = mix(baseDark, 0.12, clamp(rays * 0.9 + bloom * 0.55, 0.0, 1.0));
    // Soft black fog layer in alpha channel via premultiplied-ish mix:
    // we output dark color with moderate alpha in shadows, bright with low alpha on rays
    vec3 fog = vec3(0.02, 0.02, 0.025) * (1.0 - rays * 0.65);
    color = mix(fog, color, clamp(0.25 + rays * 0.9 + bloom * 0.5, 0.0, 1.0));

    fragColor = vec4(max(color, 0.0), clamp(cover * alpha, 0.0, 0.92));
}

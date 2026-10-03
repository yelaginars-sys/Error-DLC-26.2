#version 330
precision mediump float;

uniform sampler2D Sampler0;

layout(std140) uniform EffectData {
    vec4 uColor;
    vec4 uColor2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixelData;
};

in vec2 TexCoord;
out vec4 OutColor;

#define uTime screen.z
#define uRes screen.xy
#define alpha screen.w
#define colorMode params1.z
vec3 color;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

#define OCTAVES 5
float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(st);
        st *= 2.1;
        amplitude *= 0.45;
    }
    return value;
}

void main() {
    vec4 s0 = texture(Sampler0, TexCoord);
    float mask = s0.a;
    if (mask < 0.01) discard;

    vec3 rawColor = mix(uColor.rgb, s0.rgb, step(0.5, colorMode));
    float luma = dot(rawColor, vec3(0.299, 0.587, 0.114));
    color = mix(rawColor, clamp(luma + (rawColor - luma) * 6.0, 0.0, 1.0), step(0.5, colorMode));

    vec2 uv = (gl_FragCoord.xy * 2.0 - uRes.xy) / min(uRes.x, uRes.y);

    float t = uTime * 0.35;

    vec2 q = vec2(fbm(uv * 1.2 + vec2(t * 0.2, -t * 0.3)), fbm(uv * 1.2 + vec2(1.2)));
    vec2 r = vec2(fbm(uv + q * 1.5 + vec2(t * 0.3)), fbm(uv + q * 1.5 + vec2(-t * 0.2)));
    float n = fbm(uv * 0.8 + r * 1.2);

    float pulse = sin(t * 1.5) * 0.5 + 0.5;

    float lines = sin((n * 9.0) + (t * 1.5));
    float contour = smoothstep(0.2, 0.8, abs(lines));
    contour = 1.0 - contour;
    contour = pow(contour, 3.0) * (0.8 + pulse * 0.4);

    vec2 grid_uv = gl_FragCoord.xy * 0.18;
    vec2 fpos = fract(grid_uv) - 0.5;

    float sparkle = random(floor(grid_uv) + floor(uTime * 5.0));
    float dot_size = contour * 0.55 * (0.8 + sparkle * 0.2);
    float dist = length(fpos);
    float dot_mask = 1.0 - smoothstep(dot_size - 0.07, dot_size + 0.07, dist);

    float core_glow = contour * (1.5 + sin(t * 4.0 + n * 6.0) * 0.5);
    vec3 spark_color = color * core_glow;

    vec3 outColor = vec3(0.0);
    outColor += vec3(dot_mask) * spark_color;
    outColor += vec3(contour * 0.08);

    OutColor = vec4(outColor, mask * alpha);
}

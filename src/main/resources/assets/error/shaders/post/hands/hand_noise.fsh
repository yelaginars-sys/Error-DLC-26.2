#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

float voronoi(vec2 x, float time) {
    vec2 n = floor(x);
    vec2 f = fract(x);
    float minDist = 8.0;

    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = hash2(n + g);
            o = 0.5 + 0.41 * sin(time + 6.2831 * o);
            vec2 r = g - f + o;
            float d = dot(r, r);
            if (d < minDist) {
                minDist = d;
            }
        }
    }
    return sqrt(minDist);
}

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }

    float time = FillParams.x;
    vec2 st = uv * 14.0;

    float v1 = voronoi(st, time * 0.8);
    float v2 = voronoi(st * 2.0 + vec2(time * 0.3, -time * 0.4), time * 1.2);

    float pattern = mix(v1, v2, 0.4);
    float border = smoothstep(0.05, 0.35, pattern);

    vec3 col = mix(FillColor.rgb * 1.6, FillColor.rgb * 0.25, border);
    col += FillColor.rgb * pow(1.0 - pattern, 3.0) * 1.5;

    finalColor = vec4(clamp(col, 0.0, 1.0), clamp(mask * FillColor.a, 0.0, 1.0));
}
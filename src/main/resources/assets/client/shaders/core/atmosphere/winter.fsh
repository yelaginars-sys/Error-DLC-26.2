#version 330 core

uniform sampler2D Sampler0;

layout(std140) uniform AtmosphereData {
    float u_Time;
    float u_Mode;
    float u_Intensity;
    float u_Vignette;
    float u_Bloom;
    float u_Width;
    float u_Height;
    float u_Particles;
};

in vec2 texCoord0;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
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

void main() {
    vec4 scene = texture(Sampler0, texCoord0);
    vec3 col = scene.rgb;
    float intensity = clamp(u_Intensity, 0.0, 2.0);

    vec3 coldTint = vec3(0.88, 0.95, 1.08);
    col *= coldTint;

    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(lum);
    col = mix(col, gray, 0.15 * intensity);

    float snowHighlight = smoothstep(0.55, 0.95, lum);
    col += vec3(0.12, 0.18, 0.25) * snowHighlight * intensity;

    if (u_Vignette > 0.5) {
        float dist = distance(texCoord0, vec2(0.5));
        float vigEdge = smoothstep(0.35, 0.75, dist);

        vec2 frostUV = (texCoord0 - 0.5) * vec2(u_Width / max(u_Height, 1.0), 1.0) * 12.0;
        float n = noise(frostUV * 2.0 + u_Time * 0.02) * 0.6 + noise(frostUV * 5.0) * 0.4;
        float frost = vigEdge * (0.5 + 0.5 * n);

        vec3 frostColor = vec3(0.82, 0.92, 1.0);
        col = mix(col, frostColor, frost * 0.55 * intensity);
    }

    if (u_Particles > 0.5) {
        vec2 pUv = texCoord0 * vec2(u_Width / max(u_Height, 1.0), 1.0) * 35.0;
        pUv.y += u_Time * 1.2;
        pUv.x += sin(u_Time * 0.8 + pUv.y * 0.3) * 0.4;
        vec2 pGrid = floor(pUv);
        float h = hash(pGrid);
        if (h > 0.96) {
            vec2 pInCell = fract(pUv) - 0.5;
            float spark = 1.0 - smoothstep(0.0, 0.18, length(pInCell));
            spark *= (0.6 + 0.4 * sin(u_Time * 6.0 + h * 6.28));
            col += vec3(0.9, 0.96, 1.0) * spark * 0.8 * intensity;
        }
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), scene.a);
}

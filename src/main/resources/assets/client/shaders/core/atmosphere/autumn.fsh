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

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 sampleBloom(vec2 uv, vec2 texelSize) {
    vec3 col = vec3(0.0);
    float totalWeight = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize * 2.5;
            vec3 s = texture(Sampler0, uv + offset).rgb;
            float lum = dot(s, vec3(0.299, 0.587, 0.114));
            float weight = max(0.0, lum - 0.45) + 0.1;
            col += s * weight;
            totalWeight += weight;
        }
    }
    return col / max(totalWeight, 0.001);
}

void main() {
    vec4 scene = texture(Sampler0, texCoord0);
    vec3 col = scene.rgb;
    float intensity = clamp(u_Intensity, 0.0, 2.0);

    vec3 hsv = rgb2hsv(col);

    if (hsv.x > 0.18 && hsv.x < 0.45 && hsv.y > 0.15) {
        float t = (hsv.x - 0.18) / (0.45 - 0.18);
        float autumnHue = mix(0.07, 0.11, t);
        hsv.x = mix(hsv.x, autumnHue, 0.85 * intensity);
        hsv.y = min(1.0, hsv.y * (1.0 + 0.3 * intensity));
        hsv.z = min(1.0, hsv.z * 1.05);
    }

    col = hsv2rgb(hsv);

    vec3 autumnGrading = vec3(1.10, 0.95, 0.80);
    col *= autumnGrading;

    if (u_Bloom > 0.5) {
        vec2 texelSize = vec2(1.0 / max(u_Width, 1.0), 1.0 / max(u_Height, 1.0));
        vec3 bloom = sampleBloom(texCoord0, texelSize);
        col += bloom * vec3(1.1, 0.8, 0.4) * 0.3 * intensity;
    }

    if (u_Vignette > 0.5) {
        float dist = distance(texCoord0, vec2(0.5));
        float vig = smoothstep(0.4, 0.85, dist);
        vec3 autumnVigColor = vec3(0.5, 0.25, 0.1) * col;
        col = mix(col, autumnVigColor, vig * 0.45 * intensity);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), scene.a);
}

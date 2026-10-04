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
    if (hsv.x > 0.20 && hsv.x < 0.44 && hsv.y > 0.10) {
        hsv.y = min(1.0, hsv.y * (1.0 + 0.30 * intensity));
        hsv.z = min(1.0, hsv.z * 1.12);
    }
    col = hsv2rgb(hsv);

    vec2 aspect = vec2(u_Width / max(u_Height, 1.0), 1.0);
    float perspective = 1.0 / max(0.12, (1.0 - texCoord0.y) * 2.0 + 0.15);
    vec2 groundUV = (texCoord0 - vec2(0.5, 0.5)) * aspect * perspective * 4.0;

    float n1 = noise(groundUV * 1.3);
    float n2 = noise(groundUV * 3.0 + vec2(4.7, 2.1));
    float puddleNoise = n1 * 0.65 + n2 * 0.35;

    float groundMask = smoothstep(0.30, 0.70, texCoord0.y);
    float puddleMask = smoothstep(0.46, 0.62, puddleNoise) * groundMask;

    float rippleTotal = 0.0;
    if (puddleMask > 0.01) {
        vec2 ripUV = groundUV * 3.5;
        vec2 ripCell = floor(ripUV);
        vec2 ripFrac = fract(ripUV) - 0.5;

        float t = u_Time * 4.2;
        float h = hash(ripCell);
        float phase = fract(h + t * 0.5);
        float d = length(ripFrac);
        float wave = sin((d - phase * 0.6) * 32.0) * smoothstep(0.5, 0.0, d) * (1.0 - phase);
        rippleTotal = wave * puddleMask;
    }

    vec2 reflOffset = vec2(rippleTotal * 0.025, (0.10 + rippleTotal * 0.02) * (1.0 - texCoord0.y * 0.45));
    vec2 reflUV = texCoord0 + vec2(reflOffset.x, -reflOffset.y);
    reflUV = clamp(reflUV, vec2(0.002), vec2(0.998));

    vec3 reflectedScene = texture(Sampler0, reflUV).rgb;
    vec3 skyRefl = vec3(0.68, 0.80, 0.96);
    vec3 mirrorReflection = mix(reflectedScene, skyRefl, 0.35);

    float fresnel = pow(clamp(1.0 - texCoord0.y, 0.0, 1.0), 1.5) * 0.65 + 0.35;
    float specular = pow(clamp(rippleTotal * 0.85 + 0.55, 0.0, 1.0), 16.0) * 1.8;
    vec3 specColor = vec3(1.0, 1.0, 1.0) * specular * puddleMask;

    vec3 wetBase = col * mix(vec3(1.0), vec3(0.75, 0.78, 0.82), 0.45 * intensity * groundMask);

    col = mix(wetBase, mirrorReflection, puddleMask * fresnel * 0.88 * intensity);
    col += specColor * intensity;

    if (u_Bloom > 0.5) {
        vec2 texelSize = vec2(1.0 / max(u_Width, 1.0), 1.0 / max(u_Height, 1.0));
        vec3 bloom = sampleBloom(texCoord0, texelSize);
        col += bloom * vec3(0.65, 0.82, 1.0) * 0.30 * intensity;
    }

    if (u_Vignette > 0.5) {
        float dist = distance(texCoord0, vec2(0.5));
        float vig = smoothstep(0.40, 0.85, dist);
        col = mix(col, col * vec3(0.92, 0.94, 0.98), vig * 0.35 * intensity);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), scene.a);
}

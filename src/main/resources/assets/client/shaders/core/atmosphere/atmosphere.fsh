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

vec3 applyWinter(vec3 baseColor, vec2 uv, float intensity) {
    vec3 coldTint = vec3(0.90, 0.96, 1.08);
    vec3 col = baseColor * coldTint;

    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(lum);
    col = mix(col, gray, 0.12 * intensity);

    float snowHighlight = smoothstep(0.52, 0.95, lum);
    col += vec3(0.14, 0.20, 0.28) * snowHighlight * intensity;

    if (u_Vignette > 0.5) {
        float dist = distance(uv, vec2(0.5));
        float vigEdge = smoothstep(0.35, 0.75, dist);

        vec2 frostUV = (uv - 0.5) * vec2(u_Width / max(u_Height, 1.0), 1.0) * 12.0;
        float n = noise(frostUV * 2.0 + u_Time * 0.02) * 0.6 + noise(frostUV * 5.0) * 0.4;
        float frost = vigEdge * (0.5 + 0.5 * n);

        vec3 frostColor = vec3(0.82, 0.92, 1.0);
        col = mix(col, frostColor, frost * 0.50 * intensity);
    }

    return col;
}

vec3 applySummer(vec3 baseColor, vec2 uv, float intensity) {
    vec3 hsv = rgb2hsv(baseColor);

    float satBoost = (1.0 - hsv.y) * 0.45 * intensity;
    hsv.y = clamp(hsv.y + satBoost, 0.0, 1.0);

    if (hsv.x > 0.20 && hsv.x < 0.45) {
        hsv.y = min(1.0, hsv.y * (1.0 + 0.35 * intensity));
        hsv.z = min(1.0, hsv.z * (1.0 + 0.15 * intensity));
    }

    vec3 col = hsv2rgb(hsv);

    vec3 sunTint = vec3(1.05, 1.02, 0.94);
    col *= sunTint;

    if (u_Bloom > 0.5) {
        vec2 texelSize = vec2(1.0 / max(u_Width, 1.0), 1.0 / max(u_Height, 1.0));
        vec3 bloom = sampleBloom(uv, texelSize);
        col += bloom * vec3(1.05, 0.95, 0.7) * 0.35 * intensity;
    }

    if (u_Vignette > 0.5) {
        float dist = distance(uv, vec2(0.5));
        float vig = smoothstep(0.4, 0.85, dist);
        col = mix(col, col * vec3(0.95, 0.90, 0.80), vig * 0.35 * intensity);
    }

    return col;
}

vec3 applyAutumn(vec3 baseColor, vec2 uv, float intensity) {
    vec3 hsv = rgb2hsv(baseColor);

    if (hsv.x > 0.18 && hsv.x < 0.45 && hsv.y > 0.15) {
        float t = (hsv.x - 0.18) / (0.45 - 0.18);
        float autumnHue = mix(0.07, 0.11, t);
        hsv.x = mix(hsv.x, autumnHue, 0.85 * intensity);
        hsv.y = min(1.0, hsv.y * (1.0 + 0.3 * intensity));
        hsv.z = min(1.0, hsv.z * 1.05);
    }

    vec3 col = hsv2rgb(hsv);

    vec3 autumnGrading = vec3(1.10, 0.95, 0.80);
    col *= autumnGrading;

    if (u_Bloom > 0.5) {
        vec2 texelSize = vec2(1.0 / max(u_Width, 1.0), 1.0 / max(u_Height, 1.0));
        vec3 bloom = sampleBloom(uv, texelSize);
        col += bloom * vec3(1.1, 0.8, 0.4) * 0.3 * intensity;
    }

    if (u_Vignette > 0.5) {
        float dist = distance(uv, vec2(0.5));
        float vig = smoothstep(0.4, 0.85, dist);
        vec3 autumnVigColor = vec3(0.5, 0.25, 0.1) * col;
        col = mix(col, autumnVigColor, vig * 0.45 * intensity);
    }

    return col;
}

vec3 applySpring(vec3 baseColor, vec2 uv, float intensity) {
    // 1. Wet spring rain color grading
    vec3 hsv = rgb2hsv(baseColor);
    if (hsv.x > 0.20 && hsv.x < 0.44 && hsv.y > 0.10) {
        hsv.y = min(1.0, hsv.y * (1.0 + 0.30 * intensity));
        hsv.z = min(1.0, hsv.z * 1.12);
    }
    vec3 col = hsv2rgb(hsv);

    // 2. Procedural puddles on ground surfaces
    vec2 aspect = vec2(u_Width / max(u_Height, 1.0), 1.0);
    float perspective = 1.0 / max(0.12, (1.0 - uv.y) * 2.0 + 0.15);
    vec2 groundUV = (uv - vec2(0.5, 0.5)) * aspect * perspective * 4.0;

    float n1 = noise(groundUV * 1.3);
    float n2 = noise(groundUV * 3.0 + vec2(4.7, 2.1));
    float puddleNoise = n1 * 0.65 + n2 * 0.35;

    float groundMask = smoothstep(0.30, 0.70, uv.y);
    float puddleMask = smoothstep(0.46, 0.62, puddleNoise) * groundMask;

    // 3. Animated Rain Ripples inside puddles
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

    // 4. Mirror Screen-Space Reflection distorted by ripples
    vec2 reflOffset = vec2(rippleTotal * 0.025, (0.10 + rippleTotal * 0.02) * (1.0 - uv.y * 0.45));
    vec2 reflUV = uv + vec2(reflOffset.x, -reflOffset.y);
    reflUV = clamp(reflUV, vec2(0.002), vec2(0.998));

    vec3 reflectedScene = texture(Sampler0, reflUV).rgb;
    vec3 skyRefl = vec3(0.68, 0.80, 0.96);
    vec3 mirrorReflection = mix(reflectedScene, skyRefl, 0.35);

    // Fresnel reflectance factor
    float fresnel = pow(clamp(1.0 - uv.y, 0.0, 1.0), 1.5) * 0.65 + 0.35;

    // 5. Specular Glistening Highlight on ripples
    float specular = pow(clamp(rippleTotal * 0.85 + 0.55, 0.0, 1.0), 16.0) * 1.8;
    vec3 specColor = vec3(1.0, 1.0, 1.0) * specular * puddleMask;

    // 6. Wet Porous Ground Darkening outside puddles
    vec3 wetBase = col * mix(vec3(1.0), vec3(0.75, 0.78, 0.82), 0.45 * intensity * groundMask);

    col = mix(wetBase, mirrorReflection, puddleMask * fresnel * 0.88 * intensity);
    col += specColor * intensity;

    // 7. Rain Bloom & Wet Vignette
    if (u_Bloom > 0.5) {
        vec2 texelSize = vec2(1.0 / max(u_Width, 1.0), 1.0 / max(u_Height, 1.0));
        vec3 bloom = sampleBloom(uv, texelSize);
        col += bloom * vec3(0.65, 0.82, 1.0) * 0.30 * intensity;
    }

    if (u_Vignette > 0.5) {
        float dist = distance(uv, vec2(0.5));
        float vig = smoothstep(0.40, 0.85, dist);
        col = mix(col, col * vec3(0.92, 0.94, 0.98), vig * 0.35 * intensity);
    }

    return col;
}

void main() {
    vec4 scene = texture(Sampler0, texCoord0);
    vec3 col = scene.rgb;
    float intensity = clamp(u_Intensity, 0.0, 2.0);

    int mode = int(round(u_Mode));
    if (mode == 0) {
        col = applyWinter(col, texCoord0, intensity);
    } else if (mode == 1) {
        col = applySummer(col, texCoord0, intensity);
    } else if (mode == 2) {
        col = applyAutumn(col, texCoord0, intensity);
    } else if (mode == 3) {
        col = applySpring(col, texCoord0, intensity);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), scene.a);
}

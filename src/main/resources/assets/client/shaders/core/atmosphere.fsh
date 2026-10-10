#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D uScene;
uniform sampler2D uDepth;
uniform mat4 uInverseViewProjection;
uniform vec3 uCamera;
uniform vec2 uResolution;
uniform int uMode; // 0 = Dawn, 1 = Dust, 2 = Snow, 3 = Embers

uniform float uTime;
uniform float uDistance;
uniform float uOutside;
uniform vec3 uSkyTop;
uniform vec3 uSkyHorizon;
uniform vec3 uFogCool;
uniform vec3 uFogWarm;
uniform vec3 uSunColor;
uniform vec3 uSunDirection;
uniform float uDensity;
uniform float uScatterHeight;
uniform float uRays;
uniform float uSoftness;
uniform float uAmount;
uniform float uSpeed;
uniform float uHaze;
uniform float uGrade;
uniform float uVignette;
uniform vec3 uSunScreen;

float hash31(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 reconstructCamRel(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 p = uInverseViewProjection * clip;
    return p.xyz / max(abs(p.w), 1e-6);
}

// God rays calculation
vec3 calculateGodRays(vec2 uv, float intensity) {
    if (intensity <= 0.001 || uSunScreen.z <= 0.01) return vec3(0.0);
    vec2 sunPos = uSunScreen.xy;
    vec2 delta = (sunPos - uv) * (1.0 / 12.0);
    vec2 curUv = uv;
    float occ = 0.0;
    float decay = 1.0;
    for (int i = 0; i < 12; i++) {
        curUv += delta;
        float d = texture(uDepth, curUv).r;
        if (d >= 0.9999 || d <= 0.0001) {
            occ += decay;
        }
        decay *= 0.88;
    }
    occ /= 7.0;
    return uSunColor * (occ * intensity * uOutside);
}

// 3D Particles
float renderParticles(vec3 worldPos, vec3 camPos, int mode, float amount, float speed) {
    if (amount <= 0.01) return 0.0;
    vec3 p = worldPos - camPos;
    float dist = length(p);
    if (dist > 28.0) return 0.0;

    float t = uTime * speed;

    if (mode == 1) { // Dust: horizontal drifting
        p += vec3(t * 1.5, sin(t * 0.5 + p.x * 0.2) * 0.4, t * 0.8);
        vec3 grid = floor(p * 2.5);
        vec3 f = fract(p * 2.5) - 0.5;
        float rnd = hash31(grid);
        if (rnd > (1.0 - amount * 0.35)) {
            float d = length(f + (vec3(rnd, fract(rnd*10.0), fract(rnd*100.0)) - 0.5) * 0.6);
            return smoothstep(0.12, 0.02, d) * clamp(1.0 - dist / 26.0, 0.0, 1.0) * (0.4 + 0.6 * rnd);
        }
    } else if (mode == 2) { // Snow: falling downwards
        p += vec3(sin(t * 0.4 + p.y * 0.1) * 0.5, -t * 2.0, cos(t * 0.3 + p.y * 0.1) * 0.5);
        vec3 grid = floor(p * 1.8);
        vec3 f = fract(p * 1.8) - 0.5;
        float rnd = hash31(grid);
        if (rnd > (1.0 - amount * 0.45)) {
            float d = length(f + (vec3(rnd, fract(rnd*7.0), fract(rnd*43.0)) - 0.5) * 0.7);
            return smoothstep(0.14, 0.03, d) * clamp(1.0 - dist / 26.0, 0.0, 1.0);
        }
    } else if (mode == 3) { // Embers: rising upwards
        p += vec3(sin(t * 0.8 + p.y * 0.3) * 0.4, t * 1.8, cos(t * 0.7 + p.y * 0.3) * 0.4);
        vec3 grid = floor(p * 2.2);
        vec3 f = fract(p * 2.2) - 0.5;
        float rnd = hash31(grid);
        if (rnd > (1.0 - amount * 0.30)) {
            float d = length(f + (vec3(rnd, fract(rnd*13.0), fract(rnd*71.0)) - 0.5) * 0.6);
            float flicker = 0.5 + 0.5 * sin(t * 5.0 + rnd * 20.0);
            return smoothstep(0.10, 0.02, d) * clamp(1.0 - dist / 24.0, 0.0, 1.0) * flicker * 1.8;
        }
    }
    return 0.0;
}

void main() {
    vec3 scene = texture(uScene, texCoord).rgb;
    float depth = texture(uDepth, texCoord).r;

    bool isSky = (depth >= 0.9999 || depth <= 0.0001);

    vec3 camRel = reconstructCamRel(texCoord, isSky ? 0.999 : depth);
    float dist = isSky ? uDistance : length(camRel);
    vec3 rayDir = normalize(camRel);
    vec3 worldPos = isSky ? (uCamera + rayDir * uDistance) : (uCamera + camRel);

    vec3 col = scene;

    if (uMode == 0) { // Dawn ("Рассвет")
        float cosTheta = dot(rayDir, normalize(uSunDirection));
        float sunGlow = pow(max(0.0, cosTheta), mix(64.0, 12.0, uSoftness * 0.01)) * (uRays * 0.01);

        float horizonFactor = exp(-max(0.0, rayDir.y) * 3.5);
        vec3 skyAtmosphere = mix(uSkyTop, uSkyHorizon, clamp(horizonFactor, 0.0, 1.0));

        if (isSky) {
            col = mix(col, skyAtmosphere, 0.65 * uOutside);
            col += uSunColor * (sunGlow * 1.5 * uOutside);
        } else {
            // Gentle volumetric height-fog near ground level
            float heightAtten = clamp((uScatterHeight - worldPos.y + 10.0) / 40.0, 0.0, 1.0);
            float fogDensity = uDensity * 0.008 * (0.2 + 0.8 * heightAtten);
            float fogFactor = 1.0 - exp(-dist * fogDensity);
            fogFactor = clamp(fogFactor * uOutside, 0.0, 0.80);

            vec3 fogCol = mix(uFogCool, uFogWarm, clamp(cosTheta * 0.5 + 0.5, 0.0, 1.0));
            fogCol += uSunColor * (sunGlow * 0.4);

            col = mix(col, fogCol, fogFactor);
        }

        col += calculateGodRays(texCoord, uRays * 0.008);
    } else {
        // Mood Modes: 1 = Dust ("Пыль"), 2 = Snow ("Снег"), 3 = Embers ("Угли")
        if (isSky) {
            col = mix(col, uSkyHorizon, clamp(uHaze * 0.7, 0.0, 0.85));
        } else {
            float distRel = clamp(dist / max(uDistance, 32.0), 0.0, 1.0);
            float fogFactor = (1.0 - exp(-distRel * 3.0 * uHaze)) * max(0.25, uOutside);
            fogFactor = clamp(fogFactor, 0.0, 0.80);

            vec3 moodFog = mix(uFogCool, uFogWarm, 0.5);
            col = mix(col, moodFog, fogFactor);
        }

        // Color grading
        if (uGrade > 0.001) {
            if (uMode == 1) { // Warm sepia / desert amber
                vec3 sepia = vec3(dot(col, vec3(0.393, 0.769, 0.189)),
                                  dot(col, vec3(0.349, 0.686, 0.168)),
                                  dot(col, vec3(0.272, 0.534, 0.131)));
                col = mix(col, sepia * 1.12, uGrade * 0.40);
            } else if (uMode == 2) { // Crisp ice blue / silver
                float lum = dot(col, vec3(0.299, 0.587, 0.114));
                vec3 ice = mix(vec3(lum), col, 0.75) * vec3(0.94, 0.98, 1.10);
                col = mix(col, ice, uGrade * 0.45);
            } else if (uMode == 3) { // Dark volcanic contrast with red shadows
                vec3 fireGrade = col * vec3(1.12, 0.90, 0.80) + vec3(0.04, 0.01, 0.0) * (1.0 - col);
                col = mix(col, fireGrade, uGrade * 0.50);
            }
        }

        // 3D Particles
        if (!isSky && dist < 28.0) {
            float pIntensity = renderParticles(worldPos, uCamera, uMode, uAmount, uSpeed);
            if (pIntensity > 0.0) {
                if (uMode == 1) {
                    col += uSunColor * pIntensity * 0.6;
                } else if (uMode == 2) {
                    col += vec3(0.95, 0.97, 1.0) * pIntensity * 0.8;
                } else if (uMode == 3) {
                    col += vec3(1.0, 0.45, 0.1) * pIntensity;
                }
            }
        }

        // Vignette
        if (uVignette > 0.001) {
            float distFromCenter = length((texCoord - 0.5) * vec2(uResolution.x / max(uResolution.y, 1.0), 1.0));
            float vig = smoothstep(0.40, 0.90, distFromCenter) * uVignette;
            col *= (1.0 - vig * 0.60);
        }
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}

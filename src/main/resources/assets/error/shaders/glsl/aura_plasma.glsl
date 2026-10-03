#version 120

uniform float time;
uniform float speed;
uniform float intensity;
uniform float rainStrength;
uniform float aspectRatio;
uniform float tanHalfFov;
uniform float horizonFog;

uniform vec3 cameraForward;
uniform vec3 cameraUp;
uniform vec3 cameraRight;
uniform vec3 lowColor;
uniform vec3 fogColor;

varying vec2 screenUv;

#define TAU 6.28318530718
#define PI 3.14159265
#define MAX_ITER 5
#define BASE_INTENSITY 0.005

vec3 skyWorldDir() {
    vec2 ndc = screenUv * 2.0 - 1.0;
    vec3 viewDir = normalize(vec3(ndc.x * aspectRatio * tanHalfFov, ndc.y * tanHalfFov, -1.0));
    return normalize(viewDir.x * cameraRight + viewDir.y * cameraUp - viewDir.z * cameraForward);
}

float plasmaValue(vec2 uv, float animTime) {
    vec2 p = mod(uv * TAU, TAU) - 250.0;
    vec2 iterPos = p;
    float value = 1.0;
    float timeOffset = animTime * 0.5 + 23.0;

    for (int n = 0; n < MAX_ITER; n++) {
        float iterTime = timeOffset * (1.0 - (3.5 / float(n + 1)));
        iterPos = p + vec2(
            cos(iterTime - iterPos.x) + sin(iterTime + iterPos.y),
            sin(iterTime - iterPos.y) + cos(iterTime + iterPos.x)
        );

        float sinX = sin(iterPos.x + iterTime);
        float cosY = cos(iterPos.y + iterTime);
        float compX = p.x * BASE_INTENSITY / max(abs(sinX), 0.001);
        float compY = p.y * BASE_INTENSITY / max(abs(cosY), 0.001);
        float dist = sqrt(compX * compX + compY * compY);
        value += 1.0 / dist;
    }

    value = 1.17 - pow(value / float(MAX_ITER), 1.4);
    float pwr = value * value;
    pwr = pwr * pwr;
    pwr = pwr * pwr;
    return abs(pwr);
}

void main() {
    vec3 ray = skyWorldDir();

    float scale = clamp(intensity, 0.2, 3.0);
    float animTime = time * speed * 0.08;
    float elev = asin(clamp(ray.y, -1.0, 1.0)) / PI;

    float az1 = atan(ray.x, ray.z);
    vec2 uv1 = vec2(az1 / TAU, elev) * scale;
    float az2 = atan(ray.z, -ray.x);
    vec2 uv2 = vec2(az2 / TAU, elev) * scale;

    float p1 = plasmaValue(uv1, animTime);
    float p2 = plasmaValue(uv2, animTime);
    float seam = abs(az1) / PI;
    float blend = smoothstep(0.72, 0.92, seam);
    float pwr = mix(p1, p2, blend);

    vec3 baseColor = vec3(pwr);
    vec3 enhancedColor = clamp(baseColor + vec3(0.0, 0.35, 0.5), 0.0, 1.0);
    float luminance = dot(enhancedColor, vec3(0.299, 0.587, 0.114));

    float visibility = (1.0 - rainStrength) * (1.0 - rainStrength);
    vec3 plasmaCol = vec3(luminance) * lowColor * 1.4 * intensity * visibility;

    // Плавный переход в цвет тумана у горизонта — без чёрной полосы
    float toFog = smoothstep(0.62, -0.08, ray.y) * clamp(horizonFog, 0.0, 1.0);
    vec3 col = mix(plasmaCol, fogColor, toFog);
    gl_FragColor = vec4(col, 1.0);
}

#version 120

uniform float time;
uniform float speed;
uniform float intensity;
uniform float rainStrength;
uniform float visibilityFactor;
uniform float aspectRatio;
uniform float tanHalfFov;

uniform vec3 cameraForward;
uniform vec3 cameraUp;
uniform vec3 cameraRight;
uniform vec3 lowColor;
uniform vec3 highColor;

varying vec2 screenUv;

const float Bright = 0.05;
const float Fog = 0.03;
const float StepSize = 0.3;
const int RaySteps = 80;

float map(in vec3 p, float animTime) {
    float s, c;
    vec2 rot = vec2(cos(0.1 * animTime), sin(0.0357 * animTime));

    c = cos(rot.y);
    s = -sin(rot.y);
    p.yz = mat2(c, s, -s, c) * p.yz;

    c = cos(rot.x);
    s = -sin(rot.x);
    p.xz = mat2(c, s, -s, c) * p.xz;

    float k = length(p) < 1.0 ? dot(p, p) : 1.0;
    p /= k * 0.77;
    p = mod(p + animTime * vec3(0.3, 0.5, 0.85), 2.0) - 1.0;

    return min(min(length(p.xy), length(p.xz)), length(p.yz)) * k;
}

vec3 march(vec3 ray, vec3 dir, float animTime) {
    float dist;
    vec3 glowCol = mix(highColor, vec3(1.0), 0.4) * vec3(3.0, 2.5, 1.0);
    vec3 col = vec3(0.0);
    float alph = 1.0;

    for (int i = 0; i < RaySteps; i++) {
        dist = map(ray, animTime) * StepSize;
        ray += dist * dir;
        col = mix(glowCol, col, alph);
        alph = max(0.0, alph - Bright / (dist + Fog) / float(RaySteps * RaySteps));
        if (alph < 0.9) {
            break;
        }
    }

    return col;
}

vec3 skyWorldDir() {
    vec2 ndc = screenUv * 2.0 - 1.0;
    vec3 viewDir = normalize(vec3(ndc.x * aspectRatio * tanHalfFov, ndc.y * tanHalfFov, -1.0));
    return normalize(viewDir.x * cameraRight + viewDir.y * cameraUp - viewDir.z * cameraForward);
}

void main() {
    float visibility = visibilityFactor * (1.0 - rainStrength) * (1.0 - rainStrength);
    if (visibility <= 0.001) {
        gl_FragColor = vec4(0.0);
        return;
    }

    vec3 wpos = skyWorldDir();
    float skyMask = smoothstep(-0.05, 0.2, wpos.y);
    if (skyMask <= 0.001) {
        gl_FragColor = vec4(0.0);
        return;
    }

    float animTime = time * speed;
    vec3 rayDir = wpos;
    vec3 rayBeg = -rayDir * 4.0;

    vec3 col = march(rayBeg, rayDir, animTime);
    col *= intensity * visibility * skyMask;
    gl_FragColor = vec4(col, 1.0);
}

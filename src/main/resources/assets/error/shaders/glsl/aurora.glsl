#version 120

uniform sampler2D noisetex;

uniform float time;
uniform float speed;
uniform float intensity;
uniform float rainStrength;
uniform float visibilityFactor;
uniform float aspectRatio;
uniform float tanHalfFov;

uniform vec3 cameraPos;
uniform vec3 cameraForward;
uniform vec3 cameraUp;
uniform vec3 cameraRight;
uniform vec3 lowColor;
uniform vec3 highColor;

varying vec2 screenUv;

float noiseTex(vec2 uv) {
    return texture2D(noisetex, fract(uv)).b;
}

float auroraSample(vec2 coord, vec2 wind, float voU) {
    float noise = noiseTex(coord * 0.0625 + wind * 0.25) * 3.0;
    noise += noiseTex(coord * 0.03125 + wind * 0.15) * 3.0;
    noise = max(1.0 - 4.0 * (0.5 * voU + 0.5) * abs(noise - 3.0), 0.0);
    return noise;
}

void main() {
    const float sampleCount = 24.0;

    float sampleStep = 1.0 / sampleCount;
    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453 + time);
    float currentStep = dither * sampleStep;

    vec2 ndc = screenUv * 2.0 - 1.0;
    vec3 viewDir = normalize(vec3(ndc.x * aspectRatio * tanHalfFov, ndc.y * tanHalfFov, -1.0));
    vec3 wpos = normalize(viewDir.x * cameraRight + viewDir.y * cameraUp - viewDir.z * cameraForward);
    float voU = dot(wpos, vec3(0.0, 1.0, 0.0));
    float heightFactor = clamp((cameraPos.y - 72.0) / 96.0, 0.0, 1.0);
    float horizonAmount = 1.0 - clamp(voU, 0.0, 1.0);
    float domeCurve = horizonAmount * horizonAmount * mix(0.24, 0.95, heightFactor);
    float auroraVoU = voU + mix(0.04, 0.16, heightFactor) + domeCurve + heightFactor * 0.28;

    float visibility = visibilityFactor * (1.0 - rainStrength) * (1.0 - rainStrength);
    visibility *= clamp((cameraPos.y + 6.0) / 8.0, 0.0, 1.0);

    vec3 aurora = vec3(0.0);
    vec2 wind = vec2(
        time * speed * 0.000125,
        sin(time * speed * 0.05) * 0.00025
    );

    vec3 auroraLowCol = lowColor * lowColor;
    vec3 auroraHighCol = highColor * highColor;

    if (auroraVoU > 0.0 && visibility > 0.0) {
        for (int i = 0; i < 24; i++) {
            float layerBase = mix(8.0, 4.0, heightFactor);
            float layerSpan = mix(7.0, 3.25, heightFactor);
            vec3 planeCoord = wpos * ((layerBase + currentStep * layerSpan) / max(auroraVoU, 0.08)) * 0.004;

            vec2 coord = cameraPos.xz * 0.00004 + planeCoord.xz;
            coord += vec2(coord.y, -coord.x) * 0.3;

            float noise = auroraSample(coord, wind, auroraVoU);

            if (noise > 0.0) {
                float radiusScale = mix(3.75, 1.35, heightFactor);
                noise *= noiseTex(coord * 0.125 + wind * 0.25);
                noise *= 0.5 * noiseTex(coord + wind * 16.0) + 0.75;
                noise = noise * noise * 3.0 * sampleStep;
                noise *= max(sqrt(max(1.0 - length(planeCoord.xz) * radiusScale, 0.0)), 0.0);

                vec3 auroraColor = mix(auroraLowCol, auroraHighCol, pow(currentStep, 0.4));
                aurora += noise * auroraColor * exp2(-6.0 * float(i) * sampleStep);
            }

            currentStep += sampleStep;
        }
    }

    vec3 color = sqrt(max(aurora * visibility * intensity, vec3(0.0)));
    gl_FragColor = vec4(color, 1.0);
}

vec3 auraRaymarch(vec3 worldDir, vec3 cameraPos, float effectTime, float effectSpeed, float visibility,
                  vec3 lowColor, vec3 highColor, float effectIntensity) {
    const float sampleCount = 24.0;
    float sampleStep = 1.0 / sampleCount;
    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453 + effectTime);
    float currentStep = dither * sampleStep;

    float heightFactor = clamp((cameraPos.y - 72.0) / 96.0, 0.0, 1.0);
    float voU = dot(worldDir, vec3(0.0, 1.0, 0.0));
    float horizonAmount = 1.0 - clamp(voU, 0.0, 1.0);
    float domeCurve = horizonAmount * horizonAmount * mix(0.24, 0.95, heightFactor);
    float auroraVoU = voU + mix(0.04, 0.16, heightFactor) + domeCurve + heightFactor * 0.28;

    vec3 aurora = vec3(0.0);
    vec2 wind = auraWind(effectTime, effectSpeed);
    vec3 auroraLowCol = lowColor * lowColor;
    vec3 auroraHighCol = highColor * highColor;

    if (auroraVoU > 0.0 && visibility > 0.0) {
        for (int i = 0; i < 24; i++) {
            float layerBase = mix(8.0, 4.0, heightFactor);
            float layerSpan = mix(7.0, 3.25, heightFactor);
            vec3 planeCoord = worldDir * ((layerBase + currentStep * layerSpan) / max(auroraVoU, 0.08)) * 0.004;

            vec2 coord = cameraPos.xz * 0.00004 + planeCoord.xz;
            coord += vec2(coord.y, -coord.x) * 0.3;

            float noise = auraModeSample(coord, wind, auroraVoU);

            if (noise > 0.0) {
                float radiusScale = mix(3.75, 1.35, heightFactor);
                noise *= auraModeDetail(coord, wind);
                noise = noise * noise * 3.0 * sampleStep;
                noise *= max(sqrt(max(1.0 - length(planeCoord.xz) * radiusScale, 0.0)), 0.0);

                vec3 auroraColor = mix(auroraLowCol, auroraHighCol, pow(currentStep, 0.4));
                aurora += noise * auroraColor * exp2(-6.0 * float(i) * sampleStep);
            }

            currentStep += sampleStep;
        }
    }

    return sqrt(max(aurora * visibility * effectIntensity, vec3(0.0)));
}

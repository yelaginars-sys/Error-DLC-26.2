#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform float useTextureColor;

void main() {
    float centerA = texture2D(texture, gl_TexCoord[0].st).a;
    if (centerA > 0.05) discard;

    float sumA = 0.0;
    vec3 colorSum = vec3(0.0);
    float r = max(radius, 0.5);

    const int K_RAD = 3;

    for (int dx = -K_RAD; dx <= K_RAD; dx++) {
        for (int dy = -K_RAD; dy <= K_RAD; dy++) {
            if (dx == 0 && dy == 0) continue;

            vec2 offset = vec2(float(dx), float(dy)) * texelSize;
            vec4 sampleTex = texture2D(texture, gl_TexCoord[0].st + offset);

            if (sampleTex.a > 0.0) {
                float d = length(vec2(float(dx), float(dy)));
                if (d <= r) {
                    float w = max(0.0, 1.0 - (d / r));
                    float contrib = sampleTex.a * w;
                    sumA += contrib;
                    colorSum += sampleTex.rgb * contrib;
                }
            }
        }
    }

    if (sumA < 0.001) discard;

    float finalAlpha = min(1.0, sumA * 2.5);
    vec3 outColor = useTextureColor > 0.5
            ? (colorSum / max(sumA, 0.001))
            : color;

    gl_FragColor = vec4(outColor, finalAlpha);
}

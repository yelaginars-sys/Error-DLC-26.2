#version 150 core
in vec2 fragCoord;
uniform sampler2D uTexIn;
uniform vec4 color;
uniform vec2 texelSize;
uniform float edgeFade;
uniform float intensity;
out vec4 fragColor;
void main() {
    vec4 texColor = texture(uTexIn, fragCoord);
    if (texColor.a < 0.01) discard;
    float minDist = 999.0;
    float maxRadius = edgeFade;
    for (float angle = 0.0; angle < 6.28318; angle += 0.4) {
        for (float r = 1.0; r <= maxRadius; r += 1.0) {
            vec2 o = vec2(cos(angle), sin(angle)) * r * texelSize;
            float sampleAlpha = texture(uTexIn, fragCoord + o).a;
            if (sampleAlpha < 0.1) {
                minDist = min(minDist, r);
                break;
            }
        }
    }
    if (minDist > maxRadius - 0.5) {
        minDist = maxRadius;
    }
    float normalizedDist = minDist / maxRadius;
    float minAlpha = intensity;
    float alpha = mix(1.0, minAlpha, normalizedDist) * color.a * texColor.a;
    fragColor = vec4(color.rgb, alpha);
}

#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float Decay;
uniform float Initialize;

out vec4 OutColor;

void main() {
    vec4 previous = texture(Sampler0, TexCoord);
    vec4 current = texture(Sampler1, TexCoord);
    float safeDecay = clamp(Decay, 0.0, 1.0);

    // Retain the actual blurred light at its old screen position. Because this is
    // purely temporal, swing, bobbing and mouse-driven hand movement work equally.
    float retainedLevel = max(previous.a * safeDecay
            - (1.0 - safeDecay) * 0.035, 0.0);
    vec3 retainedColor = previous.rgb
            * (retainedLevel / max(previous.a, 0.0001));

    vec4 history = vec4(retainedColor, retainedLevel);
    if (current.a >= retainedLevel) {
        history = current;
    }

    if (Initialize > 0.5) {
        history = current;
    }

    OutColor = history;
}

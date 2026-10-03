#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <error:ui_common.glsl>
#moj_import <error:ui_fragment.glsl>

uniform sampler2D Sampler0;

in vec2 localPos;
in vec2 screenUv;
in vec4 tintColor;
flat in vec2 halfSize;
flat in float cornerRadius;
flat in float overallAlpha;

out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 half_ = max(halfSize, vec2(0.5));
    float radius = clamp(cornerRadius, 0.0, min(half_.x, half_.y));
    float dist = ui_roundedBoxSdfUniform(localPos, half_, radius);
    float mask = ui_coverage(dist);
    if (mask <= 0.003) {
        discard;
    }

    vec3 blurred = texture(Sampler0, clamp(screenUv, vec2(0.0), vec2(1.0))).rgb;
    vec4 tint = tintColor * ColorModulator;

    // Authentic Nursultan frosted acrylic blur:
    // Blurred world shines through tinted and textured with fine dither grain
    float dither = (hash12(gl_FragCoord.xy) - 0.5) / 64.0;
    vec3 panel = (blurred + vec3(dither)) * (tint.rgb * 1.55);
    panel = mix(panel, tint.rgb, clamp(tint.a * 0.25, 0.0, 1.0));

    float alpha = mask * overallAlpha;
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(panel, alpha);
}

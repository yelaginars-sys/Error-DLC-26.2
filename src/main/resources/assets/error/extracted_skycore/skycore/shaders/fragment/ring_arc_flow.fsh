#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 location, rectSize;
uniform vec4 colorA;
uniform vec4 colorB;
uniform float radius;
uniform float thickness;
uniform float startAngle;
uniform float sweepAngle;
uniform float smoothness;
uniform float endFade;
uniform float rotation;

const float PI2 = 6.28318530718;

void main() {
    vec2 center = rectSize * 0.5;
    vec2 p = texCoord * rectSize - center;
    float dist = length(p);

    float ang = degrees(atan(p.y, p.x));
    float start = startAngle;
    float sweep = clamp(sweepAngle, 0.0, 360.0);
    float rel = mod(ang - start + 360.0, 360.0);
    float inArc = sweep >= 359.5 ? 1.0 : step(rel, sweep);

    float edge = 1.0;
    if (endFade > 0.001 && sweep < 359.5) {
        float fade = min(endFade, sweep * 0.5);
        edge = smoothstep(0.0, fade, rel) * smoothstep(0.0, fade, sweep - rel);
    }

    float outer = radius;
    float localThick = mix(thickness * 0.12, thickness, edge);
    float inner = max(0.0, radius - localThick);
    float aa = max(smoothness, 0.75);
    float ring = smoothstep(inner - aa, inner + aa, dist) * (1.0 - smoothstep(outer - aa, outer + aa, dist));

    // Same Harmony wave as the bar, sampled along the ring angle
    float normalized = mod(ang + 90.0, 360.0) / 360.0;
    float gradientPos = normalized + rotation / PI2;
    float gradient = (sin(gradientPos * PI2 * 2.0) + 1.0) * 0.5;
    vec4 finalColor = mix(colorA, colorB, gradient);

    float alpha = finalColor.a * ring * inArc * edge;
    if (alpha <= 0.001) {
        discard;
    }
    fragColor = vec4(finalColor.rgb, alpha);
}

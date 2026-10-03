#version 150 core
in vec2 fragCoord;
uniform vec2 size;
uniform vec4 color;
uniform float radius;
uniform float startAngle;
uniform float endAngle;
uniform float thickness;
uniform float smoothness;
out vec4 fragColor;
void main() {
    vec2 st = (fragCoord - 0.5) * size;
    float dist = length(st);
    float outer = radius;
    float inner = radius - thickness;
    float mask = smoothstep(outer, outer - smoothness, dist) -
                 smoothstep(inner, inner - smoothness, dist);
    float angle = degrees(atan(st.y, st.x));
    if (angle < 0.0) angle += 360.0;
    float s = mod(startAngle, 360.0);
    float e = mod(endAngle, 360.0);
    float angleMask = 1.0;
    if (s != e) {
        if (s < e) {
            if (angle < s || angle > e) angleMask = 0.0;
        } else {
            if (angle < s && angle > e) angleMask = 0.0;
        }
    }
    fragColor = vec4(color.rgb, color.a * mask * angleMask);
}

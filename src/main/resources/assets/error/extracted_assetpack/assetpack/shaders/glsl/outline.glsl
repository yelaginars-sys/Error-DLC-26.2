#version 150 core
in vec2 fragCoord;
uniform vec2 u_size;
uniform float u_radius;
uniform float u_border_size;
uniform float u_alpha1, u_alpha2, u_alpha3, u_alpha4;
uniform vec4 u_color_1, u_color_2, u_color_3, u_color_4;
out vec4 fragColor;
void main(void) {
    vec2 tex = fragCoord;
    vec4 c1 = vec4(u_color_1.rgb, u_alpha1);
    vec4 c2 = vec4(u_color_2.rgb, u_alpha2);
    vec4 c3 = vec4(u_color_3.rgb, u_alpha3);
    vec4 c4 = vec4(u_color_4.rgb, u_alpha4);
    vec4 finalColor = mix(mix(c1, c2, tex.y), mix(c3, c4, tex.y), tex.x);
    vec2 position = (abs(tex - 0.5) + 0.5) * u_size;
    float dist = length(max(position - u_size + u_radius + u_border_size, 0.0)) - u_radius + 0.5;
    float borderAlpha = smoothstep(0.0, 1.0, dist) - smoothstep(0.0, 1.0, dist - u_border_size);
    fragColor = vec4(finalColor.rgb, finalColor.a * borderAlpha);
}

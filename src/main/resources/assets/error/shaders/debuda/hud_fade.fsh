#version 150

uniform sampler2D Sampler0;
uniform float Opacity;

out vec4 OutColor;

void main() {
    // Framebuffer alpha is not widget opacity. Use only the saved RGB.
    OutColor = vec4(texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).rgb, Opacity);
}

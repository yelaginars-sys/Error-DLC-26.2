#version 120
uniform sampler2D font;
uniform vec4 inColor;
uniform float width;
uniform float maxWidth;
uniform float guiFadeAlpha;

void main() {
    float f = clamp(smoothstep(0.5, 1, 1 - (gl_FragCoord.x - maxWidth) / width), 0, 1);
    vec2 pos = gl_TexCoord[0].xy;
    vec4 color = texture2D(font, pos);
    if (color.a > 0) {
        color.a = color.a * f;
    }
    vec4 outc = color * inColor;
    gl_FragColor = vec4(outc.rgb, outc.a * guiFadeAlpha);
}

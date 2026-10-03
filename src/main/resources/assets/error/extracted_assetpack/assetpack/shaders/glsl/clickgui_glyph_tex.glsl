#version 120

uniform sampler2D tex;
uniform float guiFadeAlpha;

void main() {
    vec4 t = texture2D(tex, gl_TexCoord[0].st);
    vec4 c = gl_Color;
    gl_FragColor = vec4(c.rgb * t.rgb, c.a * t.a * guiFadeAlpha);
}

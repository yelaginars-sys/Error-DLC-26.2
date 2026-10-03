#version 120

uniform sampler2D texture;
uniform vec3 color;

void main() {
    vec4 t = texture2D(texture, gl_TexCoord[0].st);
    gl_FragColor = vec4(t.rgb * color, t.a);
}

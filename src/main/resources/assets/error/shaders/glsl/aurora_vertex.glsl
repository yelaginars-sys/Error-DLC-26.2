#version 120

varying vec2 screenUv;

void main() {
    screenUv = gl_MultiTexCoord0.xy;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);
}

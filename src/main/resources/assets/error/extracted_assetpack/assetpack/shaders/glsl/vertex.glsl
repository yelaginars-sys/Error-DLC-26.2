#version 120
varying vec3 vPos;
void main() {
    vPos = gl_Vertex.xyz;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_FrontColor = gl_Color;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
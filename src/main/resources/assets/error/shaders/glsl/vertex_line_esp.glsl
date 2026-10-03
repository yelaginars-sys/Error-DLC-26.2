#version 120

varying float linePos;

void main() {
    linePos = gl_Color.a;
    gl_FrontColor = vec4(gl_Color.rgb, 1.0);
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}

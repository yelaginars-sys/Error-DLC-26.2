#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // UV matches skycore-release gl_TexCoord[0].st over the quad (triangles-safe)
    FragCoord = UV0;
    TexCoord = UV0;
    FragColor = vec4(1.0);
}

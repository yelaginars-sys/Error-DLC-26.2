#version 150

layout(std140) uniform Uniforms {
    vec4 uTexel;
    vec4 uParams;
};

out vec2 vUV;

void main() {
    vec2 corners[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 pos = corners[indices[gl_VertexID]];
    vUV = pos * 0.5 + 0.5;
    gl_Position = vec4(pos, 0.0, 1.0);

    float keepAlive = uTexel.x + uParams.x;
    if (isnan(keepAlive) || isinf(keepAlive)) keepAlive = 0.0;
    gl_Position.z += keepAlive * 0.0;
}

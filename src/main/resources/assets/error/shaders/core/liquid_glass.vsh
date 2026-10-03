#version 150

#define MAX_RECTS 64

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRegion;
    vec4 uViewport;
    vec4 uCommon;
    vec4 uConfig;
    vec4 uGlass;
    vec4 uRects[MAX_RECTS];
    vec4 uParams[MAX_RECTS];
    vec4 uTints[MAX_RECTS];
};

out vec2 vUV;
out vec2 vPixel;

void main() {
    vec2 corners[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = corners[indices[gl_VertexID]];

    vec2 pos = uRegion.xy + vertex * uRegion.zw;
    vPixel = pos;
    gl_Position = uProjection * vec4(pos, 0.0, 1.0);

    float keepAlive = uViewport.x + uCommon.x + uConfig.x + uGlass.x
                    + uRects[0].x + uParams[0].x + uTints[0].x;
    if (isnan(keepAlive) || isinf(keepAlive)) keepAlive = 0.0;
    gl_Position.z += keepAlive * 0.0;

    vUV = (gl_Position.xy / gl_Position.w + vec2(1.0)) * 0.5;
}

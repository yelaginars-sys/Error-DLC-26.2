#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;
flat in vec2 atlasSize;
flat in float distanceRange;

out vec4 fragColor;

float median(float red, float green, float blue) {
    return max(min(red, green), min(max(red, green), blue));
}

float smootherEdge(float lowerBound, float upperBound, float x) {
    float t = clamp((x - lowerBound) / (upperBound - lowerBound), 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    vec2 deriv = vec2(dFdx(texCoord0.x) * atlasSize.x, dFdy(texCoord0.y) * atlasSize.y);
    float derivatives = dot(deriv, deriv);
    float toPixels = derivatives > 0.000001 ? distanceRange * inversesqrt(derivatives) : distanceRange;
    toPixels = max(toPixels, 1.0);

    float sigDist = median(texColor.r, texColor.g, texColor.b) - 0.5;
    float alpha = smootherEdge(-0.5, 0.5, sigDist * toPixels);
    if (alpha <= 0.0005) discard;

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}

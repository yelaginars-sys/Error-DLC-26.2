#version 330

in vec4 vertexColor;
in vec2 localCoord;

out vec4 fragColor;

void main() {
    fragColor = vec4(0.0);
    // body: 1 в центре лоскута, 0 на кромке. Тело сплошное, внешняя треть мягко тает —
    // силуэт остаётся рваным (геометрия), но края пуховые.
    float body = localCoord.x;
    float alpha = vertexColor.a * smoothstep(0.0, 0.35, body);
    if (alpha <= 0.0005) discard;
    fragColor = vec4(vertexColor.rgb * (0.85 + 0.15 * body), alpha);
}

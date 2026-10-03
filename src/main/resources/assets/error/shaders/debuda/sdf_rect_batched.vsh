#version 150

// Батч-версия SDF rounded rect (только fill, без border/shadow). Параметры,
// которые в одиночном sdf_rect были uniform'ами, здесь приходят per-vertex
// через стандартные каналы (чтобы много прямоугольников рисовать одним
// draw-call'ом штатным BufferBuilder):
//   Position — экранная позиция вершины (уже pre-transformed на CPU)
//   UV0      — localPos: позиция вершины относительно центра rect в пикселях
//   Color    — цвет заливки
//   UV1      — halfSize rect (halfW, halfH) в пикселях, ivec2
//   UV2.x    — radius * 256 (скругление), ivec2
//   UV2.y    — borderWidth * 256 (0 = без бордера); цвет бордера — uniform на батч

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vLocal;
out vec2 vHalf;
out float vRadius;
out float vBorder;
out vec4 vFill;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vLocal  = UV0;
    vHalf   = vec2(UV1);
    vRadius = float(UV2.x) / 256.0;
    vBorder = float(UV2.y) / 256.0;
    vFill   = Color;
}

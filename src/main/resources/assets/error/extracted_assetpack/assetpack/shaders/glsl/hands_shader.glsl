#version 120

uniform sampler2D texture;
uniform float time;
uniform vec2 speed;
uniform vec3 themeColor;
uniform float alpha;
uniform vec3 cameraOffset;
uniform vec2 resolution;

void main( void ) {
    vec2 uv = gl_TexCoord[0].st;

    // Маска по альфе предмета
    float mask = texture2D(texture, uv).a;
    if (mask < 0.01) discard;

    // Смещаем позицию на основе движения игрока для "movement-reactive" эффекта
    // Используем cameraOffset для смещения плазмы
    vec2 move = vec2(cameraOffset.x + cameraOffset.z, cameraOffset.y) * 2.5;
    vec2 position = (gl_FragCoord.xy / resolution.xy) + move;
    
    // Множитель времени из настроек
    float t = time * speed.x;

    float p = 0.0;
    p += sin( position.x * cos( t / 15.0 ) * 80.0 ) + cos( position.y * cos( t / 15.0 ) * 10.0 );
    p += sin( position.y * sin( t / 10.0 ) * 40.0 ) + cos( position.x * sin( t / 25.0 ) * 40.0 );
    p += sin( position.x * sin( t / 5.0 ) * 10.0 ) + sin( position.y * sin( t / 35.0 ) * 80.0 );
    p *= sin( t / 10.0 ) * 0.5;

    // Базовая плазма из песочницы (адаптированная)
    vec3 plasma = vec3( p, p * 0.5, sin( p + t / 3.0 ) * 0.75 );
    
    // Накладываем цвет темы и ограничиваем диапазон
    vec3 finalColor = clamp(plasma + themeColor * 0.6, 0.0, 1.0); 
    
    gl_FragColor = vec4(finalColor, mask * alpha);
}

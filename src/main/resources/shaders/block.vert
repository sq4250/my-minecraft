#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in float aBlockType;

out vec2 TexCoord;
out vec3 FragPos;
out float BlockType;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    // 计算世界空间中的片段位置
    FragPos = vec3(model * vec4(aPos, 1.0));
    
    // 传递纹理坐标和方块类型
    TexCoord = aTexCoord;
    BlockType = aBlockType;
    
    // 计算最终位置
    gl_Position = projection * view * model * vec4(aPos, 1.0);
}
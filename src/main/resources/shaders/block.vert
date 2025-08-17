#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in float aBlockType;
layout (location = 3) in vec3 aNormal;     // 法线属性
layout (location = 4) in vec2 aVertexCoord; // 顶点在面中的坐标 (0-1)
layout (location = 5) in float aAOOcclusion; // AO遮蔽值

out vec2 TexCoord;
out vec3 FragPos;
out float BlockType;
out vec3 Normal;     // 传递法线到片段着色器
out vec2 VertexCoord; // 传递顶点在面中的坐标到片段着色器
out float AOOcclusion; // 传递AO遮蔽值到片段着色器

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    // 计算世界空间中的片段位置
    FragPos = vec3(model * vec4(aPos, 1.0));
    
    // 传递法线（需要使用法线矩阵进行变换，这里简化处理）
    Normal = aNormal;
    
    // 传递顶点在面中的坐标
    VertexCoord = aVertexCoord;
    
    // 传递AO遮蔽值
    AOOcclusion = aAOOcclusion;
    
    // 传递纹理坐标和方块类型
    TexCoord = aTexCoord;
    BlockType = aBlockType;
    
    // 计算最终位置
    gl_Position = projection * view * model * vec4(aPos, 1.0);
}
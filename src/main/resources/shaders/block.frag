#version 330 core
out vec4 FragColor;

in vec2 TexCoord;
in vec3 Normal;
in vec3 FragPos;
in float BlockType;

uniform sampler2D ourTexture;

// 光照参数
uniform vec3 lightDirection;  // 全局光照方向
uniform vec3 lightColor;      // 光照颜色
uniform vec3 ambientColor;    // 环境光颜色
uniform float ambientStrength; // 环境光强度

void main() {
    // 获取纹理颜色
    vec4 texColor = texture(ourTexture, TexCoord);
    if(texColor.a < 0.1)
        discard;
    
    // 根据方块类型调整颜色
    vec3 blockColorTint = vec3(1.0); // 默认无色调
    
    // 水方块 (ID = 7) 添加蓝色调
    if (int(BlockType) == 7) {
        blockColorTint = vec3(0.3, 0.6, 1.0); // 蓝色调
        texColor.rgb = mix(texColor.rgb, texColor.rgb * blockColorTint, 0.7); // 混合70%的蓝色调
    }
    
    // 归一化法向量
    vec3 norm = normalize(Normal);
    
    // 计算光照方向（从片段指向光源）
    vec3 lightDir = normalize(-lightDirection);
    
    // 计算漫反射因子，使用更柔和的映射
    float diff = dot(norm, lightDir);
    // 将 [-1, 1] 映射到 [0.4, 1.0]，减少对比度避免分层
    float softDiff = 0.4 + 0.6 * (diff * 0.5 + 0.5);
    
    // 简化的环境光遮蔽 - 更平滑的过渡
    float ao = 1.0;
    
    // 垂直向下的面（底面）稍暗
    if (norm.y < -0.8) {
        ao = mix(0.7, 1.0, (norm.y + 1.0) * 2.5); // 平滑过渡
    }
    // 垂直面根据朝向平滑调整
    else if (abs(norm.y) < 0.3) {
        // 使用平滑函数避免硬边界
        float sideFactor = 1.0;
        if (norm.x < 0.0 || norm.z < 0.0) {
            sideFactor = 0.8;
        }
        ao = mix(ao, sideFactor, abs(norm.y) / 0.3);
    }
    
    // 简化的阴影效果 - 更柔和
    float shadowFactor = 1.0;
    if (FragPos.y < 2.5) { // 地面附近
        float distToTree = length(FragPos.xz - vec2(2.0, 2.0));
        if (distToTree < 3.5) {
            shadowFactor = mix(0.8, 1.0, distToTree / 3.5);
        }
    }
    
    ao = mix(1.0, ao * shadowFactor, 0.8); // 减少整体阴影强度
    
    // 计算环境光（应用环境光遮蔽）
    vec3 ambient = ambientStrength * ambientColor * ao;
    
    // 计算漫反射光（也应用环境光遮蔽）
    vec3 diffuse = softDiff * lightColor * ao;
    
    // 合并光照
    vec3 result = (ambient + diffuse) * texColor.rgb;
    
    FragColor = vec4(result, texColor.a);
}
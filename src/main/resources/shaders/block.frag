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

// 破坏效果参数
uniform float breakProgress;  // 破坏进度 (0.0 - 1.0)
uniform vec3 targetBlockPos;  // 目标方块位置
uniform int targetFace;       // 目标面

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
    
    // 破坏纹理叠加 - 检查片段是否在目标方块内
    if (breakProgress > 0.0 &&
        FragPos.x >= targetBlockPos.x - 0.501 && FragPos.x <= targetBlockPos.x + 0.501 &&
        FragPos.y >= targetBlockPos.y - 0.501 && FragPos.y <= targetBlockPos.y + 0.501 &&
        FragPos.z >= targetBlockPos.z - 0.501 && FragPos.z <= targetBlockPos.z + 0.501) {
        
        // 计算在当前方块面上的局部UV坐标 [0,1]
        // 需要从TexCoord反推出在当前方块纹理中的相对位置
        vec2 localUV;
        
        // 根据方块类型获取其纹理的UV范围，然后计算局部坐标
        // 由于无法在着色器中获取方块的具体纹理范围，我们使用一个通用的方法
        // 假设所有方块纹理都是16x16像素，在64x48的纹理图集中
        float blockTexSizeU = 16.0/64.0; // 方块纹理在U方向的大小
        float blockTexSizeV = 16.0/48.0; // 方块纹理在V方向的大小
        
        // 计算TexCoord在其所属方块纹理中的局部位置
        vec2 blockTexStart = floor(TexCoord / vec2(blockTexSizeU, blockTexSizeV)) * vec2(blockTexSizeU, blockTexSizeV);
        localUV = (TexCoord - blockTexStart) / vec2(blockTexSizeU, blockTexSizeV);
        
        // 根据破坏进度选择破坏纹理（10-12号纹理）
        vec2 breakTexCoord;
        if (breakProgress < 0.33) {
            // 使用10号纹理 (第1阶段裂纹) - 位置(1,2)
            breakTexCoord = vec2(16.0/64.0, 32.0/48.0) + localUV * vec2(16.0/64.0, 16.0/48.0);
        } else if (breakProgress < 0.66) {
            // 使用11号纹理 (第2阶段裂纹) - 位置(2,2)
            breakTexCoord = vec2(32.0/64.0, 32.0/48.0) + localUV * vec2(16.0/64.0, 16.0/48.0);
        } else {
            // 使用12号纹理 (第3阶段裂纹) - 位置(3,2)
            breakTexCoord = vec2(48.0/64.0, 32.0/48.0) + localUV * vec2(16.0/64.0, 16.0/48.0);
        }
        
        vec4 breakTexColor = texture(ourTexture, breakTexCoord);
        // 混合破坏纹理
        if (breakTexColor.a > 0.1) {
            texColor.rgb = mix(texColor.rgb, breakTexColor.rgb, breakTexColor.a * 0.8);
        }
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
    
    // 检查是否是被选中的方块，提高亮度
    if (abs(FragPos.x - targetBlockPos.x) < 0.501 &&
        abs(FragPos.y - targetBlockPos.y) < 0.501 &&
        abs(FragPos.z - targetBlockPos.z) < 0.501 &&
        targetBlockPos.x > -900.0) { // 确保有有效的目标方块
        
        // 提高被选中方块的亮度
        result = result * 1.3; // 增加30%亮度
    }
    
    FragColor = vec4(result, texColor.a);
}
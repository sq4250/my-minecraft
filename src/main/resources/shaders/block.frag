#version 330 core
out vec4 FragColor;

in vec2 TexCoord;
in vec3 FragPos;
in float BlockType;

uniform sampler2D ourTexture;

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
        blockColorTint = vec3(0.1, 0.4, 1.0); // 蓝色调
        texColor.rgb = mix(texColor.rgb, texColor.rgb * blockColorTint, 0.8); // 混合80%的蓝色调
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
    
    // 检查是否是被选中的方块，提高亮度
    if (abs(FragPos.x - targetBlockPos.x) < 0.501 &&
        abs(FragPos.y - targetBlockPos.y) < 0.501 &&
        abs(FragPos.z - targetBlockPos.z) < 0.501 &&
        targetBlockPos.x > -900.0) { // 确保有有效的目标方块
        
        // 提高被选中方块的亮度
        texColor.rgb = texColor.rgb * 1.3; // 增加30%亮度
    }
    
    FragColor = vec4(texColor.rgb, texColor.a);
}
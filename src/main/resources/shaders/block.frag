#version 330 core
out vec4 FragColor;

in vec2 TexCoord;
in vec3 FragPos;
in float BlockType;
in vec3 Normal;     // 接收法线
in vec2 VertexCoord; // 接收顶点在面中的坐标
in float AOOcclusion; // 接收AO遮蔽值

uniform sampler2D ourTexture;
uniform vec3 lightDirection;  // 光照方向
uniform float ambientLight;   // 环境光强度

// 环境光遮蔽参数
uniform float aoStrength;     // AO强度 (0.0 - 1.0) - 增强边缘效果

// 破坏效果参数
uniform float breakProgress;  // 破坏进度 (0.0 - 1.0)
uniform vec3 targetBlockPos;  // 目标方块位置
uniform int targetFace;       // 目标面

// 计算平滑环境光遮蔽 (使用插值后的AO值)
float calculateSmoothAO(vec2 vertexCoord, float occlusion) {
    // 只在有遮蔽时才应用AO效果
    if (occlusion <= 0.0) {
        return 0.0;
    }
    
    // 计算顶点到最近边缘的距离
    float distToEdgeX = min(vertexCoord.x, 1.0 - vertexCoord.x);
    float distToEdgeY = min(vertexCoord.y, 1.0 - vertexCoord.y);
    float minDistToEdge = min(distToEdgeX, distToEdgeY);
    
    // 使用 smoothstep 创建平滑过渡
    // 在距离边缘 0.0-0.3 范围内应用 AO 效果
    float aoRange = 0.3;
    float aoFactor = 1.0 - smoothstep(0.0, aoRange, minDistToEdge);
    
    return aoFactor * occlusion;
}

void main() {
    // 获取纹理颜色
    vec4 texColor = texture(ourTexture, TexCoord);
    if(texColor.a < 0.1)
        discard;
    
    // 计算漫反射光照
    // 标准化法线和光线方向
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(-lightDirection);  // 负号是因为光照方向通常定义为从光源指向片段
    
    // 计算漫反射因子
    float diff = max(dot(norm, lightDir), 0.0);
    
    // 计算平滑环境光遮蔽 (使用插值后的AO值)
    float aoFactor = calculateSmoothAO(VertexCoord, AOOcclusion);
    float aoEffect = 1.0 - (aoFactor * aoStrength);
    
    // 计算总光照 (环境光 + 漫反射) * AO
    float lightingFactor = (ambientLight + (1.0 - ambientLight) * diff) * aoEffect;
    // 确保最小光照强度
    lightingFactor = max(lightingFactor, 0.1);
    
    // 根据方块类型调整颜色
    vec3 blockColorTint = vec3(1.0); // 默认无色调
    
    // 水方块 (ID = 7) 添加蓝色调
    if (int(BlockType) == 7) {
        blockColorTint = vec3(0.1, 0.4, 1.0); // 蓝色调
        texColor.rgb = mix(texColor.rgb, texColor.rgb * blockColorTint, 0.8); // 混合80%的蓝色调
    }
    
    // 应用光照
    vec3 lighting = lightingFactor * texColor.rgb;
    
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
            lighting = mix(lighting, breakTexColor.rgb, breakTexColor.a * 0.8);
        }
    }
    
    // 检查是否是被选中的方块，提高亮度
    if (abs(FragPos.x - targetBlockPos.x) < 0.501 &&
        abs(FragPos.y - targetBlockPos.y) < 0.501 &&
        abs(FragPos.z - targetBlockPos.z) < 0.501 &&
        targetBlockPos.x > -900.0) { // 确保有有效的目标方块
        
        // 提高被选中方块的亮度
        lighting = lighting * 1.3; // 增加30%亮度
    }
    
    FragColor = vec4(lighting, texColor.a);
}
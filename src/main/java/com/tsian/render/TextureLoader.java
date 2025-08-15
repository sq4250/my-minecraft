package com.tsian.render;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * 纹理加载工具类
 */
public class TextureLoader {
    
    /**
     * 从资源文件加载纹理
     * @param resourcePath 资源路径
     * @return 纹理ID
     */
    public static int loadTexture(String resourcePath) {
        int textureId;
        int width, height;
        ByteBuffer image;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            
            // 加载图像并获取宽度、高度和颜色通道数
            image = STBImage.stbi_load_from_memory(loadResource(resourcePath), w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture file: " + resourcePath + 
                    " - " + STBImage.stbi_failure_reason());
            }
            
            width = w.get();
            height = h.get();
        }
        
        // 创建纹理
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // 设置纹理参数
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // 上传纹理数据
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // 释放图像内存
        STBImage.stbi_image_free(image);
        
        return textureId;
    }
    
    /**
     * 从资源文件加载字节数据
     */
    private static ByteBuffer loadResource(String resourcePath) {
        try {
            var inputStream = TextureLoader.class.getResourceAsStream("/" + resourcePath);
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }
}
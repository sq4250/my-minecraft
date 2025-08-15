package com.tsian.render;

import com.tsian.world.World;
import com.tsian.world.Block;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 简化的渲染器 - 直接渲染预计算的可见面，无多余优化
 */
public class SimpleRenderer {
    
    private static final int FLOATS_PER_VERTEX = 5; // x,y,z,u,v
    
    // 渲染资源
    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;
    
    // 渲染数据
    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;
    
    public SimpleRenderer() {
        // 创建OpenGL资源
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        eboId = glGenBuffers();
        
        setupVertexArrayObject();
    }
    
    /**
     * 设置VAO属性
     */
    private void setupVertexArrayObject() {
        glBindVertexArray(vaoId);
        
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        // 位置属性 (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // 纹理坐标属性 (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    /**
     * 从世界数据构建渲染缓冲区（只在初始化时调用一次）
     */
    public void buildMeshFromWorld(World world) {
        List<World.VisibleFace> visibleFaces = world.getVisibleFaces();
        
        if (visibleFaces.isEmpty()) {
            vertexCount = 0;
            return;
        }
        
        // 计算所需缓冲区大小
        int totalVertices = visibleFaces.size() * 4; // 每面4个顶点
        int totalIndices = visibleFaces.size() * 6;  // 每面6个索引（2个三角形）
        
        // 分配缓冲区
        vertexBuffer = MemoryUtil.memAllocFloat(totalVertices * FLOATS_PER_VERTEX);
        indexBuffer = MemoryUtil.memAllocInt(totalIndices);
        
        int currentVertexOffset = 0;
        
        // 生成几何数据
        for (World.VisibleFace visibleFace : visibleFaces) {
            addFaceToBuffer(visibleFace.block, visibleFace.face, currentVertexOffset);
            currentVertexOffset += 4;
        }
        
        // 翻转缓冲区准备上传
        vertexBuffer.flip();
        indexBuffer.flip();
        
        // 上传到GPU
        uploadBuffersToGPU();
        
        vertexCount = totalIndices;
        
        System.out.println("Built mesh: " + totalVertices + " vertices, " + (totalIndices/3) + " triangles");
    }
    
    /**
     * 添加单个面的数据到缓冲区
     */
    private void addFaceToBuffer(Block block, int face, int vertexOffset) {
        float blockX = block.getX();
        float blockY = block.getY();
        float blockZ = block.getZ();
        
        // 获取面的顶点坐标
        float[][] vertices = getFaceVertices(blockX, blockY, blockZ, face);
        
        // 获取纹理坐标
        float[] texCoords = block.getTextureCoords(face);
        float u1 = texCoords[0], v1 = texCoords[1];
        float u2 = texCoords[2], v2 = texCoords[3];
        
        // 添加4个顶点数据
        for (int i = 0; i < 4; i++) {
            vertexBuffer.put(vertices[i][0]); // x
            vertexBuffer.put(vertices[i][1]); // y
            vertexBuffer.put(vertices[i][2]); // z
            
            // 纹理坐标
            float u = (i == 1 || i == 2) ? u2 : u1;
            float v = (i == 2 || i == 3) ? v1 : v2;
            vertexBuffer.put(u);
            vertexBuffer.put(v);
        }
        
        // 添加索引（2个三角形）
        indexBuffer.put(vertexOffset);
        indexBuffer.put(vertexOffset + 1);
        indexBuffer.put(vertexOffset + 2);
        
        indexBuffer.put(vertexOffset + 2);
        indexBuffer.put(vertexOffset + 3);
        indexBuffer.put(vertexOffset);
    }
    
    /**
     * 获取方块某个面的顶点坐标
     */
    private float[][] getFaceVertices(float x, float y, float z, int face) {
        float[][] vertices = new float[4][3];
        
        switch (face) {
            case 0: // 前面 (+Z)
                vertices[0] = new float[]{x - 0.5f, y - 0.5f, z + 0.5f}; // 左下
                vertices[1] = new float[]{x + 0.5f, y - 0.5f, z + 0.5f}; // 右下
                vertices[2] = new float[]{x + 0.5f, y + 0.5f, z + 0.5f}; // 右上
                vertices[3] = new float[]{x - 0.5f, y + 0.5f, z + 0.5f}; // 左上
                break;
            case 1: // 后面 (-Z)
                vertices[0] = new float[]{x + 0.5f, y - 0.5f, z - 0.5f}; // 左下
                vertices[1] = new float[]{x - 0.5f, y - 0.5f, z - 0.5f}; // 右下
                vertices[2] = new float[]{x - 0.5f, y + 0.5f, z - 0.5f}; // 右上
                vertices[3] = new float[]{x + 0.5f, y + 0.5f, z - 0.5f}; // 左上
                break;
            case 2: // 左面 (-X)
                vertices[0] = new float[]{x - 0.5f, y - 0.5f, z - 0.5f}; // 左下
                vertices[1] = new float[]{x - 0.5f, y - 0.5f, z + 0.5f}; // 右下
                vertices[2] = new float[]{x - 0.5f, y + 0.5f, z + 0.5f}; // 右上
                vertices[3] = new float[]{x - 0.5f, y + 0.5f, z - 0.5f}; // 左上
                break;
            case 3: // 右面 (+X)
                vertices[0] = new float[]{x + 0.5f, y - 0.5f, z + 0.5f}; // 左下
                vertices[1] = new float[]{x + 0.5f, y - 0.5f, z - 0.5f}; // 右下
                vertices[2] = new float[]{x + 0.5f, y + 0.5f, z - 0.5f}; // 右上
                vertices[3] = new float[]{x + 0.5f, y + 0.5f, z + 0.5f}; // 左上
                break;
            case 4: // 上面 (+Y)
                vertices[0] = new float[]{x - 0.5f, y + 0.5f, z + 0.5f}; // 左下
                vertices[1] = new float[]{x + 0.5f, y + 0.5f, z + 0.5f}; // 右下
                vertices[2] = new float[]{x + 0.5f, y + 0.5f, z - 0.5f}; // 右上
                vertices[3] = new float[]{x - 0.5f, y + 0.5f, z - 0.5f}; // 左上
                break;
            case 5: // 下面 (-Y)
                vertices[0] = new float[]{x - 0.5f, y - 0.5f, z - 0.5f}; // 左下
                vertices[1] = new float[]{x + 0.5f, y - 0.5f, z - 0.5f}; // 右下
                vertices[2] = new float[]{x + 0.5f, y - 0.5f, z + 0.5f}; // 右上
                vertices[3] = new float[]{x - 0.5f, y - 0.5f, z + 0.5f}; // 左上
                break;
        }
        
        return vertices;
    }
    
    /**
     * 上传缓冲区数据到GPU
     */
    private void uploadBuffersToGPU() {
        glBindVertexArray(vaoId);
        
        // 上传顶点数据
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        // 上传索引数据
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        glBindVertexArray(0);
    }
    
    /**
     * 渲染（每帧调用）
     */
    public void render(int textureId) {
        if (vertexCount == 0) return;
        
        glBindVertexArray(vaoId);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        
        glBindVertexArray(0);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        if (vboId != 0) glDeleteBuffers(vboId);
        if (eboId != 0) glDeleteBuffers(eboId);
        
        if (vertexBuffer != null) {
            MemoryUtil.memFree(vertexBuffer);
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            MemoryUtil.memFree(indexBuffer);
            indexBuffer = null;
        }
    }
}
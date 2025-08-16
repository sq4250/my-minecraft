package com.tsian.render;

import com.tsian.world.World;
import com.tsian.world.Block;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 改进的渲染器 - 支持透明方块的深度排序
 */
public class SimpleRenderer {
    
    private static final int FLOATS_PER_VERTEX = 6; // x,y,z,u,v,blockType
    
    // 渲染资源 - 不透明方块
    private int opaqueVaoId;
    private int opaqueVboId;
    private int opaqueEboId;
    private int opaqueVertexCount;
    
    // 渲染资源 - 透明方块
    private int transparentVaoId;
    private int transparentVboId;
    private int transparentEboId;
    private int transparentVertexCount;
    
    // 渲染数据
    private FloatBuffer opaqueVertexBuffer;
    private IntBuffer opaqueIndexBuffer;
    private FloatBuffer transparentVertexBuffer;
    private IntBuffer transparentIndexBuffer;
    
    public SimpleRenderer() {
        // 创建不透明方块的OpenGL资源
        opaqueVaoId = glGenVertexArrays();
        opaqueVboId = glGenBuffers();
        opaqueEboId = glGenBuffers();
        
        // 创建透明方块的OpenGL资源
        transparentVaoId = glGenVertexArrays();
        transparentVboId = glGenBuffers();
        transparentEboId = glGenBuffers();
        
        setupVertexArrayObjects();
    }
    
    /**
     * 设置VAO属性
     */
    private void setupVertexArrayObjects() {
        setupSingleVAO(opaqueVaoId, opaqueVboId, opaqueEboId);
        setupSingleVAO(transparentVaoId, transparentVboId, transparentEboId);
    }
    
    private void setupSingleVAO(int vaoId, int vboId, int eboId) {
        glBindVertexArray(vaoId);
        
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        // 位置属性 (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // 纹理坐标属性 (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // 方块类型属性 (location = 2)
        glVertexAttribPointer(2, 1, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        glBindVertexArray(0);
    }
    
    /**
     * 从世界数据构建渲染缓冲区（只在初始化时调用一次）
     */
    public void buildMeshFromWorld(World world) {
        List<World.VisibleFace> visibleFaces = world.getVisibleFaces();
        
        if (visibleFaces.isEmpty()) {
            opaqueVertexCount = 0;
            transparentVertexCount = 0;
            return;
        }
        
        // 分离不透明和透明面
        List<World.VisibleFace> opaqueFaces = new ArrayList<>();
        List<World.VisibleFace> transparentFaces = new ArrayList<>();
        
        for (World.VisibleFace face : visibleFaces) {
            if (face.block.getType() == Block.BlockType.WATER || 
                face.block.getType() == Block.BlockType.LEAVES) {
                transparentFaces.add(face);
            } else {
                opaqueFaces.add(face);
            }
        }
        
        // 构建不透明面缓冲区
        buildOpaqueBuffer(opaqueFaces);
        
        // 构建透明面缓冲区（需要深度排序）
        buildTransparentBuffer(transparentFaces);
        
        System.out.println("Built mesh: " + opaqueFaces.size() + " opaque faces, " + 
                          transparentFaces.size() + " transparent faces");
    }
    
    /**
     * 构建不透明方块缓冲区
     */
    private void buildOpaqueBuffer(List<World.VisibleFace> opaqueFaces) {
        if (opaqueFaces.isEmpty()) {
            opaqueVertexCount = 0;
            return;
        }
        
        int totalVertices = opaqueFaces.size() * 4;
        int totalIndices = opaqueFaces.size() * 6;
        
        opaqueVertexBuffer = MemoryUtil.memAllocFloat(totalVertices * FLOATS_PER_VERTEX);
        opaqueIndexBuffer = MemoryUtil.memAllocInt(totalIndices);
        
        int currentVertexOffset = 0;
        for (World.VisibleFace face : opaqueFaces) {
            addFaceToBuffer(face.block, face.face, currentVertexOffset, opaqueVertexBuffer, opaqueIndexBuffer);
            currentVertexOffset += 4;
        }
        
        opaqueVertexBuffer.flip();
        opaqueIndexBuffer.flip();
        
        uploadBuffersToGPU(opaqueVaoId, opaqueVboId, opaqueEboId, opaqueVertexBuffer, opaqueIndexBuffer);
        opaqueVertexCount = totalIndices;
    }
    
    // 存储透明面数据用于动态排序
    private List<World.VisibleFace> transparentFaces;
    
    /**
     * 构建透明方块缓冲区（不进行排序，留到渲染时动态排序）
     */
    private void buildTransparentBuffer(List<World.VisibleFace> transparentFaces) {
        this.transparentFaces = new ArrayList<>(transparentFaces); // 保存透明面数据
        
        if (transparentFaces.isEmpty()) {
            transparentVertexCount = 0;
            return;
        }
        
        int totalVertices = transparentFaces.size() * 4;
        int totalIndices = transparentFaces.size() * 6;
        
        // 预分配最大缓冲区大小
        transparentVertexBuffer = MemoryUtil.memAllocFloat(totalVertices * FLOATS_PER_VERTEX);
        transparentIndexBuffer = MemoryUtil.memAllocInt(totalIndices);
        
        // 初始时不上传数据，在渲染时动态生成
        transparentVertexCount = totalIndices;
    }
    
    /**
     * 根据摄像头位置重新排序并更新透明方块缓冲区
     */
    private void updateTransparentBuffer(float cameraX, float cameraY, float cameraZ) {
        if (transparentFaces == null || transparentFaces.isEmpty()) {
            return;
        }
        
        // 按距离摄像头的距离排序（从远到近）
        transparentFaces.sort(new Comparator<World.VisibleFace>() {
            @Override
            public int compare(World.VisibleFace a, World.VisibleFace b) {
                // 计算面的中心点
                float centerAX = a.block.getX();
                float centerAY = a.block.getY();
                float centerAZ = a.block.getZ();
                
                float centerBX = b.block.getX();
                float centerBY = b.block.getY();
                float centerBZ = b.block.getZ();
                
                // 计算到摄像头的距离平方
                float distA = (centerAX - cameraX) * (centerAX - cameraX) +
                             (centerAY - cameraY) * (centerAY - cameraY) +
                             (centerAZ - cameraZ) * (centerAZ - cameraZ);
                             
                float distB = (centerBX - cameraX) * (centerBX - cameraX) +
                             (centerBY - cameraY) * (centerBY - cameraY) +
                             (centerBZ - cameraZ) * (centerBZ - cameraZ);
                
                return Float.compare(distB, distA); // 从远到近
            }
        });
        
        // 重新生成缓冲区数据
        transparentVertexBuffer.clear();
        transparentIndexBuffer.clear();
        
        int currentVertexOffset = 0;
        for (World.VisibleFace face : transparentFaces) {
            addFaceToBuffer(face.block, face.face, currentVertexOffset, transparentVertexBuffer, transparentIndexBuffer);
            currentVertexOffset += 4;
        }
        
        transparentVertexBuffer.flip();
        transparentIndexBuffer.flip();
        
        // 重新上传到GPU
        uploadBuffersToGPU(transparentVaoId, transparentVboId, transparentEboId, transparentVertexBuffer, transparentIndexBuffer);
    }
    
    /**
     * 添加单个面的数据到指定缓冲区
     */
    private void addFaceToBuffer(Block block, int face, int vertexOffset, FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
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
            
            // 方块类型ID
            vertexBuffer.put((float) block.getType().getId());
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
    private void uploadBuffersToGPU(int vaoId, int vboId, int eboId, FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
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
    public void render(int textureId, float cameraX, float cameraY, float cameraZ) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // 先渲染不透明方块（启用背面剔除）
        if (opaqueVertexCount > 0) {
            glEnable(GL_CULL_FACE);
            glBindVertexArray(opaqueVaoId);
            glDrawElements(GL_TRIANGLES, opaqueVertexCount, GL_UNSIGNED_INT, 0);
        }
        
        // 更新并渲染透明方块（禁用背面剔除）
        if (transparentVertexCount > 0) {
            // 每帧重新排序透明方块
            updateTransparentBuffer(cameraX, cameraY, cameraZ);
            
            glDisable(GL_CULL_FACE);
            glBindVertexArray(transparentVaoId);
            glDrawElements(GL_TRIANGLES, transparentVertexCount, GL_UNSIGNED_INT, 0);
            glEnable(GL_CULL_FACE); // 恢复背面剔除
        }
        
        glBindVertexArray(0);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (opaqueVaoId != 0) glDeleteVertexArrays(opaqueVaoId);
        if (opaqueVboId != 0) glDeleteBuffers(opaqueVboId);
        if (opaqueEboId != 0) glDeleteBuffers(opaqueEboId);
        
        if (transparentVaoId != 0) glDeleteVertexArrays(transparentVaoId);
        if (transparentVboId != 0) glDeleteBuffers(transparentVboId);
        if (transparentEboId != 0) glDeleteBuffers(transparentEboId);
        
        if (opaqueVertexBuffer != null) {
            MemoryUtil.memFree(opaqueVertexBuffer);
            opaqueVertexBuffer = null;
        }
        if (opaqueIndexBuffer != null) {
            MemoryUtil.memFree(opaqueIndexBuffer);
            opaqueIndexBuffer = null;
        }
        if (transparentVertexBuffer != null) {
            MemoryUtil.memFree(transparentVertexBuffer);
            transparentVertexBuffer = null;
        }
        if (transparentIndexBuffer != null) {
            MemoryUtil.memFree(transparentIndexBuffer);
            transparentIndexBuffer = null;
        }
    }
    
}
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
    
    private static final int FLOATS_PER_VERTEX = 12; // x,y,z,u,v,blockType,nx,ny,nz,vertexCoordX,vertexCoordY,aoOcclusion
    
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
    
    // 世界引用（用于AO计算）
    private World world;
    
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
        
        // 法线属性 (location = 3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(3);
        
        // 顶点在面中的坐标 (location = 4)
        glVertexAttribPointer(4, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 9 * Float.BYTES);
        glEnableVertexAttribArray(4);
        
        // AO遮蔽值 (location = 5)
        glVertexAttribPointer(5, 1, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 11 * Float.BYTES);
        glEnableVertexAttribArray(5);
        
        glBindVertexArray(0);
    }
    
    /**
     * 从世界数据构建渲染缓冲区（只在初始化时调用一次）
     */
    public void buildMeshFromWorld(World world) {
        this.world = world; // 保存world引用用于AO计算
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
        
        // 计算AO遮蔽值 - 每个顶点独立计算
        float[] aoOcclusions = calculateVertexAOOcclusion(block, face);
        
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
            
            // 法线向量 (根据面的方向确定)
            float[] normal = getFaceNormal(face);
            vertexBuffer.put(normal[0]); // nx
            vertexBuffer.put(normal[1]); // ny
            vertexBuffer.put(normal[2]); // nz
            
            // 顶点在面中的坐标 (0-1)
            float vertexCoordX = 0.0f, vertexCoordY = 0.0f;
            switch (i) {
                case 0: // 左下角
                    vertexCoordX = 0.0f;
                    vertexCoordY = 0.0f;
                    break;
                case 1: // 右下角
                    vertexCoordX = 1.0f;
                    vertexCoordY = 0.0f;
                    break;
                case 2: // 右上角
                    vertexCoordX = 1.0f;
                    vertexCoordY = 1.0f;
                    break;
                case 3: // 左上角
                    vertexCoordX = 0.0f;
                    vertexCoordY = 1.0f;
                    break;
            }
            vertexBuffer.put(vertexCoordX); // vertexCoordX
            vertexBuffer.put(vertexCoordY); // vertexCoordY
            vertexBuffer.put(aoOcclusions[i]);   // AO遮蔽值
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
     * 修复：统一坐标系统，方块整数坐标(x,y,z)对应世界坐标(x,y,z)到(x+1,y+1,z+1)
     */
    private float[][] getFaceVertices(float x, float y, float z, int face) {
        float[][] vertices = new float[4][3];
        
        switch (face) {
            case 0: // 前面 (+Z)
                vertices[0] = new float[]{x, y, z + 1.0f}; // 左下
                vertices[1] = new float[]{x + 1.0f, y, z + 1.0f}; // 右下
                vertices[2] = new float[]{x + 1.0f, y + 1.0f, z + 1.0f}; // 右上
                vertices[3] = new float[]{x, y + 1.0f, z + 1.0f}; // 左上
                break;
            case 1: // 后面 (-Z)
                vertices[0] = new float[]{x + 1.0f, y, z}; // 左下
                vertices[1] = new float[]{x, y, z}; // 右下
                vertices[2] = new float[]{x, y + 1.0f, z}; // 右上
                vertices[3] = new float[]{x + 1.0f, y + 1.0f, z}; // 左上
                break;
            case 2: // 左面 (-X)
                vertices[0] = new float[]{x, y, z}; // 左下
                vertices[1] = new float[]{x, y, z + 1.0f}; // 右下
                vertices[2] = new float[]{x, y + 1.0f, z + 1.0f}; // 右上
                vertices[3] = new float[]{x, y + 1.0f, z}; // 左上
                break;
            case 3: // 右面 (+X)
                vertices[0] = new float[]{x + 1.0f, y, z + 1.0f}; // 左下
                vertices[1] = new float[]{x + 1.0f, y, z}; // 右下
                vertices[2] = new float[]{x + 1.0f, y + 1.0f, z}; // 右上
                vertices[3] = new float[]{x + 1.0f, y + 1.0f, z + 1.0f}; // 左上
                break;
            case 4: // 上面 (+Y)
                vertices[0] = new float[]{x, y + 1.0f, z + 1.0f}; // 左下
                vertices[1] = new float[]{x + 1.0f, y + 1.0f, z + 1.0f}; // 右下
                vertices[2] = new float[]{x + 1.0f, y + 1.0f, z}; // 右上
                vertices[3] = new float[]{x, y + 1.0f, z}; // 左上
                break;
            case 5: // 下面 (-Y)
                vertices[0] = new float[]{x, y, z}; // 左下
                vertices[1] = new float[]{x + 1.0f, y, z}; // 右下
                vertices[2] = new float[]{x + 1.0f, y, z + 1.0f}; // 右上
                vertices[3] = new float[]{x, y, z + 1.0f}; // 左上
                break;
        }
        
        return vertices;
    }
    
    /**
     * 获取方块某个面的法线向量
     */
    private float[] getFaceNormal(int face) {
        switch (face) {
            case 0: // 前面 (+Z)
                return new float[]{0, 0, 1};
            case 1: // 后面 (-Z)
                return new float[]{0, 0, -1};
            case 2: // 左面 (-X)
                return new float[]{-1, 0, 0};
            case 3: // 右面 (+X)
                return new float[]{1, 0, 0};
            case 4: // 上面 (+Y)
                return new float[]{0, 1, 0};
            case 5: // 下面 (-Y)
                return new float[]{0, -1, 0};
            default:
                return new float[]{0, 0, 1}; // 默认正面
        }
    }
    
    
    /**
     * 计算面顶点的AO遮蔽值
     * 使用Minecraft风格的环境光遮蔽算法
     */
    private float[] calculateVertexAOOcclusion(Block block, int face) {
        if (world == null) return new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        
        // 根据面的类型计算四个顶点的AO值
        float[] aoValues = new float[4];
        
        switch (face) {
            case 0: // 前面 (+Z)
                // 左下角 (0,0) - 检查面上方一格周围的方块
                aoValues[0] = calculateVertexAO(x-1, y, z+1, x, y-1, z+1, x-1, y-1, z+1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x+1, y, z+1, x, y-1, z+1, x+1, y-1, z+1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x+1, y, z+1, x, y+1, z+1, x+1, y+1, z+1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x-1, y, z+1, x, y+1, z+1, x-1, y+1, z+1);
                break;
            case 1: // 后面 (-Z)
                // 左下角 (0,0)
                aoValues[0] = calculateVertexAO(x+1, y, z-1, x, y-1, z-1, x+1, y-1, z-1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x-1, y, z-1, x, y-1, z-1, x-1, y-1, z-1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x-1, y, z-1, x, y+1, z-1, x-1, y+1, z-1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x+1, y, z-1, x, y+1, z-1, x+1, y+1, z-1);
                break;
            case 2: // 左面 (-X)
                // 左下角 (0,0)
                aoValues[0] = calculateVertexAO(x-1, y, z-1, x-1, y-1, z, x-1, y-1, z-1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x-1, y, z+1, x-1, y-1, z, x-1, y-1, z+1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x-1, y, z+1, x-1, y+1, z, x-1, y+1, z+1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x-1, y, z-1, x-1, y+1, z, x-1, y+1, z-1);
                break;
            case 3: // 右面 (+X)
                // 左下角 (0,0)
                aoValues[0] = calculateVertexAO(x+1, y, z+1, x+1, y-1, z, x+1, y-1, z+1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x+1, y, z-1, x+1, y-1, z, x+1, y-1, z-1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x+1, y, z-1, x+1, y+1, z, x+1, y+1, z-1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x+1, y, z+1, x+1, y+1, z, x+1, y+1, z+1);
                break;
            case 4: // 上面 (+Y)
                // 左下角 (0,0)
                aoValues[0] = calculateVertexAO(x-1, y+1, z, x, y+1, z+1, x-1, y+1, z+1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x+1, y+1, z, x, y+1, z+1, x+1, y+1, z+1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x+1, y+1, z, x, y+1, z-1, x+1, y+1, z-1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x-1, y+1, z, x, y+1, z-1, x-1, y+1, z-1);
                break;
            case 5: // 下面 (-Y)
                // 左下角 (0,0)
                aoValues[0] = calculateVertexAO(x-1, y-1, z, x, y-1, z-1, x-1, y-1, z-1);
                // 右下角 (1,0)
                aoValues[1] = calculateVertexAO(x+1, y-1, z, x, y-1, z-1, x+1, y-1, z-1);
                // 右上角 (1,1)
                aoValues[2] = calculateVertexAO(x+1, y-1, z, x, y-1, z+1, x+1, y-1, z+1);
                // 左上角 (0,1)
                aoValues[3] = calculateVertexAO(x-1, y-1, z, x, y-1, z+1, x-1, y-1, z+1);
                break;
        }
        
        return aoValues;
    }
    
    /**
     * 计算单个顶点的AO值
     * 改进的Minecraft风格AO算法，创建更平滑的圆形阴影效果
     * 检查周围9个位置的方块来计算更精确的AO值
     */
    private float calculateVertexAO(int side1X, int side1Y, int side1Z,
                                     int side2X, int side2Y, int side2Z,
                                     int side3X, int side3Y, int side3Z) {
        // 检查三个相邻方向的方块
        Block side1Block = world.getBlockAt(side1X, side1Y, side1Z);
        Block side2Block = world.getBlockAt(side2X, side2Y, side2Z);
        Block side3Block = world.getBlockAt(side3X, side3Y, side3Z);
        
        // 计算遮蔽值 - 为边缘中心创建更暗的效果，减少角落影响
        int side1Solid = (side1Block != null && side1Block.isSolid()) ? 1 : 0; // 边缘方向1
        int side2Solid = (side2Block != null && side2Block.isSolid()) ? 1 : 0; // 边缘方向2
        int side3Solid = (side3Block != null && side3Block.isSolid()) ? 1 : 0; // 对角线方向
        
        // 改进的Minecraft风格AO计算 - 创建更平滑的过渡效果
        // 当两个边缘方向都被遮挡时，增加遮蔽强度以创建圆形阴影
        if (side1Solid == 1 && side2Solid == 1) {
            // 两个边缘都被遮挡，创建更强的角落阴影
            return 0.5f; // 强遮蔽
        } else if (side1Solid == 1 || side2Solid == 1) {
            // 只有一个边缘被遮挡
            return 0.25f; // 中等遮蔽
        } else if (side3Solid == 1) {
            // 只有对角线被遮挡
            return 0.1f; // 轻微遮蔽
        } else {
            // 没有遮挡
            return 0.0f; // 无遮蔽
        }
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
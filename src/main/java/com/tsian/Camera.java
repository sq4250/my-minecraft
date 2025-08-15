package com.tsian;

import static java.lang.Math.*;

/**
 * 摄像头类 - 管理摄像头位置、方向和观察矩阵
 */
public class Camera {
    
    // 摄像头位置
    private float x, y, z;
    
    // 摄像头方向 (欧拉角)
    private float yaw;   // 偏航角 (绕Y轴旋转)
    private float pitch; // 俯仰角 (绕X轴旋转)
    
    // 摄像头参数
    private float moveSpeed = 5.0f;
    private float mouseSensitivity = 0.1f;
    
    // 摄像头向量
    private float[] front = new float[3];
    private float[] right = new float[3];
    private float[] up = {0.0f, 1.0f, 0.0f};
    
    public Camera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = -90.0f;  // 初始方向朝向-Z轴
        this.pitch = 0.0f;
        updateCameraVectors();
    }
    
    /**
     * 获取观察矩阵 (View Matrix)
     */
    public float[] getViewMatrix() {
        // 计算目标点
        float targetX = x + front[0];
        float targetY = y + front[1];
        float targetZ = z + front[2];
        
        return lookAt(x, y, z, targetX, targetY, targetZ, up[0], up[1], up[2]);
    }
    
    /**
     * 处理键盘输入移动
     */
    public void processKeyboardInput(boolean forward, boolean backward, boolean left, boolean right,
                                   boolean up, boolean down, float deltaTime) {
        float velocity = moveSpeed * deltaTime;
        
        // 计算水平前向向量 (只在XZ平面移动，忽略Y分量)
        float[] horizontalFront = new float[3];
        horizontalFront[0] = (float) cos(toRadians(yaw));
        horizontalFront[1] = 0.0f;  // 水平移动时Y分量为0
        horizontalFront[2] = (float) sin(toRadians(yaw));
        normalize(horizontalFront);
        
        // 计算水平右向向量
        float[] horizontalRight = new float[3];
        cross(horizontalFront, new float[]{0.0f, 1.0f, 0.0f}, horizontalRight);
        normalize(horizontalRight);
        
        if (forward) {
            x += horizontalFront[0] * velocity;
            z += horizontalFront[2] * velocity;
        }
        if (backward) {
            x -= horizontalFront[0] * velocity;
            z -= horizontalFront[2] * velocity;
        }
        if (left) {
            x -= horizontalRight[0] * velocity;
            z -= horizontalRight[2] * velocity;
        }
        if (right) {
            x += horizontalRight[0] * velocity;
            z += horizontalRight[2] * velocity;
        }
        if (up) {
            y += velocity;
        }
        if (down) {
            y -= velocity;
        }
    }
    
    /**
     * 处理鼠标输入 (暂时不实现，保留接口)
     */
    public void processMouseMovement(float xoffset, float yoffset) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;
        
        yaw += xoffset;
        pitch += yoffset;
        
        // 限制俯仰角
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
        
        updateCameraVectors();
    }
    
    /**
     * 更新摄像头向量
     */
    private void updateCameraVectors() {
        // 计算新的前向量
        front[0] = (float) (cos(toRadians(yaw)) * cos(toRadians(pitch)));
        front[1] = (float) sin(toRadians(pitch));
        front[2] = (float) (sin(toRadians(yaw)) * cos(toRadians(pitch)));
        
        // 归一化前向量
        normalize(front);
        
        // 计算右向量
        cross(front, up, right);
        normalize(right);
    }
    
    /**
     * 创建观察矩阵 (LookAt)
     */
    private float[] lookAt(float eyeX, float eyeY, float eyeZ,
                          float centerX, float centerY, float centerZ,
                          float upX, float upY, float upZ) {
        
        float[] f = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
        normalize(f);
        
        float[] u = {upX, upY, upZ};
        normalize(u);
        
        float[] s = new float[3];
        cross(f, u, s);
        normalize(s);
        
        cross(s, f, u);
        
        float[] result = new float[16];
        
        result[0] = s[0];
        result[1] = u[0];
        result[2] = -f[0];
        result[3] = 0.0f;
        
        result[4] = s[1];
        result[5] = u[1];
        result[6] = -f[1];
        result[7] = 0.0f;
        
        result[8] = s[2];
        result[9] = u[2];
        result[10] = -f[2];
        result[11] = 0.0f;
        
        result[12] = -dot(s, new float[]{eyeX, eyeY, eyeZ});
        result[13] = -dot(u, new float[]{eyeX, eyeY, eyeZ});
        result[14] = dot(f, new float[]{eyeX, eyeY, eyeZ});
        result[15] = 1.0f;
        
        return result;
    }
    
    /**
     * 创建透视投影矩阵
     */
    public static float[] perspective(float fovy, float aspect, float near, float far) {
        float f = (float) (1.0 / tan(toRadians(fovy) / 2.0));
        
        float[] result = new float[16];
        
        result[0] = f / aspect;
        result[1] = 0.0f;
        result[2] = 0.0f;
        result[3] = 0.0f;
        
        result[4] = 0.0f;
        result[5] = f;
        result[6] = 0.0f;
        result[7] = 0.0f;
        
        result[8] = 0.0f;
        result[9] = 0.0f;
        result[10] = (far + near) / (near - far);
        result[11] = -1.0f;
        
        result[12] = 0.0f;
        result[13] = 0.0f;
        result[14] = (2.0f * far * near) / (near - far);
        result[15] = 0.0f;
        
        return result;
    }
    
    // 辅助数学函数
    private void normalize(float[] vec) {
        float length = (float) sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
        if (length > 0) {
            vec[0] /= length;
            vec[1] /= length;
            vec[2] /= length;
        }
    }
    
    private void cross(float[] a, float[] b, float[] result) {
        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];
    }
    
    private float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
    
    // Getter方法
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    
    /**
     * 获取摄像头前向量
     */
    public float[] getFrontVector() {
        return new float[]{front[0], front[1], front[2]};
    }
}
package com.phonecl.armeasure.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.phonecl.armeasure.presenter.MeasurePresenter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ARRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "ARRenderer";

    private MeasurePresenter presenter;
    private Context context;
    
    private int cameraTextureId = -1;
    private int cameraProgram = -1;
    
    private int uMvpMatrixHandle;
    private int uTextureHandle;
    private int aPositionHandle;
    private int aTexCoordHandle;
    
    private float[] orthoMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    
    private int viewportWidth;
    private int viewportHeight;
    private int displayRotation;
    private boolean isInitialized = false;
    
    private float pendingTouchX = -1;
    private float pendingTouchY = -1;
    private boolean hasPendingTouch = false;

    public interface RendererCallback {
        void onSurfaceCreated();
        void onFrameUpdated(int planeCount);
    }
    
    private RendererCallback callback;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMvpMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMvpMatrix * aPosition;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";
    
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}\n";

    public ARRenderer(MeasurePresenter presenter, Context context) {
        this.presenter = presenter;
        this.context = context;
    }

    public void setCallback(RendererCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        cameraTextureId = createCameraTexture();
        Log.i(TAG, "Camera texture ID: " + cameraTextureId);
        
        if (!initCameraProgram()) {
            Log.e(TAG, "Failed to initialize camera program");
            return;
        }
        
        initBuffers();
        
        isInitialized = true;
        
        if (presenter != null && cameraTextureId >= 0) {
            presenter.setCameraTextureName(cameraTextureId);
        }
        
        if (presenter != null) {
            presenter.initAROnGLThread();
        }
        
        if (callback != null) {
            callback.onSurfaceCreated();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        GLES20.glViewport(0, 0, width, height);
        
        displayRotation = getDisplayRotation();
        Log.i(TAG, "Surface changed: " + width + "x" + height + ", rotation: " + displayRotation);
        
        updateProjection();
        
        if (presenter != null) {
            presenter.setDisplayGeometry(displayRotation, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        if (!isInitialized || cameraProgram < 0) {
            Log.w(TAG, "Renderer not initialized");
            return;
        }
        
        if (presenter == null || !presenter.isARReady()) {
            return;
        }
        
        try {
            Frame frame = presenter.updateARFrame();
            if (frame == null) {
                Log.w(TAG, "Frame is null");
                return;
            }
            
            Camera camera = frame.getCamera();
            if (camera == null) {
                Log.w(TAG, "Camera is null");
                return;
            }
            
            drawCameraBackground();
            
            processPendingTouch();
            
            int planeCount = presenter.getTrackedPlaneCount(frame);
            if (callback != null && planeCount > 0) {
                callback.onFrameUpdated(planeCount);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }

    private void updateProjection() {
        float screenAspect = (float) viewportWidth / viewportHeight;
        float cameraAspect = 1080.0f / 1920.0f;
        
        float left = -1.0f;
        float right = 1.0f;
        float bottom = -1.0f;
        float top = 1.0f;
        
        if (cameraAspect > screenAspect) {
            float scale = cameraAspect / screenAspect;
            bottom = -scale;
            top = scale;
        } else {
            float scale = screenAspect / cameraAspect;
            left = -scale;
            right = scale;
        }
        
        Matrix.orthoM(orthoMatrix, 0, left, right, bottom, top, -1, 1);
        Log.i(TAG, "Projection updated - screen: " + screenAspect + ", camera: " + cameraAspect);
    }

    private void drawCameraBackground() {
        GLES20.glDepthMask(false);
        
        GLES20.glUseProgram(cameraProgram);
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glUniform1i(uTextureHandle, 0);
        
        updateTextureMatrix();
        GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        
        GLES20.glEnableVertexAttribArray(aTexCoordHandle);
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        
        GLES20.glDepthMask(true);
    }

    private void updateTextureMatrix() {
        float[] rotationMatrix = new float[16];
        
        switch (displayRotation) {
            case 0:
                Matrix.setRotateM(rotationMatrix, 0, 90, 0, 0, 1);
                break;
            case 1:
                Matrix.setIdentityM(rotationMatrix, 0);
                break;
            case 2:
                Matrix.setRotateM(rotationMatrix, 0, -90, 0, 0, 1);
                break;
            case 3:
                Matrix.setRotateM(rotationMatrix, 0, 180, 0, 0, 1);
                break;
            default:
                Matrix.setRotateM(rotationMatrix, 0, 90, 0, 0, 1);
                break;
        }
        
        Matrix.multiplyMM(mvpMatrix, 0, orthoMatrix, 0, rotationMatrix, 0);
    }

    public void queueTouchEvent(float x, float y) {
        synchronized (this) {
            pendingTouchX = x;
            pendingTouchY = y;
            hasPendingTouch = true;
            Log.d(TAG, "Touch event queued: (" + x + ", " + y + ")");
        }
    }

    private void processPendingTouch() {
        synchronized (this) {
            if (!hasPendingTouch) return;
            
            float x = pendingTouchX;
            float y = pendingTouchY;
            hasPendingTouch = false;
            pendingTouchX = -1;
            pendingTouchY = -1;
            
            if (presenter != null) {
                presenter.handleTouch(x, y);
            }
        }
    }

    private boolean initCameraProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        
        if (vertexShader == 0 || fragmentShader == 0) {
            return false;
        }
        
        cameraProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cameraProgram, vertexShader);
        GLES20.glAttachShader(cameraProgram, fragmentShader);
        GLES20.glLinkProgram(cameraProgram);
        
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(cameraProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(cameraProgram);
            Log.e(TAG, "Program link error: " + error);
            GLES20.glDeleteProgram(cameraProgram);
            cameraProgram = -1;
            return false;
        }
        
        uMvpMatrixHandle = GLES20.glGetUniformLocation(cameraProgram, "uMvpMatrix");
        uTextureHandle = GLES20.glGetUniformLocation(cameraProgram, "uTexture");
        aPositionHandle = GLES20.glGetAttribLocation(cameraProgram, "aPosition");
        aTexCoordHandle = GLES20.glGetAttribLocation(cameraProgram, "aTexCoord");
        
        return true;
    }

    private void initBuffers() {
        float[] vertices = {
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f
        };
        
        float[] texCoords = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private int createCameraTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        
        if (textures[0] == 0) {
            Log.e(TAG, "Failed to generate texture");
            return -1;
        }
        
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        
        return textures[0];
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        
        if (compileStatus[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compile error: " + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        
        return shader;
    }

    private int getDisplayRotation() {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 1;
            case Surface.ROTATION_180: return 2;
            case Surface.ROTATION_270: return 3;
            default: return 0;
        }
    }

    public int getCameraTextureId() {
        return cameraTextureId;
    }
}
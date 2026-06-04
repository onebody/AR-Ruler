package com.phonecl.armeasure;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.phonecl.armeasure.presenter.MeasurePresenter;
import com.phonecl.armeasure.render.ARRenderer;

import android.opengl.GLSurfaceView;

/**
 * MainActivity - AR 测距测高主界面
 *
 * 修复内容（2026-06-03）：
 * 1. onSurfaceCreated 回调中触发 AR 初始化，解决点击无效问题
 * 2. 生命周期正确管理 AR session（resume/pause/close）
 * 3. 点击坐标正确传递给 presenter
 */
public class MainActivity extends AppCompatActivity
        implements com.phonecl.armeasure.presenter.MeasureContract.View,
                   ARRenderer.RendererCallback {

    private static final String TAG = "ARMeasure";
    private static final int REQUEST_CAMERA = 100;

    private GLSurfaceView glSurfaceView;
    private MeasurePresenter presenter;
    private ARRenderer renderer;

    // UI 控件
    private TextView tvResult;
    private TextView tvMode;
    private TextView tvGuide;
    private TextView tvPointStart;
    private TextView tvPointEnd;
    private TextView tvAccuracy;
    private TextView tvAdvice;
    private Button btnSwitchMode;
    private Button btnReset;
    private Button btnUnit;
    private Button btnSave;
    private ImageView ivSettings;

    private boolean permissionGranted = false;
    private boolean surfaceCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();       // 先加载布局，拿到所有 View 引用
        checkCameraPermission();   // 请求权限（权限通过后由 onRequestPermissionsResult 触发 initAR）
        initAR();            // 初始化 AR（内部会判断 permissionGranted）
    }

    private void initViews() {
        glSurfaceView   = findViewById(R.id.gl_surface_view);
        tvResult       = findViewById(R.id.tv_result);
        tvMode         = findViewById(R.id.tv_mode);
        tvGuide        = findViewById(R.id.tv_guide);
        tvPointStart   = findViewById(R.id.tv_point_start);
        tvPointEnd     = findViewById(R.id.tv_point_end);
        tvAccuracy     = findViewById(R.id.tv_accuracy);
        tvAdvice       = findViewById(R.id.tv_advice);
        btnSwitchMode  = findViewById(R.id.btn_switch_mode);
        btnReset       = findViewById(R.id.btn_reset);
        btnUnit        = findViewById(R.id.btn_unit);
        btnSave        = findViewById(R.id.btn_save);
        ivSettings     = findViewById(R.id.iv_settings);

        // 设置按钮点击事件
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.phonecl.armeasure.settings.SettingsActivity.class);
            startActivity(intent);
        });

        // 防御：确认 GLSurfaceView 不为 null
        if (glSurfaceView == null) {
            Log.e(TAG, "initViews: glSurfaceView is NULL! Check R.layout.activity_main ID.");
            return;
        }

        glSurfaceView.setEGLContextClientVersion(2);

        // 创建 presenter & renderer（必须在 setRenderer 之前）
        presenter = new MeasurePresenter(this);
        presenter.attachView(this);

        renderer = new ARRenderer(presenter, this);
        renderer.setCallback(this);
        glSurfaceView.setRenderer(renderer);
        // setRenderMode 必须在 setRenderer 之后调用
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 按钮事件
        btnSwitchMode.setOnClickListener(v -> presenter.switchMode());
        btnReset.setOnClickListener(v -> presenter.resetMeasurement());
        btnUnit.setOnClickListener(v -> presenter.toggleUnit());
        btnSave.setOnClickListener(v -> presenter.saveMeasurement());

        // 触摸事件 → 测距取点（通过 renderer 传递到 GL 线程处理，使用屏幕中心点）
        glSurfaceView.setOnTouchListener((v, event) -> {
            Log.i(TAG, "Touch event: action=" + event.getAction() + ", x=" + event.getX() + ", y=" + event.getY());
            if (event.getAction() == MotionEvent.ACTION_UP && renderer != null) {
                // 使用屏幕中心点作为取点位置（十字架标记位置）
                float centerX = v.getWidth() / 2.0f;
                float centerY = v.getHeight() / 2.0f;
                renderer.queueTouchEvent(centerX, centerY);
                Log.i(TAG, "Using center point: (" + centerX + ", " + centerY + ")");
            }
            return true;   // 消耗事件
        });
    }

    /**
     * 初始化 AR 渲染线程（在 onSurfaceCreated 回调中触发）
     * 这样保证 GLSurfaceView 的 EGL context 已就绪
     */
    private void initAR() {
        // AR session 的创建放在 RendererCallback.onSurfaceCreated() 中
        // 这里只做 UI 状态初始化
        tvGuide.setText("正在初始化 AR 相机...");
        Log.i(TAG, "initAR: waiting for surface created");
    }

    // ==================== 权限 ====================

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
            // surface 可能还没创建，initAR 由 onSurfaceCreated 触发
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true;
                Toast.makeText(this, "相机权限已获取", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Camera permission granted");
                // 权限通过后，如果 surface 已创建则立即初始化 AR
                if (surfaceCreated && presenter != null) {
                    presenter.initAR();
                    presenter.onResumeSafely();
                }
            } else {
                Toast.makeText(this, "需要相机权限才能测量", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ==================== ARRenderer.RendererCallback ====================
    // 这些回调在 GL 渲染线程中执行，注意不要直接操作 UI

    @Override
    public void onSurfaceCreated() {
        Log.i(TAG, "onSurfaceCreated: GL surface ready");
        surfaceCreated = true;
        // AR session 初始化已在 ARRenderer.onSurfaceCreated() 中通过 initAROnGLThread() 完成
        // 这里只做状态标记
        if (permissionGranted) {
            Log.i(TAG, "onSurfaceCreated: AR will be initialized in GL thread");
        } else {
            Log.w(TAG, "onSurfaceCreated: camera permission not granted yet");
        }
    }

    @Override
    public void onFrameUpdated(int planeCount) {
        // 在 GL 线程，通过 runOnUiThread 更新 UI
        runOnUiThread(() -> {
            if (presenter == null) return;
            presenter.onPlaneDetected(planeCount);
        });
    }

    // ==================== MeasureContract.View ====================

    @Override
    public void showGuideMessage(String message) {
        runOnUiThread(() -> tvGuide.setText(message));
    }

    @Override
    public void showResult(String result) {
        runOnUiThread(() -> tvResult.setText(result));
    }

    @Override
    public void showMode(String mode) {
        runOnUiThread(() -> {
            tvMode.setText(mode);
            btnSwitchMode.setText(mode.equals("测高模式") ? "切换测距" : "切换测高");
        });
    }

    @Override
    public void showUnit(String unit) {
        runOnUiThread(() -> btnUnit.setText(unit));
    }

    @Override
    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void updateStartPoint(float x, float y) {
        runOnUiThread(() -> {
            tvPointStart.setVisibility(View.VISIBLE);
            tvPointStart.setX(x - 12);
            tvPointStart.setY(y - 12);
        });
    }

    @Override
    public void updateEndPoint(float x, float y) {
        runOnUiThread(() -> {
            tvPointEnd.setVisibility(View.VISIBLE);
            tvPointEnd.setX(x - 12);
            tvPointEnd.setY(y - 12);
        });
    }

    @Override
    public void hideStartPoint() {
        runOnUiThread(() -> tvPointStart.setVisibility(View.GONE));
    }

    @Override
    public void hideEndPoint() {
        runOnUiThread(() -> tvPointEnd.setVisibility(View.GONE));
    }

    @Override
    public void resetUI() {
        runOnUiThread(() -> {
            tvPointStart.setVisibility(View.GONE);
            tvPointEnd.setVisibility(View.GONE);
            tvResult.setText("0.00 m");
            tvGuide.setText("移动手机使十字架对准目标，点击屏幕标记起点");
            tvAdvice.setText("");
            tvAccuracy.setText("精度: 高");
            tvAccuracy.setTextColor(0xFF4CAF50);
        });
    }

    @Override
    public void showAccuracy(String accuracy) {
        runOnUiThread(() -> {
            tvAccuracy.setText(accuracy);
            if (accuracy.contains("高")) {
                tvAccuracy.setTextColor(0xFF4CAF50);
            } else if (accuracy.contains("中")) {
                tvAccuracy.setTextColor(0xFFFFC107);
            } else {
                tvAccuracy.setTextColor(0xFFF44336);
            }
        });
    }

    @Override
    public void showAdvice(String advice) {
        runOnUiThread(() -> tvAdvice.setText(advice));
    }

    // ==================== 生命周期 ====================

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        // 只有在 AR session 已初始化后才 resume
        if (presenter != null && permissionGranted) {
            presenter.onResumeSafely();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        if (presenter != null) {
            presenter.onPauseSafely();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.onDestroy();
            presenter.detachView();
        }
        Log.i(TAG, "onDestroy");
    }
}

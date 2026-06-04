package com.phonecl.armeasure.presenter;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.phonecl.armeasure.core.DistanceCalculator;
import com.phonecl.armeasure.data.ARDataManager;

import java.util.List;

/**
 * MeasurePresenter - 测量业务逻辑层（MVP-Presenter）
 *
 * 修复内容（2026-06-03）：
 * 1. onResume() 中增加 arDataManager 空判断
 * 2. handleTouch() 在 AR 未就绪时给出 Toast 提示
 * 3. markStart/EndPoint 中正确使用 performHitTest 返回值
 * 4. 修复 Log.i 参数类型错误（字符串拼接改为 String.format）
 */
public class MeasurePresenter implements MeasureContract.Presenter,
        ARDataManager.SessionCallback {

    private static final String TAG = "MeasurePresenter";

    private MeasureContract.View view;
    private ARDataManager arDataManager;
    private DistanceCalculator calculator;

    private boolean isMeasureHeight = false;
    private boolean hasStartPoint = false;
    private boolean hasEndPoint = false;

    private Anchor startAnchor;
    private Anchor endAnchor;
    private final float[] startPose = new float[3];
    private final float[] endPose = new float[3];

    public MeasurePresenter(android.content.Context context) {
        this.arDataManager = new ARDataManager(context, this);
        this.calculator = new DistanceCalculator();
    }

    // ==================== View 绑定 ====================

    @Override
    public void attachView(MeasureContract.View view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        this.view = null;
    }

    // ==================== AR 初始化 ====================

    @Override
    public void initAR() {
        if (view != null) {
            view.showGuideMessage("正在初始化 AR...");
        }
        arDataManager.initSession();
    }

    public void initAROnGLThread() {
        Log.i(TAG, "initAROnGLThread: called from GL rendering thread");
        if (view != null) {
            view.showGuideMessage("正在初始化 AR...");
        }
        arDataManager.initSession();
    }

    // ==================== 触摸取点 ====================

    @Override
    public void handleTouch(float x, float y) {
        Log.i(TAG, String.format("handleTouch: (%.1f, %.1f)", x, y));

        if (!arDataManager.isReady()) {
            if (view != null) {
                view.showToast("AR 未就绪，请稍候");
            }
            Log.w(TAG, "handleTouch: AR not ready");
            return;
        }

        if (!hasStartPoint) {
            markStartPoint(x, y);
        } else if (!hasEndPoint) {
            markEndPoint(x, y);
        } else {
            // 第三次点击：重置并重新开始
            resetMeasurement();
            markStartPoint(x, y);
        }
    }

    private void markStartPoint(float x, float y) {
        Log.i(TAG, String.format("markStartPoint at screen (%.1f, %.1f)", x, y));

        List<HitResult> hits = arDataManager.performHitTest(x, y);
        if (hits == null || hits.isEmpty()) {
            if (view != null) {
                view.showToast("未检测到平面，请移动手机对准平面");
            }
            Log.w(TAG, "markStartPoint: no hit result");
            return;
        }

        HitResult hit = hits.get(0);
        startAnchor = arDataManager.createAnchor(hit);
        if (startAnchor == null) {
            if (view != null) {
                view.showToast("无法创建起点锚点");
            }
            Log.e(TAG, "markStartPoint: createAnchor failed");
            return;
        }

        com.google.ar.core.Pose pose = startAnchor.getPose();
        startPose[0] = pose.tx();
        startPose[1] = pose.ty();
        startPose[2] = pose.tz();

        hasStartPoint = true;
        Log.i(TAG, String.format("Start point world: (%.3f, %.3f, %.3f)",
                startPose[0], startPose[1], startPose[2]));

        if (view != null) {
            view.updateStartPoint(x, y);
            view.showResult("起点已标记");
            view.showGuideMessage("移动手机使十字架对准终点，点击屏幕标记");
        }
    }

    private void markEndPoint(float x, float y) {
        Log.i(TAG, String.format("markEndPoint at screen (%.1f, %.1f)", x, y));

        List<HitResult> hits = arDataManager.performHitTest(x, y);
        if (hits == null || hits.isEmpty()) {
            if (view != null) {
                view.showToast("未检测到平面，请移动手机对准平面");
            }
            Log.w(TAG, "markEndPoint: no hit result");
            return;
        }

        HitResult hit = hits.get(0);
        endAnchor = arDataManager.createAnchor(hit);
        if (endAnchor == null) {
            if (view != null) {
                view.showToast("无法创建终点锚点");
            }
            Log.e(TAG, "markEndPoint: createAnchor failed");
            return;
        }

        com.google.ar.core.Pose pose = endAnchor.getPose();
        endPose[0] = pose.tx();
        endPose[1] = pose.ty();
        endPose[2] = pose.tz();

        hasEndPoint = true;
        Log.i(TAG, String.format("End point world: (%.3f, %.3f, %.3f)",
                endPose[0], endPose[1], endPose[2]));

        if (view != null) {
            view.updateEndPoint(x, y);
        }

        updateResult();
    }

    // ==================== 结果计算 ====================

    private void updateResult() {
        if (!hasStartPoint || !hasEndPoint || view == null) return;

        double distance;
        if (isMeasureHeight) {
            distance = calculator.calculateHeight(startPose, endPose);
        } else {
            distance = calculator.calculateDistance(startPose, endPose);
        }

        distance = calculator.convertToUnit(distance);
        String result = calculator.formatResult(distance, isMeasureHeight);
        view.showResult(result);

        String accuracy = calculator.getAccuracyLevel(distance);
        view.showAccuracy(accuracy);

        String advice = calculator.getMeasurementAdvice(distance);
        view.showAdvice(advice);

        Log.i(TAG, String.format("Result: %.2f %s", distance, calculator.getUnit()));
    }

    // ==================== 用户操作 ====================

    @Override
    public void switchMode() {
        isMeasureHeight = !isMeasureHeight;
        if (view != null) {
            view.showMode(isMeasureHeight ? "测高模式" : "测距模式");
        }
        resetMeasurement();
        Log.i(TAG, "Mode switched to: " + (isMeasureHeight ? "测高" : "测距"));
    }

    @Override
    public void resetMeasurement() {
        hasStartPoint = false;
        hasEndPoint = false;

        if (startAnchor != null) {
            arDataManager.detachAnchor(startAnchor);
        }
        if (endAnchor != null) {
            arDataManager.detachAnchor(endAnchor);
        }
        startAnchor = null;
        endAnchor = null;

        if (view != null) {
            view.hideStartPoint();
            view.hideEndPoint();
            view.showResult("0.00 m");
            view.showGuideMessage("移动手机使十字架对准目标，点击屏幕标记起点");
            view.showAccuracy("精度: 高");
            view.showAdvice("");
        }

        Log.i(TAG, "Measurement reset");
    }

    @Override
    public void toggleUnit() {
        calculator.setMetric(!calculator.isMetric());
        if (view != null) {
            view.showUnit(calculator.isMetric() ? "单位: 米" : "单位: 英尺");
        }
        updateResult();
        Log.i(TAG, "Unit toggled to: " + (calculator.isMetric() ? "米" : "英尺"));
    }

    @Override
    public void saveMeasurement() {
        if (hasStartPoint && hasEndPoint) {
            double distance;
            if (isMeasureHeight) {
                distance = calculator.calculateHeight(startPose, endPose);
            } else {
                distance = calculator.calculateDistance(startPose, endPose);
            }
            distance = calculator.convertToUnit(distance);
            String result = calculator.formatResult(distance, isMeasureHeight);
            if (view != null) {
                view.showToast("已保存: " + result);
            }
            Log.i(TAG, "Saved: " + result);
        } else {
            if (view != null) {
                view.showToast("请先完成测量");
            }
            Log.w(TAG, "saveMeasurement: measurement incomplete");
        }
    }

    // ==================== 生命周期（安全版）=====================

    /** 直接尝试 resume（resumeSession() 内部已处理 null 情况） */
    public void onResumeSafely() {
        if (arDataManager != null) {
            arDataManager.resumeSession();
            Log.i(TAG, "onResume: session resumed (or already resumed)");
        }
    }

    /** 只有 session 非空才 pause */
    public void onPauseSafely() {
        if (arDataManager != null) {
            arDataManager.pauseSession();
            Log.i(TAG, "onPause: session paused");
        }
    }

    @Override
    public void onResume() {
        onResumeSafely();
    }

    @Override
    public void onPause() {
        onPauseSafely();
    }

    @Override
    public void onDestroy() {
        resetMeasurement();
        if (arDataManager != null) {
            arDataManager.closeSession();
            Log.i(TAG, "onDestroy: session closed");
        }
    }

    // ==================== ARDataManager.SessionCallback ====================

    @Override
    public void onSessionReady() {
        Log.i(TAG, "onSessionReady - now resuming session");
        // initSession() 完成后，立即 resume，isReady 才会变成 true
        if (arDataManager != null) {
            arDataManager.resumeSession();
        }
        if (view != null) {
            view.showGuideMessage("AR 就绪，请点击屏幕标记起点");
        }
    }

    @Override
    public void onSessionError(String errorMessage) {
        Log.e(TAG, "onSessionError: " + errorMessage);
        if (view != null) {
            view.showGuideMessage("AR 初始化失败: " + errorMessage);
        }
    }

    @Override
    public void onPlaneDetected(int planeCount) {
        Log.d(TAG, "onPlaneDetected: " + planeCount);
        if (view != null) {
            String horizontalStatus = arDataManager.getHorizontalStatus();
            String guideMessage;
            if (!hasStartPoint) {
                guideMessage = "已检测到 " + planeCount + " 个平面，请点击起点\n" + horizontalStatus;
            } else if (!hasEndPoint) {
                guideMessage = "已检测到 " + planeCount + " 个平面，请点击终点\n" + horizontalStatus;
            } else {
                guideMessage = horizontalStatus;
            }
            view.showGuideMessage(guideMessage);
        }
    }

    // ==================== 供 Renderer 调用 ====================

    /** 检查 AR session 是否真正处于 resumed 状态 */
    public boolean isARSessionPaused() {
        return arDataManager == null || !arDataManager.isSessionResumed();
    }

    public boolean isARReady() {
        return arDataManager != null && arDataManager.isReady();
    }

    public void setCameraTextureName(int textureId) {
        if (arDataManager != null) {
            arDataManager.setCameraTextureName(textureId);
        }
    }

    public void setDisplayGeometry(int rotation, int width, int height) {
        if (arDataManager != null) {
            arDataManager.setDisplayGeometry(rotation, width, height);
        }
    }

    public Frame updateARFrame() {
        if (arDataManager == null) return null;
        return arDataManager.update();
    }

    public int getTrackedPlaneCount(Frame frame) {
        if (arDataManager == null) return 0;
        return arDataManager.getTrackedPlaneCount(frame);
    }
}

package com.phonecl.armeasure.data;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ARDataManager {

    private static final String TAG = "ARDataManager";
    
    private static final float DEFAULT_HORIZONTAL_TOLERANCE_DEGREES = 30.0f;

    private Session arSession;
    private final Context context;
    private boolean isReady = false;
    private int cameraTextureId = -1;

    private final List<Plane> trackedPlanes = new ArrayList<>();
    
    private float horizontalTolerance = DEFAULT_HORIZONTAL_TOLERANCE_DEGREES;

    public interface SessionCallback {
        void onSessionReady();
        void onSessionError(String errorMessage);
        void onPlaneDetected(int planeCount);
    }

    private SessionCallback callback;

    public ARDataManager(Context context, SessionCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public boolean initSession() {
        try {
            Log.i(TAG, "initSession: creating ARCore session...");
            
            arSession = new Session(context);
            Config config = new Config(arSession);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
            config.setFocusMode(Config.FocusMode.AUTO);
            arSession.configure(config);

            if (cameraTextureId >= 0) {
                arSession.setCameraTextureName(cameraTextureId);
                Log.i(TAG, "Camera texture bound during init: " + cameraTextureId);
            }

            isReady = true;
            Log.i(TAG, "ARCore session created & configured successfully");

            if (callback != null) {
                callback.onSessionReady();
            }
            return true;

        } catch (com.google.ar.core.exceptions.MissingGlContextException e) {
            Log.e(TAG, "initSession: Missing GL Context! Must be called from GL thread", e);
            isReady = false;
            if (callback != null) {
                callback.onSessionError("OpenGL上下文未就绪，请稍后重试");
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "initSession: Failed to create AR session", e);
            isReady = false;
            if (callback != null) {
                callback.onSessionError("AR初始化失败: " + e.getMessage());
            }
            return false;
        }
    }

    public void resumeSession() {
        if (arSession == null) {
            Log.w(TAG, "resumeSession: session is null");
            return;
        }
        try {
            arSession.resume();
            isReady = true;
            Log.i(TAG, "AR session resumed, isReady=" + isReady);
        } catch (Exception e) {
            Log.e(TAG, "Failed to resume AR session", e);
            isReady = false;
        }
    }

    public boolean isSessionResumed() {
        return isReady && arSession != null;
    }

    public void pauseSession() {
        if (arSession != null) {
            try {
                arSession.pause();
                Log.i(TAG, "AR session paused");
            } catch (Exception e) {
                Log.e(TAG, "Failed to pause AR session", e);
            }
        }
        isReady = false;
    }

    public void closeSession() {
        if (arSession != null) {
            try {
                arSession.close();
                Log.i(TAG, "AR session closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing AR session", e);
            }
            arSession = null;
        }
        isReady = false;
        trackedPlanes.clear();
    }

    public Frame update() {
        if (arSession == null) {
            Log.w(TAG, "update: session is null");
            return null;
        }
        try {
            Frame frame = arSession.update();
            updateTrackedPlanes(frame);
            return frame;
        } catch (Exception e) {
            Log.e(TAG, "update() failed", e);
            return null;
        }
    }

    private void updateTrackedPlanes(Frame frame) {
        if (frame == null) return;

        trackedPlanes.clear();
        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
        int trackingCount = 0;
        for (Plane plane : planes) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                trackedPlanes.add(plane);
                trackingCount++;
            }
        }

        if (callback != null && trackingCount > 0) {
            callback.onPlaneDetected(trackingCount);
        }
    }

    public List<HitResult> performHitTest(float x, float y) {
        Frame frame = update();
        if (frame == null) {
            Log.w(TAG, "performHitTest: frame is null");
            return null;
        }

        Log.d(TAG, "performHitTest: screen coordinates (" + x + ", " + y + ")");

        List<HitResult> hits = frame.hitTest(x, y);
        if (hits == null || hits.isEmpty()) {
            Log.d(TAG, "performHitTest: no hit at (" + x + ", " + y + ")");
            
            return tryExtendedHitTest(frame, x, y);
        }

        Log.d(TAG, "performHitTest: total hits = " + hits.size());

        List<HitResult> planeHits = new ArrayList<>();
        List<HitResult> limitedPlaneHits = new ArrayList<>();
        List<HitResult> featureHits = new ArrayList<>();
        
        for (HitResult hit : hits) {
            if (hit.getTrackable() instanceof Plane) {
                Plane plane = (Plane) hit.getTrackable();
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    planeHits.add(hit);
                } else if (plane.getTrackingState() == TrackingState.PAUSED) {
                    limitedPlaneHits.add(hit);
                }
            } else {
                featureHits.add(hit);
            }
        }

        if (!planeHits.isEmpty()) {
            Log.d(TAG, "performHitTest: found " + planeHits.size() + " TRACKING plane hits");
            return planeHits;
        }

        if (!limitedPlaneHits.isEmpty()) {
            Log.d(TAG, "performHitTest: using " + limitedPlaneHits.size() + " PAUSED plane hits");
            return limitedPlaneHits;
        }

        if (!featureHits.isEmpty()) {
            Log.d(TAG, "performHitTest: using " + featureHits.size() + " feature-point hits");
            return featureHits;
        }

        Log.d(TAG, "performHitTest: using " + hits.size() + " raw hits");
        return hits;
    }

    private List<HitResult> tryExtendedHitTest(Frame frame, float x, float y) {
        Log.d(TAG, "tryExtendedHitTest: attempting extended hit test");
        
        float[] offsets = {-20, -10, 0, 10, 20};
        
        for (float dx : offsets) {
            for (float dy : offsets) {
                List<HitResult> hits = frame.hitTest(x + dx, y + dy);
                if (hits != null && !hits.isEmpty()) {
                    Log.d(TAG, "tryExtendedHitTest: found hits at offset (" + dx + ", " + dy + ")");
                    return hits;
                }
            }
        }
        
        Log.d(TAG, "tryExtendedHitTest: no hits found");
        return null;
    }

    public Anchor createAnchor(HitResult hitResult) {
        try {
            Anchor anchor = hitResult.createAnchor();
            Log.i(TAG, "Anchor created");
            return anchor;
        } catch (Exception e) {
            Log.e(TAG, "createAnchor failed", e);
            return null;
        }
    }

    public void detachAnchor(Anchor anchor) {
        if (anchor != null) {
            try {
                anchor.detach();
            } catch (Exception e) {
                Log.e(TAG, "detachAnchor failed", e);
            }
        }
    }

    /**
     * 设置相机纹理名称
     * 注意：此方法可能在Session创建之前调用，需要缓存纹理ID
     */
    public void setCameraTextureName(int textureId) {
        this.cameraTextureId = textureId;
        
        if (arSession != null) {
            try {
                arSession.setCameraTextureName(textureId);
                Log.i(TAG, "Camera texture name set: " + textureId);
            } catch (Exception e) {
                Log.e(TAG, "setCameraTextureName failed", e);
            }
        } else {
            Log.w(TAG, "setCameraTextureName: session is not yet created, caching texture ID");
        }
    }

    public void setDisplayGeometry(int rotation, int width, int height) {
        if (arSession != null) {
            try {
                arSession.setDisplayGeometry(rotation, width, height);
            } catch (Exception e) {
                Log.e(TAG, "setDisplayGeometry failed", e);
            }
        }
    }

    public boolean isReady() {
        return isReady && arSession != null;
    }

    public Session getSession() {
        return arSession;
    }

    public int getTrackedPlaneCount(Frame frame) {
        if (frame == null) return 0;
        int count = 0;
        for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                count++;
            }
        }
        return count;
    }

    public List<Plane> getTrackedPlanes() {
        return new ArrayList<>(trackedPlanes);
    }

    public boolean hasTrackedPlanes() {
        return !trackedPlanes.isEmpty();
    }

    public void setHorizontalTolerance(float degrees) {
        this.horizontalTolerance = Math.max(0, Math.min(90, degrees));
        Log.i(TAG, "Horizontal tolerance set to: " + horizontalTolerance + " degrees");
    }

    public float getHorizontalTolerance() {
        return horizontalTolerance;
    }

    private float cachedTiltAngle = 90.0f;
    private long lastTiltUpdateTime = 0;
    private static final long TILT_UPDATE_INTERVAL_MS = 500;

    public float getCameraTiltAngle() {
        if (arSession == null) {
            return cachedTiltAngle;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastTiltUpdateTime < TILT_UPDATE_INTERVAL_MS) {
            return cachedTiltAngle;
        }
        
        Frame frame;
        try {
            frame = arSession.update();
        } catch (com.google.ar.core.exceptions.CameraNotAvailableException e) {
            Log.w(TAG, "Camera not available", e);
            return cachedTiltAngle;
        } catch (Exception e) {
            Log.w(TAG, "Error getting camera tilt angle - not in GL thread?", e);
            return cachedTiltAngle;
        }
        
        if (frame == null) {
            return cachedTiltAngle;
        }
        
        Camera camera = frame.getCamera();
        if (camera == null) {
            return cachedTiltAngle;
        }
        
        float[] viewMatrix = new float[16];
        camera.getViewMatrix(viewMatrix, 0);
        float pitch = (float) Math.toDegrees(Math.asin(-viewMatrix[6]));
        float roll = (float) Math.toDegrees(Math.atan2(viewMatrix[4], viewMatrix[0]));
        float tiltAngle = (float) Math.sqrt(pitch * pitch + roll * roll);
        
        cachedTiltAngle = tiltAngle;
        lastTiltUpdateTime = now;
        
        Log.d(TAG, "Camera tilt angle: " + tiltAngle + " degrees");
        return tiltAngle;
    }

    public boolean isHorizontalEnough() {
        float tiltAngle = getCameraTiltAngle();
        boolean isHorizontal = tiltAngle <= horizontalTolerance;
        Log.d(TAG, "isHorizontalEnough: tilt=" + tiltAngle + ", tolerance=" + horizontalTolerance + ", result=" + isHorizontal);
        return isHorizontal;
    }

    public String getHorizontalStatus() {
        float tiltAngle = getCameraTiltAngle();
        if (tiltAngle <= horizontalTolerance * 0.5f) {
            return "水平良好";
        } else if (tiltAngle <= horizontalTolerance) {
            return "基本水平";
        } else {
            return "请调整至水平位置 (倾斜: " + String.format("%.1f", tiltAngle) + "°)";
        }
    }
}
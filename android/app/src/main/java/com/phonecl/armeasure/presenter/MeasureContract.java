package com.phonecl.armeasure.presenter;

public interface MeasureContract {

    interface View {
        void showGuideMessage(String message);
        void showResult(String result);
        void showMode(String mode);
        void showUnit(String unit);
        void showToast(String message);
        void updateStartPoint(float x, float y);
        void updateEndPoint(float x, float y);
        void hideStartPoint();
        void hideEndPoint();
        void resetUI();
        void showAccuracy(String accuracy);
        void showAdvice(String advice);
    }

    interface Presenter {
        void attachView(View view);
        void detachView();
        void initAR();
        void handleTouch(float x, float y);
        void switchMode();
        void resetMeasurement();
        void toggleUnit();
        void saveMeasurement();
        void onResume();
        void onPause();
        void onDestroy();
        void onPlaneDetected(int planeCount);
    }
}
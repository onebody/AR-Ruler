package com.phonecl.armeasure.settings;

import com.phonecl.armeasure.settings.model.PrivacyInfo;
import com.phonecl.armeasure.settings.model.VersionInfo;

public interface SettingsContract {

    interface View {
        void showLoading();
        void hideLoading();
        void showVersionInfo(VersionInfo versionInfo);
        void showUpdateAvailable(VersionInfo versionInfo);
        void showNoUpdate();
        void showFeedbackSuccess();
        void showFeedbackFailed();
        void showPrivacyInfo(PrivacyInfo privacyInfo);
        void showPrivacyRevoked();
        void showError(String message);
    }

    interface Presenter {
        void attachView(View view);
        void detachView();
        void checkVersion();
        void submitFeedback(String type, String content, String contact);
        void getPrivacyInfo();
        void revokePrivacyAgreement();
    }
}
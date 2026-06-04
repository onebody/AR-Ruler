package com.phonecl.armeasure.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.phonecl.armeasure.data.SettingsRepository;
import com.phonecl.armeasure.settings.model.DeviceInfo;
import com.phonecl.armeasure.settings.model.FeedbackRequest;
import com.phonecl.armeasure.settings.model.PrivacyInfo;
import com.phonecl.armeasure.settings.model.VersionInfo;

public class SettingsPresenter implements SettingsContract.Presenter {
    private static final String TAG = "SettingsPresenter";

    private SettingsContract.View view;
    private SettingsRepository repository;
    private Context context;

    public SettingsPresenter(Context context) {
        this.context = context;
        this.repository = new SettingsRepository(context);
    }

    @Override
    public void attachView(SettingsContract.View view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        this.view = null;
    }

    @Override
    public void checkVersion() {
        if (view != null) {
            view.showLoading();
        }

        new AsyncTask<Void, Void, VersionInfo>() {
            @Override
            protected VersionInfo doInBackground(Void... voids) {
                String currentVersion = getCurrentVersion();
                return repository.fetchVersionInfo(currentVersion);
            }

            @Override
            protected void onPostExecute(VersionInfo versionInfo) {
                if (view != null) {
                    view.hideLoading();
                    if (versionInfo != null) {
                        view.showVersionInfo(versionInfo);
                        if (isUpdateAvailable(versionInfo)) {
                            view.showUpdateAvailable(versionInfo);
                        } else {
                            view.showNoUpdate();
                        }
                    } else {
                        view.showError("获取版本信息失败");
                    }
                }
            }
        }.execute();
    }

    @Override
    public void submitFeedback(String type, String content, String contact) {
        if (view != null) {
            view.showLoading();
        }

        FeedbackRequest request = new FeedbackRequest();
        request.setType(type);
        request.setContent(content);
        request.setContact(contact);
        request.setDeviceInfo(getDeviceInfo());

        new AsyncTask<FeedbackRequest, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(FeedbackRequest... requests) {
                return repository.sendFeedback(requests[0]);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (view != null) {
                    view.hideLoading();
                    if (success) {
                        view.showFeedbackSuccess();
                    } else {
                        view.showFeedbackFailed();
                    }
                }
            }
        }.execute(request);
    }

    @Override
    public void getPrivacyInfo() {
        if (view != null) {
            view.showLoading();
        }

        new AsyncTask<Void, Void, PrivacyInfo>() {
            @Override
            protected PrivacyInfo doInBackground(Void... voids) {
                return repository.fetchPrivacyInfo();
            }

            @Override
            protected void onPostExecute(PrivacyInfo privacyInfo) {
                if (view != null) {
                    view.hideLoading();
                    if (privacyInfo != null) {
                        view.showPrivacyInfo(privacyInfo);
                    } else {
                        view.showError("获取隐私信息失败");
                    }
                }
            }
        }.execute();
    }

    @Override
    public void revokePrivacyAgreement() {
        repository.savePrivacyAgreement(false);
        if (view != null) {
            view.showPrivacyRevoked();
        }
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            Log.e(TAG, "getCurrentVersion failed: " + e.getMessage());
            return "1.0.0";
        }
    }

    private boolean isUpdateAvailable(VersionInfo versionInfo) {
        try {
            String currentVersion = getCurrentVersion();
            String latestVersion = versionInfo.getVersionName();
            return compareVersions(latestVersion, currentVersion) > 0;
        } catch (Exception e) {
            Log.e(TAG, "isUpdateAvailable failed: " + e.getMessage());
            return false;
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return p1 - p2;
            }
        }
        return 0;
    }

    private DeviceInfo getDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setModel(android.os.Build.MODEL);
        deviceInfo.setOsVersion(android.os.Build.VERSION.RELEASE);
        deviceInfo.setAppVersion(getCurrentVersion());
        return deviceInfo;
    }
}
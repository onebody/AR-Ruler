package com.phonecl.armeasure.settings.model;

public class VersionInfo {
    private int versionCode;
    private String versionName;
    private String updateContent;
    private String downloadUrl;
    private boolean isForceUpdate;

    public VersionInfo() {}

    public VersionInfo(int versionCode, String versionName, String updateContent, 
                       String downloadUrl, boolean isForceUpdate) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.updateContent = updateContent;
        this.downloadUrl = downloadUrl;
        this.isForceUpdate = isForceUpdate;
    }

    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getUpdateContent() { return updateContent; }
    public void setUpdateContent(String updateContent) { this.updateContent = updateContent; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public boolean isForceUpdate() { return isForceUpdate; }
    public void setForceUpdate(boolean forceUpdate) { isForceUpdate = forceUpdate; }
}
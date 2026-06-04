package com.phonecl.armeasure.settings.model;

public class DeviceInfo {
    private String model;
    private String osVersion;
    private String appVersion;

    public DeviceInfo() {}

    public DeviceInfo(String model, String osVersion, String appVersion) {
        this.model = model;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}
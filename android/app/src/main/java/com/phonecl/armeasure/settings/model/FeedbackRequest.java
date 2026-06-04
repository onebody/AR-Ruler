package com.phonecl.armeasure.settings.model;

public class FeedbackRequest {
    private String type;
    private String content;
    private String contact;
    private DeviceInfo deviceInfo;

    public FeedbackRequest() {}

    public FeedbackRequest(String type, String content, String contact, DeviceInfo deviceInfo) {
        this.type = type;
        this.content = content;
        this.contact = contact;
        this.deviceInfo = deviceInfo;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }
}
package com.phonecl.armeasure.settings.model;

import java.util.List;

public class ThirdPartyInfo {
    private String name;
    private List<String> sharedInfo;
    private String purpose;

    public ThirdPartyInfo() {}

    public ThirdPartyInfo(String name, List<String> sharedInfo, String purpose) {
        this.name = name;
        this.sharedInfo = sharedInfo;
        this.purpose = purpose;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getSharedInfo() { return sharedInfo; }
    public void setSharedInfo(List<String> sharedInfo) { this.sharedInfo = sharedInfo; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
package com.phonecl.armeasure.settings.model;

import java.util.List;

public class PrivacyInfo {
    private List<InfoItem> personalInfoList;
    private List<ThirdPartyInfo> thirdPartyList;

    public PrivacyInfo() {}

    public PrivacyInfo(List<InfoItem> personalInfoList, List<ThirdPartyInfo> thirdPartyList) {
        this.personalInfoList = personalInfoList;
        this.thirdPartyList = thirdPartyList;
    }

    public List<InfoItem> getPersonalInfoList() { return personalInfoList; }
    public void setPersonalInfoList(List<InfoItem> personalInfoList) { this.personalInfoList = personalInfoList; }

    public List<ThirdPartyInfo> getThirdPartyList() { return thirdPartyList; }
    public void setThirdPartyList(List<ThirdPartyInfo> thirdPartyList) { this.thirdPartyList = thirdPartyList; }
}
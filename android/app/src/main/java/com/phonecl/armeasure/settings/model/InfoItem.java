package com.phonecl.armeasure.settings.model;

public class InfoItem {
    private String name;
    private String purpose;
    private String storagePeriod;

    public InfoItem() {}

    public InfoItem(String name, String purpose, String storagePeriod) {
        this.name = name;
        this.purpose = purpose;
        this.storagePeriod = storagePeriod;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStoragePeriod() { return storagePeriod; }
    public void setStoragePeriod(String storagePeriod) { this.storagePeriod = storagePeriod; }
}
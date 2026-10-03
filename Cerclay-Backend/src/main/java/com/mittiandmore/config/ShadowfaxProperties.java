package com.mittiandmore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shadowfax")
public class ShadowfaxProperties {

    private String baseUrl =
            "https://dale.staging.shadowfax.in/api";

    private String apiToken;

    // Pickup details
    private String pickupName;
    private String pickupContact;
    private String pickupAddressLine1;
    private String pickupAddressLine2;
    private String pickupCity;
    private String pickupState;
    private String pickupPincode;
    private String pickupUniqueCode;

    // RTS details
    private String rtsName;
    private String rtsContact;
    private String rtsAddressLine1;
    private String rtsAddressLine2;
    private String rtsCity;
    private String rtsState;
    private String rtsPincode;
    private String rtsEmail;
    private String rtsUniqueCode;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getPickupName() {
        return pickupName;
    }

    public void setPickupName(String pickupName) {
        this.pickupName = pickupName;
    }

    public String getPickupContact() {
        return pickupContact;
    }

    public void setPickupContact(String pickupContact) {
        this.pickupContact = pickupContact;
    }

    public String getPickupAddressLine1() {
        return pickupAddressLine1;
    }

    public void setPickupAddressLine1(String pickupAddressLine1) {
        this.pickupAddressLine1 = pickupAddressLine1;
    }

    public String getPickupAddressLine2() {
        return pickupAddressLine2;
    }

    public void setPickupAddressLine2(String pickupAddressLine2) {
        this.pickupAddressLine2 = pickupAddressLine2;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getPickupState() {
        return pickupState;
    }

    public void setPickupState(String pickupState) {
        this.pickupState = pickupState;
    }

    public String getPickupPincode() {
        return pickupPincode;
    }

    public void setPickupPincode(String pickupPincode) {
        this.pickupPincode = pickupPincode;
    }

    public String getPickupUniqueCode() {
        return pickupUniqueCode;
    }

    public void setPickupUniqueCode(String pickupUniqueCode) {
        this.pickupUniqueCode = pickupUniqueCode;
    }

    public String getRtsName() {
        return rtsName;
    }

    public void setRtsName(String rtsName) {
        this.rtsName = rtsName;
    }

    public String getRtsContact() {
        return rtsContact;
    }

    public void setRtsContact(String rtsContact) {
        this.rtsContact = rtsContact;
    }

    public String getRtsAddressLine1() {
        return rtsAddressLine1;
    }

    public void setRtsAddressLine1(String rtsAddressLine1) {
        this.rtsAddressLine1 = rtsAddressLine1;
    }

    public String getRtsAddressLine2() {
        return rtsAddressLine2;
    }

    public void setRtsAddressLine2(String rtsAddressLine2) {
        this.rtsAddressLine2 = rtsAddressLine2;
    }

    public String getRtsCity() {
        return rtsCity;
    }

    public void setRtsCity(String rtsCity) {
        this.rtsCity = rtsCity;
    }

    public String getRtsState() {
        return rtsState;
    }

    public void setRtsState(String rtsState) {
        this.rtsState = rtsState;
    }

    public String getRtsPincode() {
        return rtsPincode;
    }

    public void setRtsPincode(String rtsPincode) {
        this.rtsPincode = rtsPincode;
    }

    public String getRtsEmail() {
        return rtsEmail;
    }

    public void setRtsEmail(String rtsEmail) {
        this.rtsEmail = rtsEmail;
    }

    public String getRtsUniqueCode() {
        return rtsUniqueCode;
    }

    public void setRtsUniqueCode(String rtsUniqueCode) {
        this.rtsUniqueCode = rtsUniqueCode;
    }
}
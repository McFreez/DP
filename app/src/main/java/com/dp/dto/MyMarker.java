package com.dp.dto;

public class MyMarker {

    private String id;
    private String name;
    private String address;
    private String groupID;
    private double latitude;
    private double longitude;

    public MyMarker(String id, String name, String address, String groupID, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.groupID = groupID;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public MyMarker(String address, String groupID, double latitude, double longitude) {
        this.address = address;
        this.groupID = groupID;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

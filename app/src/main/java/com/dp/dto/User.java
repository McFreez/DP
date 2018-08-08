package com.dp.dto;

public class User {

    private String id;
    private String name;
    private String email;
    private double latitude;
    private double longitude;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        latitude = 0;
        longitude = 0;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

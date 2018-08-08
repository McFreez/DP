package com.dp.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group {

    private String id;
    private String name;
    private String adminGoogleID;
    private String adminEmail;
    private Map<String, User> users = new HashMap<>();
    private Map<String, MyMarker> markers = new HashMap<>();

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Group(String id, String name, String adminGoogleID, String adminEmail) {
        this.id = id;
        this.name = name;
        this.adminGoogleID = adminGoogleID;
        this.adminEmail = adminEmail;
    }

    public Group(String id, String name, String adminGoogleID, String adminEmail, Map<String, User> users) {
        this.id = id;
        this.name = name;
        this.adminGoogleID = adminGoogleID;
        this.adminEmail = adminEmail;
        this.users = users;
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

    public String getAdminGoogleID() {
        return adminGoogleID;
    }

    public void setGoogleID(String admin) {
        this.adminGoogleID = admin;
    }

    public void setAdminGoogleID(String adminGoogleID) {
        this.adminGoogleID = adminGoogleID;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public Map<String, User> getUsers(){
        return users;
    }

    public void addOrUpdateUser(User newUser){
        users.put(newUser.getId(), newUser);
    }

    public void removeUser(String userGoogleID){
        users.remove(userGoogleID);
    }

    public User getUser(String userGoogleID){
        if(users.containsKey(userGoogleID))
            return users.get(userGoogleID);
        else
            return null;
    }

    public Map<String, MyMarker> getMarkers() {
        return markers;
    }

    public void addOrUpdateMarker(MyMarker marker){
        markers.put(marker.getId(), marker);
    }

    public void removeMarker(String markerID){
        markers.remove(markerID);
    }

    public MyMarker getMarker(String markerID){
        if(markers.containsKey(markerID))
            return markers.get(markerID);
        else
            return null;
    }
}

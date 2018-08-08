package com.dp.db;

import java.util.HashMap;
import java.util.Map;

public final class DbContract {

    public static final String ROOT = "dp/project";

    public static final class UserObject{

        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";

        public static Map<String, Object> updateUserData(String name, String email){
            Map<String, Object> user = new HashMap<>();
            user.put(NAME, name);
            user.put(EMAIL, email);

            return user;
        }

        public static Map<String, Object> updateUserData(String name, String email, double latitude, double longitude){
            Map<String, Object> user = new HashMap<>();
            user.put(NAME, name);
            user.put(EMAIL, email);
            user.put(LATITUDE, latitude);
            user.put(LONGITUDE, longitude);

            return user;
        }

        public static Map<String, Object> newUserGroup(String groupName, String adminGoogleID, String adminEmail){
            Map<String, Object> newGroup = new HashMap<>();
            newGroup.put(GroupObject.NAME, groupName);
            newGroup.put(GroupObject.ADMIN_ID, adminGoogleID);
            newGroup.put(GroupObject.ADMIN_EMAIL, adminEmail);

            return newGroup;
        }

        public static Map<String, Object> addInvitation(String groupName, String adminGoogleID, String adminEmail){
            Map<String, Object> newInvitation = new HashMap<>();
            newInvitation.put(GroupObject.NAME, groupName);
            newInvitation.put(GroupObject.ADMIN_ID, adminGoogleID);
            newInvitation.put(GroupObject.ADMIN_EMAIL, adminEmail);

            return newInvitation;
        }
    }

    public static final String USER_GROUPS = "user_groups";
    public static final String USER_INVITATIONS = "invitations";

    public static String usersRoot(){
        return ROOT + "/users";
    }

    public static String addOrUpdateUser(String userGoogleID){
        return usersRoot() + "/" + userGoogleID;
    }

    public static String getUser(String userGoogleID){
        return usersRoot() + "/" + userGoogleID;
    }

    public static String getUserGroups(String userGoogleID){
        return usersRoot() + "/" + userGoogleID + "/" + USER_GROUPS;
    }

    public static String getExactUserGroup(String userGoogleID, String groupID){
        return getUserGroups(userGoogleID) + "/" + groupID;
    }

    public static String getUserInvitations(String userGoogleID){
        return usersRoot() + "/" + userGoogleID + "/" + USER_INVITATIONS;
    }

    public static String getInvitationToGroup(String userGoogleID, String groupID){
        return getUserInvitations(userGoogleID) + "/" + groupID;
    }

    public static String addUserGroup(String userGoogleID, String groupID){
        return getUserGroups(userGoogleID) + "/" + groupID;
    }

    public static final class GroupObject{

        public static final String NAME = "name";
        public static final String ADMIN_ID = "adminID";
        public static final String ADMIN_EMAIL = "adminEmail";

        public static Map<String, Object> createGroup(String name, String adminGoogleID, String adminEmail){
            Map<String, Object> group = new HashMap<>();
            group.put(NAME, name);
            group.put(ADMIN_ID, adminGoogleID);
            group.put(ADMIN_EMAIL, adminEmail);

            return group;
        }

        public static Map<String, Object> addGroupUser(String userName, String userEmail){
            Map<String, Object> newGroupUser = new HashMap<>();
            newGroupUser.put(UserObject.NAME, userName);
            newGroupUser.put(UserObject.EMAIL, userEmail);

            return newGroupUser;
        }

        public static Map<String, Object> addInviteRequest(String userName, String userEmail){
            Map<String, Object> newInviteRequest = new HashMap<>();
            newInviteRequest.put(UserObject.NAME, userName);
            newInviteRequest.put(UserObject.EMAIL, userEmail);

            return newInviteRequest;
        }
    }

    public static final String GROUP_USERS = "group_users";
    public static final String GROUP_INVITE_REQUESTS = "invite_requests";

    public static String groupsRoot(){
        return ROOT + "/groups";
    }

    public static String addGroup(){
        return groupsRoot();
    }

    public static String getGroup(String groupID){
        return groupsRoot() + "/" + groupID;
    }

    public static String getAllGroupUsers(String groupID){
        return getGroup(groupID) + "/" + GROUP_USERS;
    }

    public static String getExactGroupUser(String groupID, String userGoogleID){
        return getAllGroupUsers(groupID) + "/" + userGoogleID;
    }

    public static String getGroupInviteRequests(String groupID){
        return groupsRoot() + "/" + groupID + "/" + GROUP_INVITE_REQUESTS;
    }

    public static String getInviteRequestByUser(String groupID, String userGoogleID){
        return getGroupInviteRequests(groupID) + "/" + userGoogleID;
    }

    public static String addOrUpdateGroupUser(String groupID, String userGoogleID){
        return getAllGroupUsers(groupID) + "/" + userGoogleID;
    }

    public static final class MarkerObject{

        public static final String NAME = "name";
        public static final String ADDRESS = "address";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";

        public static Map<String, Object> updateMarker(String name, String address, double latitude, double longitude){
            Map<String, Object> marker = new HashMap<>();
            marker.put(NAME, name);
            marker.put(ADDRESS, address);
            marker.put(LATITUDE, latitude);
            marker.put(LONGITUDE, longitude);

            return marker;
        }
    }

    public static final String GROUP_MARKERS = "markers";

    public static String getGroupMarkers(String groupID){
        return getGroup(groupID) + "/" + GROUP_MARKERS;
    }

    public static String addMarker(String groupID){
        return getGroupMarkers(groupID);
    }

    public static String getMarker(String groupID, String markerID){
        return getGroupMarkers(groupID) + "/" + markerID;
    }
}

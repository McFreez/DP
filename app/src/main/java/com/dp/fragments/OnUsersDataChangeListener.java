package com.dp.fragments;

import com.dp.dto.Group;
import com.dp.dto.User;
import com.google.firebase.firestore.FirebaseFirestore;

public interface OnUsersDataChangeListener {

    void onUserAdded(User user);
    void onUserRemoved(String userGoogleID);
    void onUserModified(User user);
    void onClear();
}

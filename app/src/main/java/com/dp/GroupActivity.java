package com.dp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.MyMarker;
import com.dp.dto.User;
import com.dp.fragments.GroupFragment;
import com.dp.fragments.OnUsersDataChangeListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;

public class GroupActivity extends BaseActivity {

    private static final String LOG_TAG = "GroupActivity";
    public static final String GROUP_ID_KEY = "group_id";

    private ListenerRegistration mUsersUpdatesRegistration;
    private ListenerRegistration mMarkersUpdatesRegistration;

    private OnUsersDataChangeListener mOnUserDataChangeListener;
    private GroupFragment.OnRouteListener mOnRouteListener;
    private GroupFragment.OnMarkersChangeListener mOnMarkersChangeListener;

    private Group mGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getExtras() == null){
            Log.w(LOG_TAG, "No data passed to activity");
            onBackPressed();
            return;
        }

        if(!getIntent().getExtras().containsKey(GROUP_ID_KEY)){
            Toast.makeText(this, "No group to open", Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Group ID not found");
            onBackPressed();
            return;
        }

        String groupID = getIntent().getExtras().getString(GROUP_ID_KEY);

        mDb.document(DbContract.getGroup(groupID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot doc = task.getResult();
                            if (doc != null && doc.exists()) {
                                String id = doc.getId();
                                String name = doc.getString(DbContract.GroupObject.NAME);
                                String adminGoogleID = doc.getString(DbContract.GroupObject.ADMIN_ID);
                                String adminEmail = doc.getString(DbContract.GroupObject.ADMIN_EMAIL);
                                mGroup = new Group(id, name, adminGoogleID, adminEmail);

                                updateMenu();
                                runUsersRealtimeUpdates();
                                runMarkersRealtimeUpdates();
                                loadMapFragment();
                            } else {
                                Log.d(LOG_TAG, "No such document");
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Group not found", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG, "Group not found");
                            onBackPressed();
                        }
                    }
                });
    }

    public void loadMapFragment(){
        Fragment fragment = new GroupFragment();
        ((GroupFragment)fragment).setDb(mDb);
        ((GroupFragment)fragment).setGroup(mGroup);

        mOnUserDataChangeListener = (OnUsersDataChangeListener) fragment;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.content_main, fragment);
        ft.commit();
    }

    public void loadMembersActivity(){
        Intent intent = new Intent(this, ManageMembersActivity.class);
        intent.putExtra(ManageMembersActivity.GROUP_ID_KEY, mGroup.getId());
        startActivity(intent);
    }

    public void setOnRouteListener(GroupFragment.OnRouteListener listener){
        mOnRouteListener = listener;
    }

    public void setOnMarkersChangeListener(GroupFragment.OnMarkersChangeListener listener){
        mOnMarkersChangeListener = listener;
    }

    @Override
    public void onBackPressed() {
        if(!closeDrawerIfOpened()) {
            if(mOnRouteListener != null && mOnRouteListener.onClearRoute())
                return;

            super.onBackPressed();
        }
    }

    private void updateMenu(){
        if(mGroup.getAdminGoogleID().equals(mUser.getUid()))
            if(mToolbar.getMenu() != null) {
                MenuItem item = mToolbar.getMenu().findItem(R.id.action_group_members);
                if(item != null)
                    mToolbar.getMenu().findItem(R.id.action_group_members).setVisible(true);
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.group_menu, menu);

        if(mGroup != null)
            updateMenu();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_group_members) {
            loadMembersActivity();
            return true;
        } else
            if(id == R.id.action_exit_group){
                if(mGroup.getAdminGoogleID().equals(mUser.getUid()))
                    adminExitGroup();
                else
                    commonExitGroup();
            }

        return super.onOptionsItemSelected(item);
    }

    private void commonExitGroup(){
        WriteBatch batch = mDb.batch();

        DocumentReference userInGroup = mDb.document(DbContract.getExactGroupUser(mGroup.getId(), mUser.getUid()));
        batch.delete(userInGroup);

        DocumentReference userGroup = mDb.document(DbContract.getExactUserGroup(mUser.getUid(), mGroup.getId()));
        batch.delete(userGroup);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    Log.d(LOG_TAG, "Leave group error.");
                    return;
                }

                onBackPressed();
            }
        });
    }

    private void adminExitGroup(){
        mDb.collection(DbContract.getGroupInviteRequests(mGroup.getId()))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            Log.d(LOG_TAG, "Invite requests check failed");
                            return;
                        }

                        QuerySnapshot query = task.getResult();

                        WriteBatch batch = mDb.batch();

                        if(!query.isEmpty()){
                            for(DocumentSnapshot doc : query.getDocuments()){
                                DocumentReference inviteRequest = mDb.document(DbContract.getInviteRequestByUser(mGroup.getId(), doc.getId()));
                                batch.delete(inviteRequest);
                            }
                        }

                        for(Map.Entry<String, User> entry : mGroup.getUsers().entrySet()){
                            if(entry.getKey().equals(mUser.getUid()))
                                continue;

                            User user = entry.getValue();

                            DocumentReference userInGroup = mDb.document(DbContract.getExactGroupUser(mGroup.getId(), user.getId()));
                            batch.delete(userInGroup);

                            DocumentReference userGroup = mDb.document(DbContract.getExactUserGroup(user.getId(), mGroup.getId()));
                            batch.delete(userGroup);
                        }

                        DocumentReference userInGroup = mDb.document(DbContract.getExactGroupUser(mGroup.getId(), mUser.getUid()));
                        batch.delete(userInGroup);

                        DocumentReference userGroup = mDb.document(DbContract.getExactUserGroup(mUser.getUid(), mGroup.getId()));
                        batch.delete(userGroup);

                        DocumentReference group = mDb.document(DbContract.getGroup(mGroup.getId()));
                        batch.delete(group);

                        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()){
                                    Log.d(LOG_TAG, "Destroy group and leave failed.");
                                    return;
                                }

                                onBackPressed();
                            }
                        });
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        displaySelectedScreen(id);

        return super.onNavigationItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        runUsersRealtimeUpdates();
        runMarkersRealtimeUpdates();
    }

    private void runUsersRealtimeUpdates(){
        if(mGroup == null)
            return;

        if(mGroup.getId() == null)
            return;

        if(mOnUserDataChangeListener != null)
            mOnUserDataChangeListener.onClear();

        Query query = mDb.collection(DbContract.getAllGroupUsers(mGroup.getId()));
        mUsersUpdatesRegistration = query.addSnapshotListener(
                new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        if(e != null){
                            Log.w(LOG_TAG, "listen:error", e);
                            return;
                        }

                        for(DocumentChange dc : documentSnapshots.getDocumentChanges()){
                            DocumentSnapshot doc = dc.getDocument();
                            String id = doc.getId();
                            switch (dc.getType()){
                                case ADDED:
                                    if(id.equals(mUser.getUid()))
                                        break;
                                    User newUser = new User(id, doc.getString(DbContract.UserObject.NAME), doc.getString(DbContract.UserObject.EMAIL));
                                    if(doc.contains(DbContract.UserObject.LATITUDE) && doc.contains(DbContract.UserObject.LONGITUDE)){
                                        newUser.setLatitude(doc.getDouble(DbContract.UserObject.LATITUDE));
                                        newUser.setLongitude(doc.getDouble(DbContract.UserObject.LONGITUDE));

                                        if(mOnUserDataChangeListener != null){
                                            mOnUserDataChangeListener.onUserAdded(newUser);
                                        }
                                    }
                                    mGroup.addOrUpdateUser(newUser);
                                    break;
                                case REMOVED:
                                    if(id.equals(mUser.getUid())) {
                                        onBackPressed();
                                    }
                                    if(mOnUserDataChangeListener != null){
                                        mOnUserDataChangeListener.onUserRemoved(id);
                                    }
                                    mGroup.removeUser(id);
                                    break;
                                case MODIFIED:
                                    if(id.equals(mUser.getUid()))
                                        break;
                                    User modifiedUser = mGroup.getUser(id);
                                    if(doc.contains(DbContract.UserObject.LATITUDE) && doc.contains(DbContract.UserObject.LONGITUDE)){
                                        modifiedUser.setLatitude(doc.getDouble(DbContract.UserObject.LATITUDE));
                                        modifiedUser.setLongitude(doc.getDouble(DbContract.UserObject.LONGITUDE));

                                        if(mOnUserDataChangeListener != null){
                                            mOnUserDataChangeListener.onUserModified(modifiedUser);
                                        }
                                    }
                                    mGroup.addOrUpdateUser(modifiedUser);
                                    break;
                            }
                        }
                    }
                }
        );
    }

    private void runMarkersRealtimeUpdates(){
        if(mGroup == null)
            return;

        if(mGroup.getId() == null)
            return;

        if(mOnMarkersChangeListener != null)
            mOnMarkersChangeListener.onClear();

        Query query = mDb.collection(DbContract.getGroupMarkers(mGroup.getId()));
        mMarkersUpdatesRegistration = query.addSnapshotListener(
                new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        if(e != null){
                            Log.w(LOG_TAG, "listen:error", e);
                            return;
                        }

                        for(DocumentChange dc : documentSnapshots.getDocumentChanges()){
                            DocumentSnapshot doc = dc.getDocument();
                            String id = doc.getId();
                            switch (dc.getType()){
                                case ADDED:
                                    String newMarkerName = null;
                                    if(doc.contains(DbContract.MarkerObject.NAME))
                                        newMarkerName = doc.getString(DbContract.MarkerObject.NAME);

                                    String newMarkerAddress = null;
                                    if(doc.contains(DbContract.MarkerObject.ADDRESS))
                                        newMarkerAddress = doc.getString(DbContract.MarkerObject.ADDRESS);

                                    MyMarker newMarker = new MyMarker(id,
                                            newMarkerName,
                                            newMarkerAddress,
                                            mGroup.getId(),
                                            doc.getDouble(DbContract.MarkerObject.LATITUDE),
                                            doc.getDouble(DbContract.MarkerObject.LONGITUDE));

                                    if(mOnMarkersChangeListener != null)
                                        mOnMarkersChangeListener.onAdded(newMarker);

                                    mGroup.addOrUpdateMarker(newMarker);
                                    break;
                                case REMOVED:
                                    if(mOnMarkersChangeListener != null)
                                        mOnMarkersChangeListener.onRemoved(id);
                                    mGroup.removeMarker(id);
                                    break;
                                case MODIFIED:
                                    String modifiedMarkerName = null;
                                    if(doc.contains(DbContract.MarkerObject.NAME))
                                        modifiedMarkerName = doc.getString(DbContract.MarkerObject.NAME);

                                    String modifiedMarkerAddress = null;
                                    if(doc.contains(DbContract.MarkerObject.ADDRESS))
                                        modifiedMarkerAddress = doc.getString(DbContract.MarkerObject.ADDRESS);

                                    MyMarker modifiedMarker = new MyMarker(id,
                                            modifiedMarkerName,
                                            modifiedMarkerAddress,
                                            mGroup.getId(),
                                            doc.getDouble(DbContract.MarkerObject.LATITUDE),
                                            doc.getDouble(DbContract.MarkerObject.LONGITUDE));

                                    if(mOnMarkersChangeListener != null)
                                        mOnMarkersChangeListener.onAdded(modifiedMarker);

                                    mGroup.addOrUpdateMarker(modifiedMarker);
                                    break;
                            }
                        }
                    }
                }
        );
    }


    @Override
    protected void onStop() {
        super.onStop();

        if(mUsersUpdatesRegistration != null)
            mUsersUpdatesRegistration.remove();

        if(mMarkersUpdatesRegistration != null)
            mMarkersUpdatesRegistration.remove();
    }
}

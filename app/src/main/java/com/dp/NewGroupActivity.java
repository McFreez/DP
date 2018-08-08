package com.dp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.fragments.NewGroupFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewGroupActivity extends BaseActivity {

    private static final String LOG_TAG = "NewGroupActivity";

    private Map<String, Group> mSearchGroupResults = new HashMap<>();
    private Map<String, Group> myGroups = new HashMap<>();

    private OnCompleteListener<QuerySnapshot> mOnCompleteListener = new OnCompleteListener<QuerySnapshot>() {
        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            if(!task.isSuccessful()) {
                Log.d(LOG_TAG, "Task result is failed");
                return;
            }

            QuerySnapshot result = task.getResult();

            if(result.isEmpty()){
                Log.d(LOG_TAG, "Task result is empty");
                return;
            }

            Log.d(LOG_TAG, "Found " + result.getDocuments().size() + " results");

            mSearchGroupResults.clear();
            List<String> foundGroups = new ArrayList<>();

            for(DocumentSnapshot doc : result){
                String id = doc.getId();
                if(!myGroups.containsKey(id)) {
                    String name = doc.getString(DbContract.GroupObject.NAME);
                    String adminGoogleID = doc.getString(DbContract.GroupObject.ADMIN_ID);
                    String adminEmail = doc.getString(DbContract.GroupObject.ADMIN_EMAIL);

                    String groupKey = name + " (" + adminEmail + ")";

                    mSearchGroupResults.put(groupKey, new Group(id, name, adminGoogleID, adminEmail));
                    foundGroups.add(groupKey);
                }
            }

            if(mSearchView != null && foundGroups.size() > 0)
                mSearchView.setSuggestions(foundGroups.toArray(new String[0]));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        Fragment fragment = new NewGroupFragment();
        ((NewGroupFragment)fragment).setDb(mDb);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content_main, fragment);
        ft.commit();

        mNavigationView.setCheckedItem(R.id.nmenu_new_group);

        mDb.collection(DbContract.getUserGroups(mUser.getUid()))
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot query) {
                        if(query.isEmpty()){
                            Log.d(LOG_TAG, "No user groups found.");
                            return;
                        }

                        for (DocumentSnapshot doc : query.getDocuments()){
                            String id = doc.getId();
                            String name = doc.getString(DbContract.GroupObject.NAME);
                            String adminGoogleID = doc.getString(DbContract.GroupObject.ADMIN_ID);
                            String adminEmail = doc.getString(DbContract.GroupObject.ADMIN_EMAIL);

                            myGroups.put(id, new Group(id, name, adminGoogleID, adminEmail));
                        }
                    }
                });

        if(mSearchView == null)
            return;

        String[] list = new String[]{"Clipcodes", "Android Tutorials", "Youtube Clipcodes Tutorials", "SearchView Clicodes", "Android Clipcodes", "Tutorials Clipcodes"};

        mSearchView.clearFocus();
        mSearchView.setSuggestions(list);
        mSearchView.setSubmitOnClick(true);
        mSearchView.setHintTextColor(getResources().getColor(R.color.textHintColor));
        mSearchView.setHint("Group name");
        mSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String groupKey) {
                if(mSearchGroupResults.containsKey(groupKey)){
                    final Group selectedGroup = mSearchGroupResults.get(groupKey);

                    AlertDialog.Builder builder = new AlertDialog.Builder(NewGroupActivity.this);
                    builder.setMessage("Join group '"
                            + selectedGroup.getName() + "'"
                            + " (admin: " + selectedGroup.getAdminEmail() + ")?")
                            .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    checkGroupBeforeSendingRequest(selectedGroup);
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).create();

                    builder.show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText == null)
                    return false;

                if(newText.equals(""))
                    return false;

                mDb.collection(DbContract.groupsRoot())
                        .orderBy(DbContract.GroupObject.NAME)
                        .limit(5)
                        .startAt(newText)
                        .get()
                        .addOnCompleteListener(mOnCompleteListener);

                return false;
            }
        });
    }

    private void checkGroupBeforeSendingRequest(final Group newGroup){
        mDb.document(DbContract.getExactGroupUser(newGroup.getId(), mUser.getUid()))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(!task.isSuccessful()){
                            Log.d(LOG_TAG, "Is user in group check failed. ");
                            return;
                        }

                        DocumentSnapshot doc = task.getResult();

                        if(doc.exists()){
                            Log.d(LOG_TAG, "User is already in group");
                            Toast.makeText(getApplicationContext(), "You are already in group", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDb.document(DbContract.getInvitationToGroup(mUser.getUid(), newGroup.getId()))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(!task.isSuccessful()){
                                            Log.d(LOG_TAG, "Invitation existence check failed. ");
                                            return;
                                        }

                                        DocumentSnapshot doc = task.getResult();

                                        if(doc.exists()){
                                            Log.d(LOG_TAG, "User is already invited to group.");
                                            acceptInvitation(newGroup);
                                            return;
                                        }

                                        sendInviteRequest(newGroup.getId());
                                    }
                                });
                    }
                });
    }

    private void sendInviteRequest(String groupID){
        mDb.collection(DbContract.getGroupInviteRequests(groupID))
                .document(mUser.getUid())
                .set(DbContract.GroupObject.addInviteRequest(mUser.getDisplayName(), mUser.getEmail()));
    }

    private void acceptInvitation(final Group newGroup){
        WriteBatch batch = mDb.batch();

        DocumentReference invitation = mDb.document(DbContract.getInvitationToGroup(mUser.getUid(), newGroup.getId()));
        batch.delete(invitation);

        Map<String, Object> newGroupUser = DbContract.GroupObject.addGroupUser(mUser.getDisplayName(), mUser.getEmail());
        DocumentReference newGroupUserReference = mDb.document(DbContract.addOrUpdateGroupUser(newGroup.getId(), mUser.getUid()));
        batch.set(newGroupUserReference, newGroupUser);

        Map<String, Object> newUserGroup = DbContract.UserObject.newUserGroup(newGroup.getName(), mUser.getUid(), mUser.getEmail());
        DocumentReference userGroupsCollection = mDb.document(DbContract.addUserGroup(mUser.getUid(), newGroup.getId()));
        batch.set(userGroupsCollection, newUserGroup);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    Log.d(LOG_TAG, "Invitation acceptation failed.");
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), GroupActivity.class);
                intent.putExtra(GroupActivity.GROUP_ID_KEY, newGroup.getId());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(!closeDrawerIfOpened()) {
            if (mSearchView != null && mSearchView.isSearchOpen())
                mSearchView.closeSearch();
            else
                super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search_group) {
            if(mSearchView != null && mSearchView.getVisibility() == View.GONE) {
                mSearchView.setVisibility(View.VISIBLE);
                mSearchView.setMenuItem(item);
                mSearchView.showSearch();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id != R.id.nmenu_new_group)
            displaySelectedScreen(id);

        return super.onNavigationItemSelected(item);
    }

}
package com.dp;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.dp.adapters.GroupMembersPagerAdapter;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.User;
import com.dp.fragments.TabGroupInvitationsFragment;
import com.dp.fragments.TabGroupMembersFragment;
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
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManageMembersActivity extends BaseActivity {

    private static final String LOG_TAG = "ManageMembersActivity";
    public static final String GROUP_ID_KEY = "group_id";

    private Group mGroup;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R.id.pager)
    ViewPager mViewPager;

    private TabLayout.OnTabSelectedListener mTabSelectedListener;
    private ListenerRegistration mUsersUpdatesRegistration;
    private OnUsersDataChangeListener mOnDataChangeListener;
    private OnEditModeListener mOnEditModeListener;
    private OnActionModeListener mOnActionModeListener;

    public interface OnEditModeListener{
        void onStateChanged();
    }

    public interface OnActionModeListener{
        boolean onActionModeChanged();
        boolean getActualMode();
    }

    private Map<String, User> mInviteUserSearchResult = new HashMap<>();

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

            mInviteUserSearchResult.clear();
            List<String> foundUsersEmails = new ArrayList<>();

            for(DocumentSnapshot doc : result){
                String id = doc.getId();
                if(!mGroup.getUsers().containsKey(id)) {
                    String name = doc.getString(DbContract.UserObject.NAME);
                    String email = doc.getString(DbContract.UserObject.EMAIL);
                    mInviteUserSearchResult.put(email, new User(id, name, email));
                    foundUsersEmails.add(email);
                }
            }

            if(mSearchView != null && foundUsersEmails.size() > 0)
                mSearchView.setSuggestions(foundUsersEmails.toArray(new String[0]));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mToolbarType = ToolbarType.HOME_AS_UP_TABS;
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
                                mToolbar.setTitle(name);
                                runUsersRealtimeUpdates();
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

        ButterKnife.bind(this);

        mTabLayout.addTab(mTabLayout.newTab().setText(TabGroupMembersFragment.TITLE));
        mTabLayout.addTab(mTabLayout.newTab().setText(TabGroupInvitationsFragment.TITLE));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        GroupMembersPagerAdapter adapter = new GroupMembersPagerAdapter(
                getSupportFragmentManager(),
                mTabLayout.getTabCount(),
                groupID,
                mDb);

        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        mTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewPager.setCurrentItem(position);
                mToolbar.getMenu().clear();
                switch (position){
                    case 0:
                        mToolbar.inflateMenu(R.menu.members_menu);
                        break;
                    case 1:
                        mToolbar.inflateMenu(R.menu.invitations_menu);
                        if(mOnActionModeListener != null)
                            setActionMode(mOnActionModeListener.getActualMode());

                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };

        mTabLayout.addOnTabSelectedListener(mTabSelectedListener);

        if(mSearchView == null)
            return;

        String[] list = new String[]{"Clipcodes", "Android Tutorials", "Youtube Clipcodes Tutorials", "SearchView Clicodes", "Android Clipcodes", "Tutorials Clipcodes"};

        mSearchView.clearFocus();
        mSearchView.setSuggestions(list);
        mSearchView.setSubmitOnClick(true);
        mSearchView.setHintTextColor(getResources().getColor(R.color.textHintColor));
        mSearchView.setHint("User email");

        mSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String userEmail) {
                if(mInviteUserSearchResult.containsKey(userEmail)){
                    final User selectedUser = mInviteUserSearchResult.get(userEmail);
                    //Toast.makeText(getApplicationContext(), "User " + selectedUser.getName() + " invited", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ManageMembersActivity.this);
                    builder.setMessage("Invite user "
                            + selectedUser.getName()
                            + " (" + selectedUser.getEmail() + ") "
                            + " to the group '" + mGroup.getName() + "'?")
                            .setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    checkUserDataBeforeInvitation(selectedUser);
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

                mDb.collection(DbContract.usersRoot())
                        .orderBy(DbContract.UserObject.EMAIL)
                        .limit(5)
                        .startAt(newText)
                        .get()
                        .addOnCompleteListener(mOnCompleteListener);

                return false;
            }
        });
    }

    private void checkUserDataBeforeInvitation(final User newUser){
        mDb.document(DbContract.getExactUserGroup(newUser.getId(), mGroup.getId()))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(!task.isSuccessful()){
                            Log.d(LOG_TAG, "User group existence check failed. ");
                            return;
                        }

                        DocumentSnapshot doc = task.getResult();

                        if(doc.exists()){
                            Log.d(LOG_TAG, "User is already in group");
                            Toast.makeText(getApplicationContext(), "User is already in group", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDb.document(DbContract.getInviteRequestByUser(mGroup.getId(), newUser.getId()))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(!task.isSuccessful()){
                                            Log.d(LOG_TAG, "Invite request existence check failed. ");
                                            return;
                                        }

                                        DocumentSnapshot doc = task.getResult();

                                        if(doc.exists()){
                                            Log.d(LOG_TAG, "User already sent invitation request.");
                                            acceptInviteRequest(newUser);
                                            return;
                                        }

                                        sendInvitation(newUser.getId());
                                    }
                                });
                    }
                });
    }

    private void sendInvitation(String userGoogleID){
        mDb.collection(DbContract.getUserInvitations(userGoogleID))
                .document(mGroup.getId())
                .set(DbContract.UserObject.addInvitation(mGroup.getName(), mGroup.getAdminGoogleID(), mGroup.getAdminEmail()));
    }

    private void acceptInviteRequest(User newUser){
        WriteBatch batch = mDb.batch();

        DocumentReference inviteRequest = mDb.document(DbContract.getInviteRequestByUser(mGroup.getId(), newUser.getId()));
        batch.delete(inviteRequest);

        Map<String, Object> newGroupUser = DbContract.GroupObject.addGroupUser(newUser.getName(), newUser.getEmail());
        DocumentReference newGroupUserReference = mDb.document(DbContract.addOrUpdateGroupUser(mGroup.getId(), newUser.getId()));
        batch.set(newGroupUserReference, newGroupUser);

        Map<String, Object> newUserGroup = DbContract.UserObject.newUserGroup(mGroup.getName(), newUser.getId(), newUser.getEmail());
        DocumentReference userGroupsCollection = mDb.document(DbContract.addUserGroup(newUser.getId(), mGroup.getId()));
        batch.set(userGroupsCollection, newUserGroup);

        batch.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        runUsersRealtimeUpdates();
    }

    @Override
    public void onBackPressed() {
        if(mSearchView != null && mSearchView.isSearchOpen())
            mSearchView.closeSearch();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.members_menu, menu);

        return true;
    }

    private void setActionMode(boolean isAcceptModeEnabled){
        if(isAcceptModeEnabled){
            mToolbar.getMenu().findItem(R.id.action_accept_mode).setVisible(false);
            mToolbar.getMenu().findItem(R.id.action_decline_mode).setVisible(true);
        } else{
            mToolbar.getMenu().findItem(R.id.action_accept_mode).setVisible(true);
            mToolbar.getMenu().findItem(R.id.action_decline_mode).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_invite_user:
                if(mSearchView != null && mSearchView.getVisibility() == View.GONE) {
                    mSearchView.setVisibility(View.VISIBLE);
                    mSearchView.setMenuItem(item);
                    mSearchView.showSearch();
                }
                return true;
            case R.id.action_edit_mode:
                if(mOnEditModeListener != null)
                    mOnEditModeListener.onStateChanged();
                return true;
            case R.id.action_accept_mode:
                setActionMode(mOnActionModeListener.onActionModeChanged());
                return true;
            case R.id.action_decline_mode:
                setActionMode(mOnActionModeListener.onActionModeChanged());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setOnUsersDataChangeListener(OnUsersDataChangeListener listener){
        mOnDataChangeListener = listener;
    }

    public void setOnEditModeListener(OnEditModeListener listener){
        mOnEditModeListener = listener;
    }

    public void setOnActionModeListener(OnActionModeListener listener){
        mOnActionModeListener = listener;
    }

    private void runUsersRealtimeUpdates(){
        if(mGroup == null)
            return;

        if(mGroup.getId() == null)
            return;

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
                                    User newUser = new User(id, doc.getString(DbContract.UserObject.NAME), doc.getString(DbContract.UserObject.EMAIL));
                                    if(doc.contains(DbContract.UserObject.LATITUDE) && doc.contains(DbContract.UserObject.LONGITUDE)){
                                        newUser.setLatitude(doc.getDouble(DbContract.UserObject.LATITUDE));
                                        newUser.setLongitude(doc.getDouble(DbContract.UserObject.LONGITUDE));
                                    }
                                    if(mOnDataChangeListener != null){
                                        mOnDataChangeListener.onUserAdded(newUser);
                                    }
                                    mGroup.addOrUpdateUser(newUser);
                                    break;
                                case REMOVED:
                                    if(mOnDataChangeListener != null){
                                        mOnDataChangeListener.onUserRemoved(id);
                                    }
                                    mGroup.removeUser(id);
                                    break;
                                case MODIFIED:
                                    User modifiedUser = mGroup.getUser(id);
                                    if(doc.contains(DbContract.UserObject.LATITUDE) && doc.contains(DbContract.UserObject.LONGITUDE)){
                                        modifiedUser.setLatitude(doc.getDouble(DbContract.UserObject.LATITUDE));
                                        modifiedUser.setLongitude(doc.getDouble(DbContract.UserObject.LONGITUDE));

                                        if(mOnDataChangeListener != null){
                                            mOnDataChangeListener.onUserModified(modifiedUser);
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

    @Override
    protected void onStop() {
        super.onStop();

        if(mUsersUpdatesRegistration != null)
            mUsersUpdatesRegistration.remove();
    }
}

package com.dp.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dp.ManageMembersActivity;
import com.dp.R;
import com.dp.adapters.GroupInvitationsAdapter;
import com.dp.adapters.GroupMembersAdapter;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class TabGroupInvitationsFragment extends Fragment {

    public static final String TITLE = "Invitations";
    private static final String LOG_TAG = "TabGroupInvitationsFrag";

    @BindView(R.id.rv_group_users)
    RecyclerView mRecyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.no_data_message)
    TextView mNoDataMessage;

    private Unbinder mUnbinder;
    private GroupInvitationsAdapter mAdapter;

    private ListenerRegistration mInvitationsUpdatesRegistration;
    private FirebaseFirestore mDb;
    private Group mGroup;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab_fragment_list, container, false);

        mUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new GroupInvitationsAdapter();
        mAdapter.setOnInviteRequestActionListener(new GroupInvitationsAdapter.OnInviteRequestActionListener() {
            @Override
            public void onAccepted(User user) {
                WriteBatch batch = mDb.batch();

                DocumentReference inviteRequest = mDb.document(DbContract.getInviteRequestByUser(mGroup.getId(), user.getId()));
                batch.delete(inviteRequest);

                Map<String, Object> newGroupUser = DbContract.GroupObject.addGroupUser(user.getName(), user.getEmail());
                DocumentReference newGroupUserReference = mDb.document(DbContract.addOrUpdateGroupUser(mGroup.getId(), user.getId()));
                batch.set(newGroupUserReference, newGroupUser);

                Map<String, Object> newUserGroup = DbContract.UserObject.newUserGroup(mGroup.getName(), user.getId(), user.getEmail());
                DocumentReference userGroupsCollection = mDb.document(DbContract.addUserGroup(user.getId(), mGroup.getId()));
                batch.set(userGroupsCollection, newUserGroup);

                batch.commit();
            }

            @Override
            public void onDeclined(User user) {
                WriteBatch batch = mDb.batch();

                DocumentReference inviteRequest = mDb.document(DbContract.getInviteRequestByUser(mGroup.getId(), user.getId()));
                batch.delete(inviteRequest);

                batch.commit();
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        ((ManageMembersActivity)getActivity()).setOnActionModeListener(new ManageMembersActivity.OnActionModeListener() {
            @Override
            public boolean onActionModeChanged() {
                return mAdapter.swapEditMode();
            }

            @Override
            public boolean getActualMode() {
                return mAdapter.getActualMode();
            }
        });
    }

    private void runMyInvitationsRealtimeUpdates(){

        Query query = mDb.collection(DbContract.getGroupInviteRequests(mGroup.getId()));
        mInvitationsUpdatesRegistration = query.addSnapshotListener(
                new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        if(e != null){
                            Log.w(LOG_TAG, "listen:error", e);
                            dataNotFound();
                            return;
                        }

                        if(documentSnapshots.isEmpty()) {
                            dataNotFound();
                            return;
                        }

                        for(DocumentChange dc : documentSnapshots.getDocumentChanges()){
                            DocumentSnapshot doc = dc.getDocument();
                            switch (dc.getType()){
                                case ADDED:
                                    if(mAdapter.getItemCount() == 0)
                                        showList();
                                    mAdapter.addInviteRequest(new User(doc.getId(), doc.getString(DbContract.UserObject.NAME), doc.getString(DbContract.UserObject.EMAIL)));
                                    break;
                                case REMOVED:
                                    mAdapter.removeInviteRequest(doc.getId());
                                    if(mAdapter.getItemCount() == 0)
                                        showNoDataMessage();
                                    break;
                            }
                        }

                        dataLoaded();
                    }
                }
        );
    }

    private void dataLoaded(){
        mAdapter.notifyDataSetChanged();
        showList();
    }

    private void dataNotFound(){
        showNoDataMessage();
    }

    private void showList(){
        mProgressBar.setVisibility(View.GONE);
        mNoDataMessage.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showNoDataMessage(){
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mNoDataMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mInvitationsUpdatesRegistration != null)
            mInvitationsUpdatesRegistration.remove();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    public void setData(FirebaseFirestore db, String groupID){
        mDb = db;
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
                                runMyInvitationsRealtimeUpdates();
                            } else {
                                Log.d(LOG_TAG, "No such document");
                            }
                        } else {
                            Log.d(LOG_TAG, "Group not found");
                        }
                    }
                });
    }

}
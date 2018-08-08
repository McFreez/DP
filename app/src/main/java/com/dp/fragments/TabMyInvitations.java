package com.dp.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dp.MyGroupsActivity;
import com.dp.R;
import com.dp.adapters.MyInvitationsAdapter;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class TabMyInvitations extends Fragment {

    private static final String LOG_TAG = "TabMyInvitations";
    public static final String TITLE = "Invitations";

    @BindView(R.id.rv_group_users)
    RecyclerView mRecyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.no_data_message)
    TextView mNoDataMessage;

    private Unbinder mUnbinder;
    private MyInvitationsAdapter mAdapter;

    private ListenerRegistration mInvitationsUpdatesRegistration;
    private FirebaseFirestore mDb;
    private FirebaseUser mUser;

    private boolean isDataUpdateStarted = false;

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

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        mProgressBar.setVisibility(View.VISIBLE);

        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new MyInvitationsAdapter();
        mAdapter.setOnInvitationActionListener(new MyInvitationsAdapter.OnInvitationActionListener() {
            @Override
            public void onAccepted(final Group group) {
                mDb.document(DbContract.getGroup(group.getId()))
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(!task.isSuccessful()){
                                    Log.d(LOG_TAG, "Group existence check failed.");
                                    return;
                                }

                                DocumentSnapshot doc = task.getResult();

                                if(!doc.exists()){
                                    Log.d(LOG_TAG, "Such group does not exist.");
                                    WriteBatch batch = mDb.batch();

                                    DocumentReference invitation = mDb.document(DbContract.getInvitationToGroup(mUser.getUid(), group.getId()));
                                    batch.delete(invitation);

                                    batch.commit();
                                    return;
                                }

                                WriteBatch batch = mDb.batch();

                                DocumentReference invitation = mDb.document(DbContract.getInvitationToGroup(mUser.getUid(), group.getId()));
                                batch.delete(invitation);

                                Map<String, Object> newGroupUser = DbContract.GroupObject.addGroupUser(mUser.getDisplayName(), mUser.getEmail());
                                DocumentReference newGroupUserReference = mDb.document(DbContract.addOrUpdateGroupUser(group.getId(), mUser.getUid()));
                                batch.set(newGroupUserReference, newGroupUser);

                                Map<String, Object> newUserGroup = DbContract.UserObject.newUserGroup(group.getName(), mUser.getUid(), mUser.getEmail());
                                DocumentReference userGroupsCollection = mDb.document(DbContract.addUserGroup(mUser.getUid(), group.getId()));
                                batch.set(userGroupsCollection, newUserGroup);

                                batch.commit();
                            }
                        });
            }

            @Override
            public void onDeclined(Group group) {
                WriteBatch batch = mDb.batch();

                DocumentReference invitation = mDb.document(DbContract.getInvitationToGroup(mUser.getUid(), group.getId()));
                batch.delete(invitation);

                batch.commit();
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        ((MyGroupsActivity)getActivity()).setOnActionModeListener(new MyGroupsActivity.OnActionModeListener() {
            @Override
            public boolean onActionModeChanged() {
                return mAdapter.swapEditMode();
            }

            @Override
            public boolean getActualMode() {
                return mAdapter.getActualMode();
            }
        });

        runMyInvitationsRealtimeUpdates();
    }

    @Override
    public void onStart() {
        super.onStart();

        if(!isDataUpdateStarted)
            runMyInvitationsRealtimeUpdates();
    }

    private void runMyInvitationsRealtimeUpdates(){
        isDataUpdateStarted = true;

        mAdapter.clear();

        Query query = mDb.collection(DbContract.getUserInvitations(mUser.getUid()));
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
                                    mAdapter.addInvitation(new Group(doc.getId(), doc.getString(DbContract.GroupObject.NAME)));
                                    break;
                                case REMOVED:
                                    mAdapter.removeInvitation(doc.getId());
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
        isDataUpdateStarted = false;
        mAdapter.notifyDataSetChanged();
        showList();
    }

    private void dataNotFound(){
        isDataUpdateStarted = false;
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

    public void setDb(FirebaseFirestore db){
        mDb = db;
    }
}
package com.dp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dp.NewGroupActivity;
import com.dp.R;
import com.dp.adapters.MyGroupsAdapter;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class TabMyGroups extends Fragment {

    private static final String LOG_TAG = "TabMyGroups";
    public static final String TITLE = "Groups";

    @BindView(R.id.rv_group_users)
    RecyclerView mRecyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.fab)
    FloatingActionButton mFab;
    @BindView(R.id.no_data_message)
    TextView mNoDataMessage;

    private Unbinder mUnbinder;
    private MyGroupsAdapter mAdapter;

    private ListenerRegistration mGroupsUpdatesRegistration;
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

        mFab.setVisibility(View.VISIBLE);
        mFab.setImageResource(R.drawable.ic_add_white_24dp);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NewGroupActivity.class);
                startActivity(intent);
            }
        });

        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new MyGroupsAdapter();
        mRecyclerView.setAdapter(mAdapter);

        runMyGroupsRealtimeUpdates();
    }

    @Override
    public void onStart() {
        super.onStart();

        if(!isDataUpdateStarted)
            runMyGroupsRealtimeUpdates();
    }

    private void runMyGroupsRealtimeUpdates(){
        isDataUpdateStarted = true;

        mAdapter.clear();

        Query query = mDb.collection(DbContract.getUserGroups(mUser.getUid()));
        mGroupsUpdatesRegistration = query.addSnapshotListener(
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
                                    mAdapter.addGroup(new Group(doc.getId(), doc.getString(DbContract.GroupObject.NAME)));
                                    break;
                                case REMOVED:
                                    mAdapter.removeGroup(doc.getId());
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

        if(mGroupsUpdatesRegistration != null)
            mGroupsUpdatesRegistration.remove();
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

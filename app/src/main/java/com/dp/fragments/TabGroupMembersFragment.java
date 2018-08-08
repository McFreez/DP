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
import android.widget.Toast;

import com.dp.ManageMembersActivity;
import com.dp.R;
import com.dp.adapters.GroupMembersAdapter;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class TabGroupMembersFragment extends Fragment {

    public static final String TITLE = "Members";
    private static final String LOG_TAG = "TabGroupMembersFragment";

    @BindView(R.id.rv_group_users) RecyclerView mRecyclerView;
    @BindView(R.id.progress_bar) ProgressBar mProgressBar;

    private FirebaseFirestore mDb;

    private Unbinder mUnbinder;
    private GroupMembersAdapter mAdapter;

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

        mAdapter = (mGroup != null)
                ? new GroupMembersAdapter(mGroup.getUsers())
                : new GroupMembersAdapter(null);

        mAdapter.setOnDeleteMemberClickListener(new GroupMembersAdapter.OnDeleteMemberClickListener() {
            @Override
            public void OnClick(final User user) {
                if(mGroup == null)
                    return;

                WriteBatch batch = mDb.batch();

                DocumentReference userInGroup = mDb.document(DbContract.getExactGroupUser(mGroup.getId(), user.getId()));
                batch.delete(userInGroup);

                DocumentReference userGroup = mDb.document(DbContract.getExactUserGroup(user.getId(), mGroup.getId()));
                batch.delete(userGroup);

                batch.commit();/*.addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(!task.isSuccessful()){
                            return;
                        }

                        mAdapter.removeUser(user.getId());
                    }
                });*/
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        ManageMembersActivity activity = (ManageMembersActivity)getActivity();
        activity.setOnUsersDataChangeListener(new OnUsersDataChangeListener() {
            @Override
            public void onUserAdded(User user) {
                mAdapter.addUser(user);
            }

            @Override
            public void onUserRemoved(String userGoogleID) {
                mAdapter.removeUser(userGoogleID);
            }

            @Override
            public void onUserModified(User user) {
                mAdapter.updateUser(user);
            }

            @Override
            public void onClear() {
            }
        });

        activity.setOnEditModeListener(new ManageMembersActivity.OnEditModeListener() {
            @Override
            public void onStateChanged() {
                mAdapter.swapEditModeState();
            }
        });
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

package com.dp.fragments;

import android.content.Intent;
import android.hardware.camera2.DngCreator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dp.GroupActivity;
import com.dp.R;
import com.dp.db.DbContract;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class NewGroupFragment extends Fragment {

    private static final String LOG_TAG = "NewGroupFragment";

    @BindView(R.id.et_groupName)
    EditText mGroupName;
    @BindView(R.id.btn_createGroup)
    Button mCreate;

    private Unbinder mUnbinder;

    private FirebaseFirestore mDb;
    private FirebaseUser mUser;

    public NewGroupFragment(){
        super();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_new_group, container, false);

        mUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("New group");

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        mCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mGroupName.getText().length() <= 0){
                    Toast.makeText(getContext(), "Input group name first", Toast.LENGTH_SHORT).show();
                    return;
                }

                mDb.collection(DbContract.getUserGroups(mUser.getUid()))
                        .whereEqualTo(DbContract.GroupObject.NAME, mGroupName.getText().toString())
                        .whereEqualTo(DbContract.GroupObject.ADMIN_ID, mUser.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(!task.isSuccessful()){
                                    Log.d(LOG_TAG, "Failed to check group existence.");
                                    return;
                                }

                                QuerySnapshot list = task.getResult();

                                if(!list.isEmpty()){
                                    Log.d(LOG_TAG, "Group already exists");
                                    Toast.makeText(getContext(), "You already have group '" + mGroupName.getText().toString() + "'.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                WriteBatch batch = mDb.batch();

                                Map<String, Object> newGroup = DbContract.GroupObject.createGroup(mGroupName.getText().toString(), mUser.getUid(), mUser.getEmail());
                                DocumentReference newGroupReference = mDb.collection(DbContract.addGroup()).document();
                                batch.set(newGroupReference, newGroup);

                                final String newGroupID = newGroupReference.getId();

                                Map<String, Object> newGroupUser = DbContract.GroupObject.addGroupUser(mUser.getDisplayName(), mUser.getEmail());
                                DocumentReference newGroupUserReference = mDb.document(DbContract.addOrUpdateGroupUser(newGroupID, mUser.getUid()));
                                batch.set(newGroupUserReference, newGroupUser);

                                Map<String, Object> newUserGroup = DbContract.UserObject.newUserGroup(mGroupName.getText().toString(), mUser.getUid(), mUser.getEmail());
                                DocumentReference userGroupsCollection = mDb.document(DbContract.addUserGroup(mUser.getUid(), newGroupID));
                                batch.set(userGroupsCollection, newUserGroup);

                                batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(!task.isSuccessful()){
                                            Log.d(LOG_TAG, "Group creation failed.");
                                            return;
                                        }

                                        Intent intent = new Intent(getActivity(), GroupActivity.class);
                                        intent.putExtra(GroupActivity.GROUP_ID_KEY, newGroupID);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
            }
        });
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

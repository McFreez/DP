package com.dp.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dp.R;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.ViewHolder> {

    private static final String LOG_TAG = "GroupMembersAdapter";

    private FirebaseUser mUser;

    private List<User> mMembers = new ArrayList<>();

    private OnDeleteMemberClickListener mListener;

    private boolean isEditModeEnabled = false;

    public interface OnDeleteMemberClickListener {
        void OnClick(User user);
    }

    public GroupMembersAdapter(Map<String, User> membersMap){

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(membersMap == null)
            return;

        for(Map.Entry<String, User> userEntry : membersMap.entrySet()){
            mMembers.add(userEntry.getValue());
        }
    }

    public void setOnDeleteMemberClickListener(OnDeleteMemberClickListener listener){
        mListener = listener;
    }

    public void swapEditModeState(){
        isEditModeEnabled = !isEditModeEnabled;

        notifyDataSetChanged();
    }

    public void addUser(User newUser){
        mMembers.add(newUser);
        notifyDataSetChanged();
    }

    public void removeUser(String userGoogleID){
        if(mMembers.size() == 0) {
            Log.d(LOG_TAG, "Empty members list, can`t remove user.");
            return;
        }

        int index = -1;

        for(int i = 0; i < getItemCount(); i++){
            if(mMembers.get(i).getId().equals(userGoogleID)){
                index = i;
                break;
            }
        }

        if(index == -1) {
            Log.d(LOG_TAG, "Can`t remove member, no such member in group.");
            return;
        }

        mMembers.remove(index);
        notifyItemRemoved(index);
    }

    public void updateUser(User updatedUser){
        if(mMembers.size() == 0) {
            Log.d(LOG_TAG, "Empty members list, can`t update user.");
            return;
        }

        int index = -1;

        for(int i = 0; i < getItemCount(); i++){
            if(mMembers.get(i).getId().equals(updatedUser.getId())){
                index = i;
                break;
            }
        }

        if(index == -1) {
            Log.d(LOG_TAG, "Can`t update member, no such member in group.");
            return;
        }

        mMembers.get(index).setName(updatedUser.getName());
        mMembers.get(index).setLatitude(updatedUser.getLatitude());
        mMembers.get(index).setLongitude(updatedUser.getLongitude());

        notifyItemChanged(index);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);

        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final User user = mMembers.get(position);

        holder.bind(user, isEditModeEnabled, mUser.getUid());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.OnClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMembers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mMemberName;
        public ImageView mDeleteButton;

        public ViewHolder(View view) {
            super(view);

            mMemberName = view.findViewById(R.id.tv_memberName);
            mDeleteButton = view.findViewById(R.id.delete);

        }

        void bind(final User user, boolean isEditModeEnabled, final String myGoogleID){
            String userName = (user.getId().equals(myGoogleID))
                    ? user.getName() + " (Me)"
                    : user.getName();
            mMemberName.setText(userName);
            if(isEditModeEnabled && !user.getId().equals(myGoogleID)) {
                mDeleteButton.setVisibility(View.VISIBLE);
            } else
                mDeleteButton.setVisibility(View.GONE);
        }
    }
}

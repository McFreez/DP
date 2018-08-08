package com.dp.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupInvitationsAdapter extends RecyclerView.Adapter<GroupInvitationsAdapter.ViewHolder> {

    private static final String LOG_TAG = "GroupInvitationsAdapter";

    private List<User> mInviteRequests = new ArrayList<>();
    private OnInviteRequestActionListener mListener;
    private boolean isAcceptModeEnabled = true;

    public interface OnInviteRequestActionListener{
        void onAccepted(User user);
        void onDeclined(User user);
    }

    public GroupInvitationsAdapter(){
    }

    public void setOnInviteRequestActionListener(OnInviteRequestActionListener listener){
        mListener = listener;
    }

    public void addInviteRequest(User newInviteRequest){
        mInviteRequests.add(newInviteRequest);
        notifyItemInserted(mInviteRequests.size() - 1);
    }

    public void removeInviteRequest(String inviteRequestID){
        if(mInviteRequests.size() == 0)
            return;

        int index = -1;

        for(int i = 0; i < mInviteRequests.size(); i++){
            if(mInviteRequests.get(i).getId().equals(inviteRequestID)){
                index = i;
                break;
            }
        }

        if(index == -1)
            return;

        mInviteRequests.remove(index);
        notifyItemRemoved(index);
    }

    public boolean swapEditMode(){
        isAcceptModeEnabled = !isAcceptModeEnabled;
        notifyDataSetChanged();
        return isAcceptModeEnabled;
    }

    public boolean getActualMode(){
        return isAcceptModeEnabled;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.invitation_item, parent, false);

        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final User user = mInviteRequests.get(position);
        holder.bind(user, isAcceptModeEnabled);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        holder.mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onAccepted(user);
            }
        });

        holder.mDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onDeclined(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mInviteRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mUserName;
        public ImageView mAcceptButton;
        public ImageView mDeclineButton;

        public ViewHolder(View view) {
            super(view);

            mUserName = view.findViewById(R.id.tv_name);
            mAcceptButton = view.findViewById(R.id.accept_invitation);
            mDeclineButton = view.findViewById(R.id.decline_invitation);
        }

        void bind(final User user, boolean isAcceptModeEnabled){
            mUserName.setText(user.getName());
            if(isAcceptModeEnabled) {
                mDeclineButton.setVisibility(View.GONE);
                mAcceptButton.setVisibility(View.VISIBLE);
            } else {
                mAcceptButton.setVisibility(View.GONE);
                mDeclineButton.setVisibility(View.VISIBLE);
            }
        }
    }
}

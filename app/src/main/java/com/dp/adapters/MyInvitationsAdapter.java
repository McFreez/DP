package com.dp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dp.R;
import com.dp.dto.Group;
import com.dp.dto.User;

import java.util.ArrayList;
import java.util.List;

public class MyInvitationsAdapter extends RecyclerView.Adapter<MyInvitationsAdapter.ViewHolder> {

    private Context mContext;
    private List<Group> mInvitations = new ArrayList<>();
    private OnInvitationActionListener mListener;

    private boolean isAcceptModeEnabled = true;

    public interface OnInvitationActionListener{
        void onAccepted(Group group);
        void onDeclined(Group group);
    }

    public MyInvitationsAdapter(){
    }

    public void setOnInvitationActionListener(OnInvitationActionListener listener){
        mListener = listener;
    }

    public void addInvitation(Group newInvitation){
        mInvitations.add(newInvitation);
        notifyItemInserted(mInvitations.size() - 1);
    }

    public void removeInvitation(String invitationID){
        if(mInvitations.size() == 0)
            return;

        int index = -1;

        for(int i = 0; i < mInvitations.size(); i++){
            if(mInvitations.get(i).getId().equals(invitationID)){
                index = i;
                break;
            }
        }

        if(index == -1)
            return;

        mInvitations.remove(index);
        notifyItemRemoved(index);
    }

    public void clear(){
        mInvitations.clear();
        notifyDataSetChanged();
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

        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.invitation_item, parent, false);

        ViewHolder vh = new ViewHolder(view);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Group group = mInvitations.get(position);
        holder.bind(group, isAcceptModeEnabled);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        holder.mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onAccepted(group);
            }
        });

        holder.mDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onDeclined(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mInvitations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mGroupName;
        public ImageView mAcceptButton;
        public ImageView mDeclineButton;

        public ViewHolder(View view) {
            super(view);

            mGroupName = view.findViewById(R.id.tv_name);
            mAcceptButton = view.findViewById(R.id.accept_invitation);
            mDeclineButton = view.findViewById(R.id.decline_invitation);
        }

        void bind(Group group, boolean isAcceptModeEnabled){
            mGroupName.setText(group.getName());
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
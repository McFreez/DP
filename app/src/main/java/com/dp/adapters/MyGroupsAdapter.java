package com.dp.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dp.GroupActivity;
import com.dp.R;
import com.dp.dto.Group;

import java.util.ArrayList;
import java.util.List;

public class MyGroupsAdapter extends RecyclerView.Adapter<MyGroupsAdapter.ViewHolder> {

    private Context mContext;
    private List<Group> mGroups = new ArrayList<>();

    public MyGroupsAdapter(){
    }

    public void addGroup(Group newGroup){
        mGroups.add(newGroup);
        notifyItemInserted(mGroups.size() - 1);
    }

    public void removeGroup(String groupID){
        //int position = mGroups.indexOf(groupToRemove);
        if(mGroups.size() == 0)
            return;

        int index = -1;

        for(int i = 0; i < mGroups.size(); i++){
            if(mGroups.get(i).getId().equals(groupID)){
                index = i;
                break;
            }
        }

        if(index == -1)
            return;

        mGroups.remove(index);
        notifyItemRemoved(index);
    }

    public void clear(){
        mGroups.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.group_item, parent, false);

        ViewHolder vh = new ViewHolder(view);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(mGroups.get(position));
        final String groupID = mGroups.get(position).getId();

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GroupActivity.class);
                intent.putExtra(GroupActivity.GROUP_ID_KEY, groupID);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mGroupName;

        public ViewHolder(View view) {
            super(view);

            mGroupName = view.findViewById(R.id.tv_groupName);
        }

        void bind(Group group){
            mGroupName.setText(group.getName());
        }
    }
}

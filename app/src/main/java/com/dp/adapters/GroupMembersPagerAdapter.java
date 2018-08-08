package com.dp.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.dp.fragments.TabGroupInvitationsFragment;
import com.dp.fragments.TabGroupMembersFragment;
import com.google.firebase.firestore.FirebaseFirestore;

public class GroupMembersPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private FirebaseFirestore mDb;
    private String mGroupID;

    public GroupMembersPagerAdapter(FragmentManager fm, int NumOfTabs, String groupID, FirebaseFirestore db) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        mGroupID = groupID;
        mDb = db;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment tab = null;

        switch (position) {
            case 0:
                tab = new TabGroupMembersFragment();
                ((TabGroupMembersFragment)tab).setData(mDb, mGroupID);
                break;
            case 1:
                tab = new TabGroupInvitationsFragment();
                ((TabGroupInvitationsFragment)tab).setData(mDb, mGroupID);
                break;
        }

        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}

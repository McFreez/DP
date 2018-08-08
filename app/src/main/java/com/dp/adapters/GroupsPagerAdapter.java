package com.dp.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.dp.dto.Group;
import com.dp.fragments.TabGroupInvitationsFragment;
import com.dp.fragments.TabGroupMembersFragment;
import com.dp.fragments.TabMyGroups;
import com.dp.fragments.TabMyInvitations;
import com.google.firebase.firestore.FirebaseFirestore;

public class GroupsPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private FirebaseFirestore mDb;

    public GroupsPagerAdapter(FragmentManager fm, int NumOfTabs, FirebaseFirestore db) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        mDb = db;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment tab = null;

        switch (position) {
            case 0:
                tab = new TabMyGroups();
                ((TabMyGroups)tab).setDb(mDb);
                break;
            case 1:
                tab = new TabMyInvitations();
                ((TabMyInvitations)tab).setDb(mDb);
                break;
        }

        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
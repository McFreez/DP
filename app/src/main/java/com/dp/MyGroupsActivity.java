package com.dp;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.dp.adapters.GroupMembersPagerAdapter;
import com.dp.adapters.GroupsPagerAdapter;
import com.dp.fragments.GroupFragment;
import com.dp.fragments.TabGroupInvitationsFragment;
import com.dp.fragments.TabGroupMembersFragment;
import com.dp.fragments.TabMyGroups;
import com.dp.fragments.TabMyInvitations;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyGroupsActivity extends BaseActivity {

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R.id.pager)
    ViewPager mViewPager;

    private OnActionModeListener mOnActionModeListener;

    public interface OnActionModeListener{
        boolean onActionModeChanged();
        boolean getActualMode();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mToolbarType = ToolbarType.MENU_TABS;
        super.onCreate(savedInstanceState);

        mNavigationView.setCheckedItem(R.id.nmenu_groups);

        ButterKnife.bind(this);

        mTabLayout.addTab(mTabLayout.newTab().setText(TabMyGroups.TITLE));
        mTabLayout.addTab(mTabLayout.newTab().setText(TabMyInvitations.TITLE));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        GroupsPagerAdapter adapter = new GroupsPagerAdapter(
                getSupportFragmentManager(),
                mTabLayout.getTabCount(),
                mDb);

        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewPager.setCurrentItem(position);
                mToolbar.getMenu().clear();
                switch (position){
                    case 1:
                        mToolbar.inflateMenu(R.menu.invitations_menu);
                        if(mOnActionModeListener != null)
                            setActionMode(mOnActionModeListener.getActualMode());

                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    public void setOnActionModeListener(OnActionModeListener listener){
        mOnActionModeListener = listener;
    }

    private void setActionMode(boolean isAcceptModeEnabled){
        if(isAcceptModeEnabled){
            mToolbar.getMenu().findItem(R.id.action_accept_mode).setVisible(false);
            mToolbar.getMenu().findItem(R.id.action_decline_mode).setVisible(true);
        } else{
            mToolbar.getMenu().findItem(R.id.action_accept_mode).setVisible(true);
            mToolbar.getMenu().findItem(R.id.action_decline_mode).setVisible(false);
        }
    }

    @Override
    public void onBackPressed() {
        closeDrawerIfOpened();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_accept_mode || id == R.id.action_decline_mode) {
            setActionMode(mOnActionModeListener.onActionModeChanged());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id != R.id.nmenu_groups)
            displaySelectedScreen(id);

        return super.onNavigationItemSelected(item);
    }
}
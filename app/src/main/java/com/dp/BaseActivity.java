package com.dp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener  {

    private static final int RC_SIGN_IN = 68;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @Nullable @BindView(R.id.mysearch) MaterialSearchView mSearchView;
    @Nullable @BindView(R.id.drawer_layout) DrawerLayout mDrawer;
    @Nullable @BindView(R.id.nav_view) NavigationView mNavigationView;

    protected FirebaseUser mUser;
    protected FirebaseFirestore mDb;

    protected ToolbarType mToolbarType = ToolbarType.MENU_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mToolbarType == ToolbarType.MENU_TABS || mToolbarType == ToolbarType.HOME_AS_UP_TABS){
            setContentView(R.layout.activity_main_tabs);
        } else
            setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mDb = FirebaseFirestore.getInstance();

        if(mToolbarType == ToolbarType.HOME_AS_UP_NORMAL || mToolbarType == ToolbarType.HOME_AS_UP_TABS){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            return;
        }

        if(mDrawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            mDrawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        if(mNavigationView != null)
            mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mUser == null)
            Auth();
    }

    private void Auth(){
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        if(mToolbarType == ToolbarType.MENU_NORMAL || mToolbarType == ToolbarType.MENU_TABS)
            authCompleted(mUser);
    }

    private void authCompleted(FirebaseUser user){
        setUserData(user);
    }

    private void setUserData(FirebaseUser user){
        if(mNavigationView == null)
            return;

        View header = mNavigationView.getHeaderView(0);
        TextView userName = (TextView) header.findViewById(R.id.tv_userName);
        if(userName != null)
            userName.setText(user.getDisplayName());

        Log.d("Auth", "Name " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        TextView userEmail = (TextView) header.findViewById(R.id.tv_userEmail);
        if(userEmail != null)
            userEmail.setText(user.getEmail());

        Log.d("Auth", "Email " + user.getEmail());
    }

    protected boolean closeDrawerIfOpened(){
        if(mDrawer == null)
            return false;

        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
            return true;
        } else
            return false;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        if(mDrawer != null)
            mDrawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public void displaySelectedScreen(int id){
        Intent intent = null;

        switch (id){
            case R.id.nmenu_new_group:
                intent = new Intent(this, NewGroupActivity.class);
                break;
            case R.id.nmenu_groups:
                intent = new Intent(this, MyGroupsActivity.class);
                break;
            case R.id.nmenu_exit:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // user is now signed out
                                finish();
                            }
                        });
                break;
        }

        if(intent != null){
            startActivity(intent);
        }
    }
}

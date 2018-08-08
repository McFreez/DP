package com.dp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.dp.db.DbContract;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.Scopes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 68;

    protected FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Auth();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void Auth(){
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mUser != null) {
            authCompleted();
            return;
        }

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER)
                        .setPermissions(Arrays.asList(Scopes.EMAIL))
                        /*.setPermissions(Arrays.asList(Scopes.PROFILE))*/
                        .build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mUser = FirebaseAuth.getInstance().getCurrentUser();

                updateUserData();
                authCompleted();
                // ...
            } else {
                // Sign in failed, check response for error code
                // ...
                mUser = null;
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void updateUserData(){
        String userName = mUser.getDisplayName();
        String userEmail = mUser.getEmail();

        Map<String, Object> user = DbContract.UserObject.updateUserData(userName, userEmail);

        FirebaseFirestore.getInstance().document(DbContract.addOrUpdateUser(mUser.getUid()))
                .set(user, SetOptions.merge());
    }

    private void authCompleted(){
        Intent intent = new Intent(this, MyGroupsActivity.class);
        startActivity(intent);
        finish();
    }
}

package com.funintended.morsepals;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.SignInButton;
import com.google.example.games.basegameutils.BaseGameActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by dustin on 1/17/14. :)
 */
public class AuthActivity extends BaseGameActivity{

    @InjectView(R.id.sign_in_button)
    SignInButton mSignInButton;

    @InjectView(R.id.sign_out_button)
    Button mSignOutButton;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_auth);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.sign_in_button)
    void requestSignIn() {
        beginUserInitiatedSignIn();
    }

    @OnClick(R.id.sign_out_button)
    void requestSignOut() {
        signOut();

        // show sign-in button, hide the sign-out button
        mSignInButton.setVisibility(View.VISIBLE);
        mSignOutButton.setVisibility(View.GONE);
    }

    @Override
    public void onSignInFailed() {
        // Sign in has failed. So show the user the sign-in button.
        mSignInButton.setVisibility(View.VISIBLE);
        mSignOutButton.setVisibility(View.GONE);
    }

    @Override
    public void onSignInSucceeded() {
        // show sign-out button, hide the sign-in button
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

        startActivity(new Intent(this,MainActivity.class));
        finish();
    }

}

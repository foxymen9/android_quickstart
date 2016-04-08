/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * For more information on setting up and running this sample code, see
 * https://developers.google.com/firebase/docs/durable-links/android
 */

package com.google.firebase.quickstart.invites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.measurement.AppMeasurement;

/**
 * Main Activity for sending App Invites and launching the DeepLinkActivity when an
 * App Invite is received.
 */
public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_INVITE = 0;

    // [START define_variables]
    private GoogleApiClient mGoogleApiClient;
    private AppMeasurement mMeasurement;
    // [END define_variables]

    // [START on_create]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // [START_EXCLUDE]
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Invite button click listener
        findViewById(R.id.invite_button).setOnClickListener(this);
        // [END_EXCLUDE]

        // Create an auto-managed GoogleApiClient with acccess to App Invites.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, this)
                .build();

        // Initialize Firebase Analytics
        mMeasurement = AppMeasurement.getInstance(this);

        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        boolean autoLaunchDeepLink = true;
        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this, autoLaunchDeepLink)
                .setResultCallback(
                        new ResultCallback<AppInviteInvitationResult>() {
                            @Override
                            public void onResult(AppInviteInvitationResult result) {
                                Log.d(TAG, "getInvitation:onResult:" + result.getStatus());

                                // Extract information from the intent
                                Intent intent = result.getInvitationIntent();
                                String deepLink = AppInviteReferral.getDeepLink(intent);
                                String invitationId = AppInviteReferral.getInvitationId(intent);

                                // Log an event to Analytics
                                Bundle bundle = new Bundle();
                                bundle.putString("app_invitation_id", invitationId);
                                bundle.putString(AppMeasurement.Param.ITEM_ID, deepLink);
                                bundle.putString(AppMeasurement.Param.ITEM_NAME, "Standard Offer");
                                bundle.putString(AppMeasurement.Param.ITEM_CATEGORY, "Offer");
                                mMeasurement.logEvent(AppMeasurement.Event.VIEW_ITEM, bundle);

                                // Because autoLaunchDeepLink = true we don't have to do anything
                                // here, but we could set that to false and manually choose
                                // an Activity to launch to handle the deep link here.
                                // ...
                            }
                        });
    }
    // [END on_create]

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        showMessage(getString(R.string.google_play_services_error));
    }

    /**
     * User has clicked the 'Invite' button, launch the invitation UI with the proper
     * title, message, and deep link
     */
    // [START on_invite_clicked]
    private void onInviteClicked() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }
    // [END on_invite_clicked]

    // [START on_activity_result]
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);

                // Log an event to Firebase Analytics for each sent invite, using the invitation
                // id as a cutom parameter.
                for (String id : ids) {
                    Bundle bundle = new Bundle();
                    bundle.putString("app_invitation_id", id);
                    bundle.putString(AppMeasurement.Param.ITEM_ID, getString(R.string.invitation_deep_link));
                    bundle.putString(AppMeasurement.Param.CONTENT_TYPE, "invitation");
                    mMeasurement.logEvent(AppMeasurement.Event.SHARE, bundle);
                }
            } else {
                // Sending failed or it was canceled, show failure message to the user
                // [START_EXCLUDE]
                showMessage(getString(R.string.send_failed));
                // [END_EXCLUDE]
            }
        }
    }
    // [END on_activity_result]

    private void showMessage(String msg) {
        ViewGroup container = (ViewGroup) findViewById(R.id.snackbar_layout);
        Snackbar.make(container, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.invite_button:
                onInviteClicked();
                break;
        }
    }
}


/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.telenav.amazon.s3;

import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.telenav.amazon.s3.tvmclient.AmazonSharedPreferencesWrapper;
import com.telenav.amazon.s3.tvmclient.AmazonTVMClient;
import com.telenav.amazon.s3.tvmclient.Response;
import com.telenav.amazon.s3.tvmclient.SharedPreferences;

/**
 * This class is used to get clients to the various AWS services. Before accessing a client the credentials should be checked to ensure validity.
 */
public class AmazonClientManager {

    private static final String LOG_TAG = "AmazonClientManager";

    private AmazonS3Client s3Client = null;
    private SharedPreferences sharedPreferences = null;
    private String tvmUrl = null;
    private boolean useSSL = false;

    public AmazonClientManager(SharedPreferences settings, String tvmUrl, boolean useSSL) {
        this.sharedPreferences = settings;
        this.tvmUrl = tvmUrl;
        this.useSSL = useSSL;
    }

    public AmazonS3Client s3() {
        
        validateCredentials();
        return s3Client;
    }

    public Response validateCredentials() {

        Response ableToGetToken = Response.SUCCESSFUL;

        if (AmazonSharedPreferencesWrapper.areCredentialsExpired(this.sharedPreferences)) {

            synchronized (this) {

                if (AmazonSharedPreferencesWrapper.areCredentialsExpired(this.sharedPreferences)) {

                    Log.d(LOG_TAG, "Credentials were expired.");

                    AmazonTVMClient tvm = new AmazonTVMClient(this.sharedPreferences, tvmUrl, useSSL);

                    ableToGetToken = tvm.anonymousRegister();

                    if (ableToGetToken.requestWasSuccessful()) {

                        ableToGetToken = tvm.getToken();

                        if (ableToGetToken.requestWasSuccessful()) {
                            Log.d(LOG_TAG, "Creating New Credentials.");
                            initClients();
                        }
                    }
                }
            }

        } else if (s3Client == null) {

            synchronized (this) {

                if (s3Client == null) {

                    Log.d(LOG_TAG, " Creating New Credentials.");
                    initClients();
                }
            }
        }

        return ableToGetToken;
    }

    private void initClients() {
        AWSCredentials credentials = AmazonSharedPreferencesWrapper.getCredentialsFromSharedPreferences(this.sharedPreferences);

        Region region = Region.getRegion(Regions.US_WEST_2);

        s3Client = new AmazonS3Client(credentials);
        s3Client.setRegion(region);
    }

    public void clearCredentials() {

        synchronized (this) {
            AmazonSharedPreferencesWrapper.wipe(this.sharedPreferences);
            s3Client = null;
        }
    }

    public boolean wipeCredentialsOnAuthError(AmazonServiceException ex) {
        // For S3
        // http://docs.amazonwebservices.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList
        if (ex.getErrorCode().equals("AccessDenied") || ex.getErrorCode().equals("BadDigest") || ex.getErrorCode().equals("CredentialsNotSupported") || ex.getErrorCode().equals("ExpiredToken") || ex.getErrorCode().equals("InternalError") || ex.getErrorCode().equals("InvalidAccessKeyId")
                || ex.getErrorCode().equals("InvalidPolicyDocument") || ex.getErrorCode().equals("InvalidToken") || ex.getErrorCode().equals("NotSignedUp") || ex.getErrorCode().equals("RequestTimeTooSkewed") || ex.getErrorCode().equals("SignatureDoesNotMatch")
                || ex.getErrorCode().equals("TokenRefreshRequired")) {
            clearCredentials();
            return true;
        }

        return false;
    }
}

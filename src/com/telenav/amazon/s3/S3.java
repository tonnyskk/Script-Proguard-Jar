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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.telenav.amazon.s3.tvmclient.SharedPreferences;

public class S3 {

    private static boolean isPublicRead = false;
    private static AmazonClientManager clientManager = null;
    private String tvmURI;
    private boolean useSSL = false;
    private static final String TAG = "S3";
    public S3(String tvmUrl, boolean useSSL) {
        this.tvmURI = tvmUrl;
        this.useSSL = useSSL;
    }
    
    public void setPublicRead(boolean isPublicRead){
        this.isPublicRead = isPublicRead;
    }

    private AmazonClientManager getClientManager() {
        if(clientManager == null) {
            synchronized (S3.class) {
                clientManager = new AmazonClientManager(SharedPreferences.getInstance(), tvmURI, useSSL);
            }
        }
        return clientManager;
    }

    private AmazonS3Client getInstance() {
        return getClientManager().s3();
    }

    public boolean uploadFile(String bucketName, String s3Path, File localFile) {
        if (bucketName == null || s3Path == null || localFile == null || !localFile.isFile() || !localFile.exists()) {
            return false;
        }
        try{
            if (!isBucketExists(bucketName)) {
                createBucket(bucketName);
            } else {
                // delete existed file
                deleteObject(bucketName, s3Path);
            }

            uploadFileMultiThread(bucketName, s3Path, localFile);
            return true;
        }catch(Exception ex){
            Log.w(TAG, ex);
        }
        return false;
    }

    private void uploadFileMultiThread(String bucketName, String s3Path, File localFile) throws AmazonClientException{
        try {
            TransferManager tm = new TransferManager(getInstance());
            TransferManagerConfiguration conf = tm.getConfiguration();

            int threshold = 1 * 1024 * 1024;
            conf.setMultipartUploadThreshold(threshold);
            tm.setConfiguration(conf);

            Upload upload = tm.upload(bucketName, s3Path, localFile);
            TransferProgress p = upload.getProgress();

            while (upload.isDone() == false) {
                int percent = (int) (p.getPercentTransferred());

                Log.d(TAG, "\r" + localFile + " - " + "[ " + percent + "% ] " + p.getBytesTransferred() + " / " + p.getTotalBytesToTransfer());

                // Do work while we wait for our upload to complete...
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            Log.d(TAG, "\r" + localFile + " - " + "[ 100% ] " + p.getBytesTransferred() + " / " + p.getTotalBytesToTransfer());

            // Add public access level for file
            if (isPublicRead) {
                getInstance().setObjectAcl(bucketName, s3Path, CannedAccessControlList.PublicRead);
            }
        } catch (AmazonServiceException ex) {
            getClientManager().wipeCredentialsOnAuthError(ex);
            throw ex;
        }
    }

    private boolean isBucketExists(String bucketName) {
        List<String> bucketList = getBucketNames();
        if (bucketList != null && bucketList.contains(bucketName)) {
            return true;
        }
        return false;
    }

    private List<String> getBucketNames() throws AmazonServiceException{

        try {
            List<Bucket> buckets = getInstance().listBuckets();

            List<String> bucketNames = new ArrayList<String>(buckets.size());
            Iterator<Bucket> bIter = buckets.iterator();
            while (bIter.hasNext()) {
                Bucket bucket = bIter.next();
                bucketNames.add((bucket.getName()));
            }
            return bucketNames;

        } catch (AmazonServiceException ex) {
            getClientManager().wipeCredentialsOnAuthError(ex);
            throw ex;
        }
    }

    private void createBucket(String bucketName) throws AmazonServiceException {
        try {
            getInstance().createBucket(bucketName);
        } catch (AmazonServiceException ex) {
            getClientManager().wipeCredentialsOnAuthError(ex);
            throw ex;
        }
    }

    private void deleteObject(String bucketName, String objectName) throws AmazonServiceException {

        try {
            getInstance().deleteObject(bucketName, objectName);
        } catch (AmazonServiceException ex) {
            getClientManager().wipeCredentialsOnAuthError(ex);
            throw ex;
        }
    }

}

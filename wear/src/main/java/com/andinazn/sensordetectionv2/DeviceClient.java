/*
** Created by Andina Zahra Nabilla on 10 April 2018
*
* Activity berfungsi untuk:
* 1. Membuat GoogleApiClient instance untuk terhubung pada service Wearable Api
* 2. Menghubungkan client dengan Google Play Services
* 3. Pemetaan Map Data Untul Sinkronisasi Antara Wear dan Handheld
* 4. Flag data urgency
* 5. Pengiriman Fall State
* 6. Pengiriman Data Sensor & Activity Recognition
 */

package com.andinazn.sensordetectionv2;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseLongArray;

import com.github.pocmo.sensordashboard.shared.DataMapKeys;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeviceClient {
    private static final String TAG = "SensorDashboard/DeviceClient";
    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;

    private static final String FALLSTATE = "com.andinazn.sensordetectionv2.fallstate";

    public static DeviceClient instance;

    public static DeviceClient getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceClient(context.getApplicationContext());
        }

        return instance;
    }

    private Context context;
    private GoogleApiClient googleApiClient;
    private ExecutorService executorService;
    private int filterId;

    private SparseLongArray lastSensorData;

    private DeviceClient(Context context) {
        this.context = context;

        //1. Membuat GoogleApiClient instance untuk terhubung pada service wearable
        googleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks(){
            @Override
            public void onConnected(Bundle connectionHint) {
                Log.d(TAG, "onConnected: " + connectionHint);
                // Now you can use the Data Layer API
            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API).build();

        googleApiClient.connect();

        executorService = Executors.newCachedThreadPool();
        lastSensorData = new SparseLongArray();
    }

    public void setSensorFilter(int filterId) {
        Log.d(TAG, "Now filtering by sensor: " + filterId);

        this.filterId = filterId;
    }

    //6. Pengiriman Data Sensor & Activity Recognition
    public void sendSensorData(final int sensorType, final int accuracy, final long timestamp, final float[] values) {
        long t = System.currentTimeMillis();

        long lastTimestamp = lastSensorData.get(sensorType);
        long timeAgo = t - lastTimestamp;

        if (lastTimestamp != 0) {
            if (filterId == sensorType && timeAgo < 100) {
                return;
            }

            if (filterId != sensorType && timeAgo < 3000) {
                return;
            }
        }

        lastSensorData.put(sensorType, t);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendSensorDataInBackground(sensorType, accuracy, timestamp, values);
            }
        });
    }

    //5. Pengiriman Fall State
    public void sendFallStatus(boolean fallstate) {

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/fall");
        putDataMapRequest.getDataMap().putBoolean(FALLSTATE, fallstate);

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, putDataRequest)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send count data item ");
                        } else {
                            Log.d(TAG, "Successfully sent count data item");
                        }
                    }
                });

        send(putDataRequest);
    }

    private void sendSensorDataInBackground(int sensorType, int accuracy, long timestamp, float[] values) {
        if (sensorType == filterId) {
            Log.i(TAG, "Sensor " + sensorType + " = " + Arrays.toString(values));
        } else {
            Log.d(TAG, "Sensor " + sensorType + " = " + Arrays.toString(values));
        }

        //3. Pemetaan Map Data Untul Sinkronisasi Antara Wear dan Handheld
        PutDataMapRequest dataMap = PutDataMapRequest.create("/sensors/" + sensorType);

        dataMap.getDataMap().putInt(DataMapKeys.ACCURACY, accuracy);
        dataMap.getDataMap().putLong(DataMapKeys.TIMESTAMP, timestamp);
        dataMap.getDataMap().putFloatArray(DataMapKeys.VALUES, values);

        //4. Flag data urgency
        PutDataRequest putDataRequest = dataMap.asPutDataRequest().setUrgent();
        send(putDataRequest);
    }

    private boolean validateConnection() {

        //2. Menghubungkan client dengan Google Play Services

        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
            return true;
        }
        ConnectionResult result = googleApiClient
                .blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        return result.isSuccess();

    }

    private void send(PutDataRequest putDataRequest) {
        if (validateConnection()) {
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.v(TAG, "Sending sensor data: " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
    }
}

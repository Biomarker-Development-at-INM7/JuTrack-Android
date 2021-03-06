/**
 Copyright © 2020 JuTrack Mobile Framework, JuTrack Platform, JuTrack Social, JuTrack Move, JuTrack EMA

 Licensed under the Apache License, Version 2.0 (the “License”);
 you may not use this file except in compliance with the License.
 you may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an “AS IS” BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and limitations under the License.

 **/
package inm7.JTrack.Jtrack_Social.AccelerationSensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import inm7.JTrack.Jtrack_Social.Constants;
import inm7.JTrack.Jtrack_Social.Serverinterface;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncAccelerationClass {

    private Context mContext;
    private int mtype;

    public SyncAccelerationClass(Context mContext, int mtype) {
        this.mContext = mContext;
        this.mtype = mtype;
    }

    private static final String TAG = SyncAccelerationJobService.class.getName() ;

    OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();

//    //Gson Builder
//    GsonBuilder gsonBuilder = new GsonBuilder();
//    Gson gson = gsonBuilder.create();



    /**
     * injection of interceptors to handle encryption and decryption
     */

//    //Encryption Interceptor
//    GzipInterceptor gzipInterceptor = new GzipInterceptor();
//
//    // OkHttpClient. Be conscious with the order
//    OkHttpClient okHttpClient = new OkHttpClient()
//            .newBuilder()
//            //httpLogging interceptor for logging network requests
//            .addInterceptor(gzipInterceptor)
//            .build();


//    // added after room integration
//    Retrofit retrofit = new Retrofit.Builder()
//            .client(okHttpClient)
//            .baseUrl(Constants.serveraddress)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build();
//    Serverinterface serverinterface = retrofit.create(Serverinterface.class);

    // added after room integration
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(Constants.serveraddress)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    Serverinterface serverinterface = retrofit.create(Serverinterface.class);



//    public SharedPreferences lastsyncvalue= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//    public SharedPreferences.Editor lastsyncvalueeditor=lastsyncvalue.edit();

//    private int patchsize = Constants.patchSizeSmall;
    private int patchsize = Constants.patchSize;

    public static boolean jobCancelled = false;


    public void syncOnBackGround() {
        SharedPreferences lastSyncTime= PreferenceManager.getDefaultSharedPreferences(mContext);


        if (jobCancelled) {
            return;
        }
        //do it here------------------------
        new Thread(new Runnable() {
            @Override
            public void run() {
             /*
             Instead of sending only one path we will send multiple
              */
                Integer lastsyncvalue= GetAccelerationSensorsDataAsyc.getIdtoSync(AccelerationSenorsAppDatabase.getAppDatabase(mContext));

                int sensorCount = GetAccelerationSensorsDataAsyc.getSensorsCount(AccelerationSenorsAppDatabase.getAppDatabase(mContext));
                // check if loop size is bigger than constant Max value fix it to Max value
                int loopCount = ( (sensorCount )/ patchsize > Constants.loopMaxSizePatchSize)  ?  Constants.loopMaxSizePatchSize: (sensorCount) / patchsize ;

                // check if we have data to send or not
                /*
                if (lastsyncvalue.getInt(Constants.locationSyncid, 1) + patchsize >= sensorCount) {
                    jobFinished(params, true);

                    return;
                }

                 */

                Long sync_Time_diff =  System.currentTimeMillis() -  lastSyncTime.getLong(Constants.AccelerationLastSync, System.currentTimeMillis());

                if ( (mtype==1) && (sync_Time_diff < Constants.One_Day) ) {
                    while (!jobCancelled && loopCount > 0) {
                        readSensorsPost(1, lastsyncvalue, lastsyncvalue + patchsize);

                        lastsyncvalue = GetAccelerationSensorsDataAsyc.getIdtoSync(AccelerationSenorsAppDatabase.getAppDatabase(mContext));
                        loopCount--;
                    }
                }
               else if  (  (mtype==0) || (sync_Time_diff > Constants.One_Day)  ){
                    while (!jobCancelled &&  sensorCount>0 ) {
                        readSensorsPost(1, lastsyncvalue, lastsyncvalue + patchsize);

                        lastsyncvalue= GetAccelerationSensorsDataAsyc.getIdtoSync(AccelerationSenorsAppDatabase.getAppDatabase(mContext));
                        sensorCount = GetAccelerationSensorsDataAsyc.getSensorsCount(AccelerationSenorsAppDatabase.getAppDatabase(mContext));

                    }

                }


                // when our job finished should send it, otherwise it will shows a notification on battery usage
                // true if need to re schedule
//                jobFinished(params, true);

            }
        }).start();


    }



    // functions related to job


    // added after room integration
    private void creatSensorsPost(final List<AccelerationSensor> sensor, Integer startindex, Integer endindex) {
        String md5_value= md5(sensor).toString();

        SharedPreferences lastSyncTime=PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor lastSyncTimeEeditor=lastSyncTime.edit();


        Call<AccelerationSensor> call = serverinterface.createAccelerationSensor("UTF-8",Constants.write_data,md5_value,sensor);
        // synchronous
        try {
            Response<AccelerationSensor> response = call.execute();

            if (!response.isSuccessful()) {
                Log.d(TAG, "onResponse: " + response.code());
                jobCancelled = true;
                return;
            }

            if (response.code() == 200 ) {
                AccelerationSensor post = response.body();
/*
                String content = "";
                content += "ID: " + post.get(post.size() - 1).getId() + "\n";
                content += "sensor name: " + post.get(post.size() - 1).getSensorname() + "\n";
                content += "sensor timestamp: " + post.get(post.size() - 1).getTimestamp() + "\n";
                content += "device id: " + post.get(post.size() - 1).getDeviceid() + "\n";
                content += "username: " + post.get(post.size() - 1).getSensorname() + "\n";
                content += "Alt: " + post.get(post.size() - 1).getAlt() + "\n";
                content += "Lat: " + post.get(post.size() - 1).getLat() + "\n";
                content += "Long: " + post.get(post.size() - 1).getLon() + "\n";
                content += "Provider: " + post.get(post.size() - 1).getAccuracy() + "\n";

*/

                deleteSensorsPosr(1,startindex,endindex);

                lastSyncTimeEeditor.putLong(Constants.AccelerationLastSync, (System.currentTimeMillis()) );
                lastSyncTimeEeditor.apply();

                Log.d(TAG, "onResponse: " + response.code() );

                jobCancelled = false;

            } else {
                jobCancelled = true;
                return;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

/* // Asynchronous if selected may need to handle, otherwise it goes crazy and during run and normal during debugging, NO explanation!!!
       call.enqueue(new Callback<List<AccelerationSensor>>() {

        Response<List<AccelerationSensor>> response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        @Override
            public void onResponse(Call<List<AccelerationSensor>> call, Response<List<AccelerationSensor>> response) {

                if (!response.isSuccessful()) {
                    Log.d(TAG, "onResponse: " + response.code());
                    jobCancelled = true;
                    return;
                }

                if (response.code() == 200 && !response.body().isEmpty()) {
                    List<AccelerationSensor> post = response.body();

                    String content = "";
                    content += "ID: " + post.get(post.size() - 1).getId() + "\n";
                    content += "sensor name: " + post.get(post.size() - 1).getSensorname() + "\n";
                    content += "sensor timestamp: " + post.get(post.size() - 1).getTimestamp() + "\n";
                    content += "x: " + post.get(post.size() - 1).getX() + "\n";
                    content += "y: " + post.get(post.size() - 1).getY() + "\n";
                    content += "z: " + post.get(post.size() - 1).getZ() + "\n";

                    lastsyncvalueeditor.putInt(Constants.synchid, lastsyncvalue.getInt(Constants.synchid, 1) + sensor.size());
                    lastsyncvalueeditor.apply();

                    Log.d(TAG, "onResponse: " + content );

                    jobCancelled = false;

                } else {
                    jobCancelled = true;
                    return;

                }
            }


            @Override
            public void onFailure(Call<List<AccelerationSensor>> call, Throwable t) {
                // errortxt.setText(t.getMessage());
                Log.d(TAG, "onFailure: " + t.getMessage());
                jobCancelled = true;

            }
        });
    */

    }

    // added after room integration
    private void readSensorsPost(int type, Integer startindex, Integer endindex) {

        if (type == 0) {
            // creatSensorsPost(GetLocationSensorsDataAsyc.getSensors(LocationSenorsAppDatabase.getAppDatabase(this)));

        } else if (type == 1) {

            creatSensorsPost(GetAccelerationSensorsDataAsyc.syncSensorwithServer(AccelerationSenorsAppDatabase.getAppDatabase(mContext), startindex, endindex),startindex, endindex);


        }

    }


    private void deleteSensorsPosr(int type, Integer startindex, Integer endindex){

        if (type == 0) {

        } else if (type == 1) {

            GetAccelerationSensorsDataAsyc.DeleteSensorFromLocal(AccelerationSenorsAppDatabase.getAppDatabase(mContext), startindex, endindex);

        }
    }

// MD5 function

    private static String md5(List<AccelerationSensor> sensor) {

        // GsonBuilder gsonBuilder= new GsonBuilder();
        //  Gson gson = gsonBuilder.create();
        // String JsonObject=gson.toJson(sensor);

        Gson gson= new Gson();
        byte[] JsonObject = new byte[0];
        try {
            JsonObject = gson.toJson(sensor).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        MessageDigest m = null;

        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] thedigest = m.digest(JsonObject);
        String hash = String.format("%032x", new BigInteger(1, thedigest));
        return hash;


    }

// to compress the body of request which is injected as interceptor.
    public class GzipInterceptor implements Interceptor {
        @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // do not set content encoding in negative use case

            if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
                return chain.proceed(originalRequest);
            }

            Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override public MediaType contentType() {
                    return body.contentType();
                }

                @Override public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }



}

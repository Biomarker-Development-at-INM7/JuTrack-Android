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
package inm7.JTrack.Jtrack_Social.AppUsageStatSensor;

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
import inm7.JTrack.Jtrack_Social.MyGlobalClass;
import inm7.JTrack.Jtrack_Social.Serverinterface;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncAppUsageStatClass {

    private Context mContext;
    private int mtype;
    public SyncAppUsageStatClass(Context context, int type) {
        mContext = context;
        mtype= type;
    }

    private static final String TAG = SyncAppUsageStatJobService.class.getName();


    // added after room integration
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(Constants.serveraddress)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    Serverinterface serverinterface = retrofit.create(Serverinterface.class);

//



    public static boolean jobCancelled = false;
    private int patchsize = Constants.patchSizeSmall;


    public void syncOnBackGround() {
        SharedPreferences lastSyncTime=PreferenceManager.getDefaultSharedPreferences(mContext);

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
                Integer lastsyncvalue=GetAppUsageStatSensorsDataAsyc.getIdtoSync(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext));

                int sensorCount = GetAppUsageStatSensorsDataAsyc.getSensorsCount(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext));
                // check if loop size is bigger than constant Max value fix it to Max value
                int loopCount = ( (sensorCount)/ patchsize > Constants.loopMaxSizePatchSize)  ?  Constants.loopMaxSizePatchSize: (sensorCount) / patchsize ;

                // check if we have data to send or not
               /*
                if (lastsyncvalue.getInt(Constants.appUsageStatSyncid, 1) + patchsize >= sensorCount) {
                    jobFinished(params, true);

                    return;
                }

                */



               Long sync_Time_diff =  System.currentTimeMillis() -  lastSyncTime.getLong(Constants.appUsageStatLastSync, System.currentTimeMillis());




               if ( (mtype==1) && (sync_Time_diff < Constants.One_Day) ) {
                   while (!jobCancelled &&  loopCount>0 ) {
                       readSensorsPost(1, lastsyncvalue, lastsyncvalue + patchsize);

                       lastsyncvalue=GetAppUsageStatSensorsDataAsyc.getIdtoSync(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext));
                       loopCount--;
                   }


               }
               else if  (  (mtype==0) || (sync_Time_diff > Constants.One_Day)  ){
                   while (!jobCancelled &&  sensorCount>0 ) {
                       readSensorsPost(1, lastsyncvalue, lastsyncvalue + patchsize);

                       lastsyncvalue=GetAppUsageStatSensorsDataAsyc.getIdtoSync(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext));
                       sensorCount = GetAppUsageStatSensorsDataAsyc.getSensorsCount(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext));

                   }

               }


                // when our job finished should send it, otherwise it will shows a notification on battery usage
                // true if need to re schedule

            }
        }).start();


    }


    // functions related to job


    // added after room integration
    private void creatSensorsPost(final List<AppUsageStatSensor> sensor, Integer startindex, Integer endindex) {
        String md5_value= md5(sensor).toString();
        SharedPreferences lastSyncTime=PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor lastSyncTimeEeditor=lastSyncTime.edit();

        Call<AppUsageStatSensor> call = serverinterface.createAppUsageStatSensor("UTF-8",Constants.write_data,md5_value,sensor);
        // synchronous
        try {
            Response<AppUsageStatSensor> response = call.execute();

            if (!response.isSuccessful()) {
                Log.d(TAG, "onResponse: " + response.code());
                jobCancelled = true;
                return;
            }

            if (response.code() == 200 ) {
                AppUsageStatSensor post = response.body();
/*
                String content = "";
                content += "ID: " + post.get(post.size() - 1).getId() + "\n";
                content += "sensor name: " + post.get(post.size() - 1).getSensorname() + "\n";
                content += "sensor timestamp: " + post.get(post.size() - 1).getTimestamp() + "\n";
                content += "device id: " + post.get(post.size() - 1).getDeviceid() + "\n";
                content += "username: " + post.get(post.size() - 1).getSensorname() + "\n";
                content += "Begin timestamp: " + post.get(post.size() - 1).getBeginTimeStamp() + "\n";
                content += "End timestamp: " + post.get(post.size() - 1).getEndTimeStamp() + "\n";
                content += "Last time used: " + post.get(post.size() - 1).getLastTimeUsed() + "\n";
                content += "Total time in foreground: " + post.get(post.size() - 1).getTotalTimeinForeground() + "\n";

*/
                deleteSensorsPosr(1,startindex,endindex);

// put last sync  time

                lastSyncTimeEeditor.putLong(Constants.appUsageStatLastSync, (System.currentTimeMillis()) );
                lastSyncTimeEeditor.apply();

                Log.d(TAG, "onResponse: " + response.code() );

                jobCancelled = false;

            }
            else if (response.code() == 403)
            {
                MyGlobalClass myGlobalClass = new MyGlobalClass(mContext);
                myGlobalClass.stop_normal();
                jobCancelled = false;

            }else {
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
            // creatSensorsPost(GetAppUsageStatSensorsDataAsyc.getSensors(AppUsageStatSenorsAppDatabase.getAppDatabase(this)));

        } else if (type == 1) {

            creatSensorsPost(GetAppUsageStatSensorsDataAsyc.syncSensorwithServer(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext), startindex, endindex),startindex,endindex);


        }

    }


    private void deleteSensorsPosr(int type, Integer startindex, Integer endindex){

        if (type == 0) {

        } else if (type == 1) {

            GetAppUsageStatSensorsDataAsyc.DeleteSensorFromLocal(AppUsageStatSenorsAppDatabase.getAppDatabase(mContext), startindex, endindex);

        }
    }

    // MD5 function

    private static String md5(List<AppUsageStatSensor> sensor) {

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

}

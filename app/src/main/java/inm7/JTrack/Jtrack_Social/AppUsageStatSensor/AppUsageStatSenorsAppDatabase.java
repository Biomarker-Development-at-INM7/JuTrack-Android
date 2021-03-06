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

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import inm7.JTrack.Jtrack_Social.Constants;

@Database(entities = {AppUsageStatSensor.class},version = 1)
public abstract class AppUsageStatSenorsAppDatabase extends RoomDatabase {
    private static AppUsageStatSenorsAppDatabase INSTANCE;

    public abstract AppUsageStatSensorsDao appUsageStatSensorsDao();

    public static AppUsageStatSenorsAppDatabase getAppDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE =
                    Room.databaseBuilder(context.getApplicationContext(), AppUsageStatSenorsAppDatabase.class, Constants.AppUsageStatSensor_td)
                            // allow queries on the main thread.
                            // Don't do this on a real app! See PersistenceBasicSample for an example.
                             .allowMainThreadQueries()
                            .build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}

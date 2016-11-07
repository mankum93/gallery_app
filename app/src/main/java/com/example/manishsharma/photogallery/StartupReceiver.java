package com.example.manishsharma.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Manish Sharma on 9/2/2016.
 */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
        Toast.makeText(context, "Brook was here", Toast.LENGTH_LONG).show();
        //context.startActivity(new Intent(context, PhotoGalleryActivity.class));
    }
}

package com.example.manishsharma.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Manish Sharma on 9/4/2016.
 */
public abstract class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null);
    }
    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mOnShowNotification);
        super.onStop();
    }
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If we receive this, we're visible, so cancel
            // the notification
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
            //Test code
            Toast.makeText(context, "I am in VisibleFragment(Activity.RESULT_CANCELED)", Toast.LENGTH_SHORT).show();
        }
    };
}

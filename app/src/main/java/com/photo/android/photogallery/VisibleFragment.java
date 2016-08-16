package com.photo.android.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by aontivero on 8/14/2016.
 */
public abstract class VisibleFragment extends Fragment{
    private static final String TAG = "VisibleFragment";

    @Override
    public void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If we received something, we are visible, so cancel the notification
            Log.i(TAG, "Cancelling Notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}

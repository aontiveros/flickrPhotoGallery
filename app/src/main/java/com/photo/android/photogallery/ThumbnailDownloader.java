package com.photo.android.photogallery;

import android.os.HandlerThread;

/**
 * Created by aontivero on 8/10/2016.
 */
public class ThumbnailDownloader <T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownlaoder";

    private boolean mHasQuit = false;
    public ThumbnailDownloader() {
        super(TAG);
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url){}
}

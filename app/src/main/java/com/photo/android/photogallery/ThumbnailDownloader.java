package com.photo.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by aontivero on 8/10/2016.
 */
public class ThumbnailDownloader <T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownlaoder";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MAX_CACHE_SIZE = 100;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloaderListener;
    private LruCache<String, Bitmap> mBitmapLruCache;

    /**
     * Interface to handle the download of a potential thumbnail
     * @param <T>
     */
    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownload(T target, Bitmap thumbnail);
    }

    public ThumbnailDownloader(Handler handler) {
        super(TAG);
        mResponseHandler = handler;
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloadListener<T> listener){
        mBitmapLruCache = new LruCache<>(MAX_CACHE_SIZE);
        mThumbnailDownloaderListener = listener;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * Queue a task to retrieve the thumbnail for the respective target
     * @param target
     * @param url
     */
    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a url: " + url);
        if(url == null){
            mRequestMap.remove(target);
        }
        else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    /**
     * When the looper is prepared, send up the handler to deal with requests
     */
    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    /**
     * Handle the respective request by downloading the image and transforming it into
     * a usable bitmap
     * @param target
     */
    private void handleRequest(final T target){
        try {
            final String url = mRequestMap.get(target);
            final Bitmap bitmap;
            if (url == null) {
                return;
            }
            else if(mBitmapLruCache.get(url) != null){
               bitmap = mBitmapLruCache.get(url);
               Log.i(TAG, "Got element from cache!");
            }
            else {
                byte[] bitmaBytes = new FlickrFetcher().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmaBytes, 0, bitmaBytes.length);
            }
            mBitmapLruCache.put(url, bitmap);
            Log.i(TAG, "Bitmap created!");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url || mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownload(target, bitmap);
                }
            });
        }
        catch(IOException ioe){
            Log.e(TAG, "Error downloading image!", ioe);
        }
    }

    /**
     * Clear the queue of any remaining tasks
     */
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
}

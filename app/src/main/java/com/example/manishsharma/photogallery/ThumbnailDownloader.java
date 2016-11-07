package com.example.manishsharma.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import android.os.Handler;

/**
 * Created by Manish Sharma on 8/20/2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_DOWNLOAD_PRELOADING = 1;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    //CHALLENGE: CACHING OF THUMBNAILS - LRU CACHE DECLARATION
    int mCacheSize = 4 * 1024 * 1024;
    LruCache<String, Bitmap> mBitmapCache = new LruCache<String, Bitmap>(mCacheSize);

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
                else if(msg.what == MESSAGE_DOWNLOAD_PRELOADING){
                    String url = (String) msg.obj;
                    Log.i(TAG, "Got a request for Preloading " + url);
                    cacheThumbnail(url);
                }
            }
        };
    }

    public void queueThumbnail(final T target, final String url) {
        Log.i("queueThumbnail", "Got a URL: " + url);
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            if(mBitmapCache.get(url) == null){
                mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                        .sendToTarget();
            }
            else{
                final Bitmap bitmap = mBitmapCache.get(url);
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        if (mRequestMap.get(target) != url) {
                            return;
                        }
                        mRequestMap.remove(target);
                        mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                    }
                });
            }

        }
    }
    public void queueThumbnail(final String url) {
        Log.i("queueThumbnail", "Got a URL for Preloading " + url);
        mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD_PRELOADING, url)
                .sendToTarget();
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD_PRELOADING);
    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }
            final Bitmap bitmap;
            //CHALLENGE: CACHING OF THUMBNAILS - CONDITION
            if (mBitmapCache.get(url) == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mBitmapCache.put(url, bitmap);
                Log.i(TAG, "Bitmap created");

            } else {
                //CHALLENGE: CACHING OF THUMBNAILS - RETRIEVING FROM CACHE
                bitmap = mBitmapCache.get(url);
                Log.i(TAG, "Bitmap already exists in cache");
            }
            //CHALLENGE: CACHING OF THUMBNAILS - ADDING TO THE CACHE
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (mRequestMap.get(target) != url) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void cacheThumbnail(String url) {

        if (url == null) {
            return;
        }
        if (mBitmapCache.get(url) == null){
            try {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mBitmapCache.put(url, bitmap);
                Log.i(TAG, "Preloading");

            } catch (IOException ioe) {
                Log.e(TAG, "Error downloading image", ioe);
            }
        }
    }
}
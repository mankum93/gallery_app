package com.example.manishsharma.photogallery;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.IOException;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }
}

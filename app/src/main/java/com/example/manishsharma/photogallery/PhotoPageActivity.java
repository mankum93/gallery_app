package com.example.manishsharma.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by Manish Sharma on 9/4/2016.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }
    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }
    @Override
    public void onBackPressed(){
        PhotoPageFragment ppf = (PhotoPageFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(ppf.informOnBackPressed()){
            return;
        }
        super.onBackPressed();
    }
}

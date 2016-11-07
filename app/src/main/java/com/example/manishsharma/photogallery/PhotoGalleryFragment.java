package com.example.manishsharma.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by Manish Sharma on 8/15/2016.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";
    private int mSpans = 3;
    private int mColumnWidth = 120;

    private int mPosition=1;

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private HashMap<Integer, Boolean> mPositionDownloadRequestList = new HashMap<>();

    private int mPageNo = 1;
    private GridLayoutManager mlayoutManager;

    private PhotoAdapter mAdapter;

    private int mNoOfRequestsConsideredEnough = 10;
    private boolean mShouldFlagEnoughRequestsAtATime = false;
    private int mCurrentNoOfRequestsQueued = 0;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                        mCurrentNoOfRequestsQueued--;
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v
                .findViewById(R.id.fragment_photo_gallery_recycler_view);
        mlayoutManager = new GridLayoutManager(getActivity(), mSpans);
        mPhotoRecyclerView.setLayoutManager(mlayoutManager);
        //CHALLENGE:PAGING - Setting the scroll listener
        mPhotoRecyclerView.setOnScrollListener(new PhotoScrollListener());
        //CHALLENGE:DYNAMICALLY CHANGING COLUMNS - Setting the Layout listener
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new DynamicNoOfColumnsListener());
        setupAdapter();

        return v;
    }
    @Override
    public void onDestroy() {
        mThumbnailDownloader.quit();
        super.onDestroy();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                hideSoftKeypad();
                updateItems();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    private void hideSoftKeypad(){
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPhotoRecyclerView.getWindowToken(), 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                //Added by me
                //QueryPreferences.setAlarmOn(getActivity(), shouldStartAlarm);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setupAdapter() {
        if (isAdded()) {
            mAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mAdapter);
        }
    }
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {

        private String mQuery;
        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            //String query = "robot"; // Just for testing
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(mPageNo);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, mPageNo);
            }
        }
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            //CHALLENGE: PROGRESS BAR - Remove the progress bar here
            setupAdapter();
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{
        ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }
        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity
                    .newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            Log.i("PhotoHolder: ", "onCreateViewHolder(ViewGroup viewGroup, int viewType)");
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            Log.i("Enter onBindViewHolder:", "onBindViewHolder(PhotoHolder photoHolder, int position)");
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            //Log.i("1 method call:", "onBindViewHolder(PhotoHolder photoHolder, int position)");
            Drawable placeholder = getResources().getDrawable(R.drawable.dante_dmc_623_350);
            photoHolder.bindDrawable(placeholder);

            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            mCurrentNoOfRequestsQueued++;
            mPositionDownloadRequestList.put(position, true);

            //CHALLENGE: PRELOADING THE CACHE
            //AND, If enough requests queued, hold it!!!
            int pos;
            for(pos = mPosition+1; pos <= mPosition+5; pos++){
                if(pos >= mGalleryItems.size()-1){
                    break;
                }
                if(mCurrentNoOfRequestsQueued >= mNoOfRequestsConsideredEnough){
                    mShouldFlagEnoughRequestsAtATime = true;
                }
                if(mPositionDownloadRequestList.containsKey(pos) == false){
                    GalleryItem toBeCachedGalleryItem = mGalleryItems.get(pos);
                    if(mShouldFlagEnoughRequestsAtATime == false){
                        mThumbnailDownloader.queueThumbnail(toBeCachedGalleryItem.getUrl());
                        mCurrentNoOfRequestsQueued++;
                        mPositionDownloadRequestList.put(pos, true);
                    }
                    else{
                        Log.i("PhotoGalleryFragment: ","Enough Requests Queued");
                        //Enough requests queued, let the thumbnail downloader handle a few
                        break;
                    }

                }
                else if(mPositionDownloadRequestList.get(pos) == true){
                    //Already exists. Do nothing.
                }
            }
            mPosition = pos-1;

            Log.i("Exit onBindViewHolder:", "onBindViewHolder(PhotoHolder photoHolder, int position)");
        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    //CHALLENGE:PAGING - Implementation of the scroller
    private class PhotoScrollListener extends RecyclerView.OnScrollListener{

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy){

            View view = (View) recyclerView.getChildAt(mlayoutManager.getChildCount()-1);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();

            //Log.d("view.getBottom()", view.getBottom() + "");
            //Log.d("recycler.getHeight()", recyclerView.getHeight() + "");
            //Log.d("recycler.getScrollY()", recyclerView.getScrollY() + "");
            //Log.d("diff", (view.getBottom()-(recyclerView.getHeight()+recyclerView.getScrollY())) + "");
            if( (lp.getViewLayoutPosition() == (mlayoutManager.getItemCount()-1)) && (view.getBottom()==(recyclerView.getHeight())))
            {
                // notify that we have reached the bottom
                Log.d("PhotoScrollListener", "MyScrollView: Bottom has been reached" );
                //Reload the model object; Increment the page that needs to be retrieved
                mPageNo++;

                /*As per my understanding, starting a task and then starting another one at the scroll end must mean following:
                 1) The task shall fetch the "data"
                 2) Then only, scrolling shall be possible. These 2 event are mutually exclusive
                 3) The task will have already been finished by then
                 4) Next task will not intersect with the current task
                    Conclusion: We are safe with the currently written code.
                */
                mThumbnailDownloader.clearQueue();
                mPositionDownloadRequestList.clear();

                updateItems();
                mAdapter.notifyDataSetChanged();
            }
        }
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState){
            super.onScrollStateChanged(recyclerView, newState);
        }
    }

    //CHALLENGE:DYNAMICALLY CHANGING COLUMNS - Implementation of the Layout listener
    private class DynamicNoOfColumnsListener implements ViewTreeObserver.OnGlobalLayoutListener{

        @Override
        public void onGlobalLayout(){
            mPhotoRecyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            //getWiwth() returns the width in pixels
            Log.d("getWidth()", mPhotoRecyclerView.getWidth() + "");
            mSpans = PhotoGalleryFragment.this.pxToDp(mPhotoRecyclerView.getWidth())/mColumnWidth;
            mlayoutManager.setSpanCount(mSpans);
            mPhotoRecyclerView.requestLayout();
        }
    }
    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }
}

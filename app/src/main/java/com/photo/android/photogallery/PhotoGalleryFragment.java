package com.photo.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aontivero on 8/8/2016.
 */
public class PhotoGalleryFragment  extends Fragment{

    //Static constants
    private static final String TAG = "PhotoGalleryFragment";

    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    //Components
    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private FlickrFetcher mFlickrFetcher;
    private ProgressBar mProgressBar;
    private SearchView mSearchView;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
       // setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownload(PhotoHolder target, Bitmap thumbnail) {
                Drawable imageDraw = new BitmapDrawable(getResources(), thumbnail);
                target.bindGalleryItem(imageDraw);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread has started!");
    }

    /**
     * Assign components in the view
     * @param inflater
     * @param container
     * @param savedInstance
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance){
        super.onCreateView(inflater, container, savedInstance);

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycle_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mRecyclerView.setOnScrollListener(new PhotoScroller());

        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem item = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) item.getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "Query submit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                mSearchView.clearFocus();
                mItems.clear();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, "Query change: " + newText);
                return false;
            }
        });

        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                QueryPreferences.setStoredQuery(getActivity(), "");
                mItems.clear();
                updateItems();
                return false;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                mSearchView.setQuery(query, false);
            }
        });
    }

    /**
     * Clear out the user's preferences for the stored result if they had selected clear item
     * otherwise return the default.
     * @param menuItem The item selected
     * @return true if the user slected to clear all items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

    }

    public static Fragment createFragment(){
        return new PhotoGalleryFragment();
    }

    private void updateItems(){

        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }
    /**
     * Set the adapter only if the fragment is in use. Otherwise notify the adapter if new items have been added
     * @param oldSize
     * @param newSize
     */
    private void setupAdapter(int oldSize, int newSize) {
        if(newSize - oldSize == 0){
            Toast noMore =Toast.makeText(getActivity(), "There are no more images!", Toast.LENGTH_SHORT);
            noMore.show();
        }
        else if(isAdded() && (mRecyclerView.getAdapter() == null || mRecyclerView.hasPendingAdapterUpdates())){
            Log.i(TAG, "Resetting adapter!");
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
        else if(isAdded()){
            mRecyclerView.getAdapter().notifyItemRangeChanged(oldSize, newSize);
        }
    }

    /**
     * places an image on the bitmap for the respective layout
     */
    public class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;
        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindGalleryItem(Drawable item){
            mImageView.setImageDrawable(item);
        }
    }

    /**
     * When destroying the fragment, clean the handlers and the message queue of any potential tasks
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.clearQueue();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread has stopped!");
    }

    public class PhotoScroller extends RecyclerView.OnScrollListener {

        private long height = 0;

        /**
         * When the user has scrolled to the end of the current list, expand the list farther so
         * it appears that there is an unlimited scrolling effect
         * @param recyclerView
         * @param dx
         * @param dy
         */
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if(recyclerView.getAdapter().getItemCount() > 0){
                int lastItem = ((GridLayoutManager)recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
                if(lastItem != RecyclerView.NO_POSITION && lastItem == recyclerView.getAdapter().getItemCount() - 1){
                    if(mSearchView.getQuery() != null || !mSearchView.getQuery().equals("")){
                        new FetchItemsTask(mSearchView.getQuery().toString()).execute();
                    }
                    else {
                        new FetchItemsTask(null).execute();
                    }
                }
            }
        }
    }

    /**
     * PhotoAdaper to manage all items part of the recycleview
     */
    public class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        List<GalleryItem> mItems;
        public PhotoAdapter(List<GalleryItem> items){
            mItems = items;
        }

        /**
         * Create a view holder give the parent's layout
         * @param parent
         * @param viewType
         * @return
         */
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return  new PhotoHolder(view);
        }

        /**
         * Bind an item by retrieving it from the handlers for the image to download and process
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem item = mItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.drawable.no_image_box);
            holder.bindGalleryItem(placeHolder);
            mThumbnailDownloader.queueThumbnail(holder, item.getUrl_s());
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    /**
     * A async task to handle the query for the images.
     */
    public class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{

        private String mQuery;
        public FetchItemsTask(String query){
            mQuery = query;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if(mFlickrFetcher == null)
                mFlickrFetcher = new FlickrFetcher();
            if(mQuery == null) {
                return mFlickrFetcher.fetchRecentPhotos();
            }
            else {
                return mFlickrFetcher.searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            setupAdapter(0, mItems.size());
        }
    }
}

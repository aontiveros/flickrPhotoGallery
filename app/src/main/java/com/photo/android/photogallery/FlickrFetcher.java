package com.photo.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aontivero on 8/8/2016.
 */
public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "834a13bc34d941207068a5582d3e82b4";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final int MAX_PAGE = 10;
    private int DEFAULT_BUFFER_SIZE = 1024;

    private int mPage = 1;
    private String mLastQuery;

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest")
            .buildUpon()
            .appendQueryParameter("method", "flickr.photos.getRecent")
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();


    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = conn.getInputStream();

            if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(conn.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally {
            conn.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    /**
     * Fetch items from the flickr api for the current page
     * @return
     */
    public List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();
        try{
            String jsonString = getUrlString(url);
            Log.i(TAG, "Sent URL: " + url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        }
        catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON: ", je);
        }
        catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items!", ioe);
        }
        return items;
    }

    /**
     * Parse the items from the retrieved JSON and appends any elements to the list of gallery items
     * @param galleryItemList
     * @param jsonBody
     * @throws IOException
     * @throws JSONException
     */
    private void parseItems(List<GalleryItem> galleryItemList, JSONObject jsonBody) throws IOException, JSONException{
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i = 0 ; i < photosJsonArray.length(); i++){
            JSONObject photoJsonObject = (JSONObject) photosJsonArray.get(i);
            GalleryItem item = new Gson().fromJson(photoJsonObject.toString(), GalleryItem.class);
            galleryItemList.add(item);

        }
    }

    /**
     * Builds a url with the respective method and query if it is a search based method.
     * @param method The method to append
     * @param query The query to apply, if search based method is applied.
     * @return The built url to query
     */
    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon();
        uriBuilder.appendQueryParameter("method", method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
            Log.i(TAG, "Searching with query: " + query);
        }
        resolvePage(query);
        uriBuilder.appendQueryParameter("page", Integer.toString(mPage));
        return uriBuilder.build().toString();
    }

    private void resolvePage(String query){
        if(query == null && mLastQuery == null){
            mPage++;
        }
        else if(query != null && query.equals(query) && !query.equals("")){
            mPage ++;
        }
        else {
            mPage = 1;
        }
        mLastQuery = query;
    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        if(query != null && !query.equals("")){
            String url = buildUrl(SEARCH_METHOD, query);
            return downloadGalleryItems(url);
        }
        else{
            return fetchRecentPhotos();
        }

    }
}

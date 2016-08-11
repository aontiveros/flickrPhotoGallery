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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aontivero on 8/8/2016.
 */
public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "834a13bc34d941207068a5582d3e82b4";
    private static final int MAX_PAGE = 10;
    private static int page = 1;
    private int DEFAULT_BUFFER_SIZE = 1024;

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
    public List<GalleryItem> fetchItems(){
        List<GalleryItem> items = new ArrayList<>();
        try{
            String url = Uri.parse("https://api.flickr.com/services/rest")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", confirmPageAmount())
                    .build().toString();
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
     * Confirms the page that is currently next to be queued
     * @return
     */
    private synchronized String confirmPageAmount(){
        //if(page > MAX_PAGE)
          //  page = 1;
        String value = Integer.toString(page);
        page++;
        return value;
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

//            GalleryItem item = new GalleryItem();
//            item.setId(photoJsonObject.getString("id"));
//            item.setTitle(photoJsonObject.getString("title"));
//
//            if(photoJsonObject.has("url_s")){
//                item.setUrl_s(photoJsonObject.getString("url_s"));
//                galleryItemList.add(item);
//            }
            GalleryItem item = new Gson().fromJson(photoJsonObject.toString(), GalleryItem.class);
            galleryItemList.add(item);

        }
    }
}

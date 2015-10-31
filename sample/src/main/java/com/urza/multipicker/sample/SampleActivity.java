package com.urza.multipicker.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.urza.multipicker.FolderListActivityFragmented;
import com.urza.multipicker.MediaEntityWrapper;
import com.urza.multipicker.MultiPicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by urza on 29.10.2015.
 */
public class SampleActivity extends Activity implements View.OnClickListener {
    
    final static String TAG = SampleActivity.class.getSimpleName();

    static final int ADD_PHOTO_REQUEST = 2;
    static final int ADD_VIDEO_REQUEST = 3;

    private static final String CURRENT_PHOTO_SELECTION = "currentPhotoSelection";
    private static final String CURRENT_VIDEO_SELECTION = "currentVideoSelection";
    
    private HashMap<String, List<MediaEntityWrapper>> currentPhotoSelection;
    private HashMap<String, List<MediaEntityWrapper>> currentVideoSelection;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .threadPoolSize(Runtime.getRuntime().availableProcessors())
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                //.writeDebugLogs()
                .build();
        Log.d(TAG, "ImageLoaderConfig threadPoolSize: "+Runtime.getRuntime().availableProcessors());
        ImageLoader.getInstance().init(config);

        if (currentPhotoSelection == null) {
            if (savedInstanceState != null)
                currentPhotoSelection = (HashMap) savedInstanceState.getSerializable(CURRENT_PHOTO_SELECTION);
            else
                currentPhotoSelection = new HashMap<String, List<MediaEntityWrapper>>();
        }
        if (currentVideoSelection == null) {
            if (savedInstanceState != null)
                currentVideoSelection = (HashMap) savedInstanceState.getSerializable(CURRENT_VIDEO_SELECTION);
            else
                currentVideoSelection = new HashMap<String, List<MediaEntityWrapper>>();
        }

        setContentView(R.layout.sample_activity);

        Button addPhoto = (Button) findViewById(R.id.submitPhotos);
        addPhoto.setOnClickListener(this);
        Button addVideo = (Button) findViewById(R.id.submitVideo);
        addVideo.setOnClickListener(this);
    }

    public void onStart(){
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Received result with code " + requestCode);
        switch (requestCode) {
            case ADD_VIDEO_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    Bundle selectionInfo = data.getExtras();
                    HashMap<String, List<MediaEntityWrapper>> selection = (HashMap) selectionInfo.getSerializable(MultiPicker.SELECTION);
                    Log.d(TAG, "Received selection: " + selection);
                    currentVideoSelection = selection;
                    ArrayList<MediaMetadata> vids = new ArrayList<MediaMetadata>();
                    for(List<MediaEntityWrapper> folder : selection.values()) {
                        for(MediaEntityWrapper video : folder){
                            MediaMetadata vid = new MediaMetadata();
                            vid.setMasterId(video.getMasterId());
                            vid.setMimeType(video.getMimeType());
                            vid.setFileSize(video.getSize());
                            vid.setFilePath(video.getMasterDataPath());
                            vids.add(vid);
                        }
                    }

                    Toast.makeText(this, vids.size() + " video added.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case ADD_PHOTO_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    Bundle selectionInfo = data.getExtras();
                    HashMap<String, List<MediaEntityWrapper>> selection = (HashMap) selectionInfo.getSerializable(MultiPicker.SELECTION);
                    Log.d(TAG, "Received selection: " + selection);
                    currentPhotoSelection = selection;
                    ArrayList<MediaMetadata> pics = new ArrayList<MediaMetadata>();
                    for (List<MediaEntityWrapper> folder : selection.values()) {
                        for(MediaEntityWrapper photo : folder){
                            MediaMetadata pic = new MediaMetadata();
                            pic.setMasterId(photo.getMasterId());
                            pic.setMimeType(photo.getMimeType());
                            pic.setFileSize(photo.getSize());
                            pic.setFilePath(photo.getMasterDataPath());
                            pics.add(pic);
                        }
                    }
                    Toast.makeText(this, pics.size() + " photos added.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default: {
                Log.d(TAG, "Got result from unexpected requestCode: " + requestCode);
            }
        }
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    public void onSaveInstanceState(Bundle outstate){
        super.onSaveInstanceState(outstate);
        Log.d(TAG, "onSaveInstanceState()");
        outstate.putSerializable(CURRENT_PHOTO_SELECTION, currentPhotoSelection);
        outstate.putSerializable(CURRENT_VIDEO_SELECTION, currentVideoSelection);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submitPhotos:
                openPhotoGallery();
                break;
            case R.id.submitVideo:
                openVideoGallery();
                break;
            default:
                Log.d(TAG, "Unknown: " + v.getId());
                break;
        }
    }

    public void openPhotoGallery(){
        Log.d(TAG, "Started MultiPicker for result with requestCode: " + ADD_PHOTO_REQUEST);
        Intent intent = new Intent(this, FolderListActivityFragmented.class);
        intent.putExtra(MultiPicker.MEDIATYPE_CHOICE, MultiPicker.IMAGE_LOADER);
        Bundle currentSelection = new Bundle();
        currentSelection.putSerializable(MultiPicker.SELECTION, currentPhotoSelection);
        Log.d(TAG, "to Intent - Adding selection data: " + currentPhotoSelection);
        intent.putExtras(currentSelection);
        startActivityForResult(intent, ADD_PHOTO_REQUEST);
    }

    public void openVideoGallery(){

        //TODO dialog choose - capture video or choose from video-gallery
        //TODO implement option to select only one or more videos

        Log.d(TAG, "Started MultiPicker for result with requestCode: " + ADD_VIDEO_REQUEST);
        Intent intent = new Intent(this, FolderListActivityFragmented.class);
        intent.putExtra(MultiPicker.MEDIATYPE_CHOICE, MultiPicker.VIDEO_LOADER);
        Bundle currentSelection = new Bundle();
        currentSelection.putSerializable(MultiPicker.SELECTION, currentVideoSelection);
        Log.d(TAG, "to Intent - Adding selection data: " + currentVideoSelection);
        intent.putExtras(currentSelection);

        startActivityForResult(intent, ADD_VIDEO_REQUEST);
    }
}

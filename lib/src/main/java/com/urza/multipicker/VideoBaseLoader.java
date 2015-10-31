package com.urza.multipicker;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Juraj Hasik on 25.7.2014.
 */
public class VideoBaseLoader extends AsyncTaskLoader<List<MediaEntityWrapper>> {

    static final String TAG = VideoBaseLoader.class.getSimpleName();

    // We hold a reference to the Loader’s data here.
    private List<MediaEntityWrapper> mData;
    private SampleObserver mObserver;
    private String parent;

    public VideoBaseLoader(Context ctx, String parent) {
        // Loaders may be used across multiple Activities (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
        this.parent = parent;
    }

    /* ************************************************ */
    /*  (1) A task that performs the asynchronous load  */
    /*                                                  */
    /* ************************************************ */

    public List<MediaEntityWrapper> loadInBackground() {
        // This method is called on a background thread and should generate a
        // new set of data to be delivered back to the client.

        List<MediaEntityWrapper> entityList = new ArrayList<MediaEntityWrapper>();
        // TODO: Perform the query here and add the results to 'data'.
        Log.d(TAG, "Querying Videos...");
        try {
            final Uri baseFilesUri = MediaStore.Files.getContentUri("external");
            Log.d(TAG, "Files Uri: " + baseFilesUri.toString());
            final String[] fileColumns = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.PARENT,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE
            };
            final String filesSelection = MediaStore.Files.FileColumns.MEDIA_TYPE+" = ? AND "
                    +MediaStore.Files.FileColumns.PARENT+" = ?";
            final String[] filesSelectionArgs = {
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    parent};
            final String filesOrderBy = MediaStore.Files.FileColumns._ID + " ASC";

            Cursor videoCursor = getContext().getContentResolver().query(
                    baseFilesUri, fileColumns,
                    filesSelection, filesSelectionArgs, filesOrderBy);

            Log.d(TAG, "VideoCursor Returned " + videoCursor.getCount() + " video files");

            final Uri baseThumbUri = MediaStore.Video.Thumbnails.getContentUri("external");
            Log.d(TAG, "Video Thumbnails baseUri: " + baseThumbUri.toString());
            final String[] thumbColumns = {
                    MediaStore.Video.Thumbnails._ID,
                    MediaStore.Video.Thumbnails.VIDEO_ID,
                    MediaStore.Video.Thumbnails.KIND,
                    MediaStore.Video.Thumbnails.DATA
            };
            final String thumbSelection = MediaStore.Video.Thumbnails.KIND+" IN (?, ?, ?)";
            final String[] thumbSelectionArgs = {
                    String.valueOf(MediaStore.Video.Thumbnails.MICRO_KIND),
                    String.valueOf(MediaStore.Video.Thumbnails.MINI_KIND),
                    String.valueOf(MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)};
            final String thumbOrderBy = MediaStore.Video.Thumbnails.VIDEO_ID + " ASC";

            Cursor thumbCursor = getContext().getContentResolver().query(
                    baseThumbUri, thumbColumns,
                    thumbSelection, thumbSelectionArgs, thumbOrderBy);

            Log.d(TAG, "ThumbCursor Returned " + thumbCursor.getCount() + " thumb files");
            //ENABLE TO TEST NO VIDEO THUMBS SCENARIO
            //thumbCursor = null;
            if ((videoCursor.getCount() > 0) && (thumbCursor.getCount() > 0)) {

                int thumbIdIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails._ID);
                int thumbKindIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.KIND);
                int thumbDataIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA);

                CursorJoiner joiner = new CursorJoiner(videoCursor,
                        new String[]{MediaStore.Files.FileColumns._ID},
                        thumbCursor,
                        new String[]{MediaStore.Video.Thumbnails.VIDEO_ID});

                for (CursorJoiner.Result joinerResult : joiner) {
                    switch (joinerResult) {
                        case LEFT: {
                            MediaEntityWrapper tmpEntity = new MediaEntityWrapper();
                            tmpEntity.masterId = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                            tmpEntity.parent = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));
                            tmpEntity.mimeType = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
                            tmpEntity.masterDataPath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                            tmpEntity.size = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
                            tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.NONE;
                            entityList.add(tmpEntity);
                            break;
                        }
                        case RIGHT:
                            // handle case where a row in cursorB is unique
                            break;
                        case BOTH: {
                            MediaEntityWrapper tmpEntity = new MediaEntityWrapper();
                            tmpEntity.masterId = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                            tmpEntity.parent = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));
                            tmpEntity.mimeType = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
                            tmpEntity.masterDataPath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                            tmpEntity.size = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
                            String[] mimeType = {
                                    getContext().getContentResolver().getType(ContentUris
                                            .withAppendedId(baseThumbUri, Long.parseLong(thumbCursor.getString(thumbIdIndex)))),
                                    getContext().getContentResolver().getType(
                                            Uri.fromFile(new File(thumbCursor.getString(thumbDataIndex))))};

                            if (mimeType[0] != null && mimeType[0].startsWith("image/")) {
                                tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.CONTENT;
                                tmpEntity.setThumbId(thumbCursor.getString(thumbIdIndex));
                            } else if (mimeType[1] != null && mimeType[1].startsWith("image/")) {
                                tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.FILE;
                                tmpEntity.setThumbDataPath(thumbCursor.getString(thumbDataIndex));
                            } else if (new File(thumbCursor.getString(thumbDataIndex)).exists()) {
                                tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.FILE_NULL_MIME;
                                tmpEntity.setThumbDataPath(thumbCursor.getString(thumbDataIndex));
                            } else {
                                //SOLUTION FOR UIL 1.9.4+
                                tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.NONE;
                                //Original video file already set in masterDataPath
                            }
                            //DEBUG
                            if(tmpEntity.thumbURI_type.equals(MultiPicker.ThumbURI_Type.NONE))
                                Log.d(TAG, "Thumbnail entry invalid -  image file does not exist, " +
                                        "falling back to original video file");
                            else
                                Log.d(TAG, "FOUND video Thumb for video " + tmpEntity.masterId +
                                        " Thumb ID: " + thumbCursor.getString(thumbIdIndex) +
                                        " KIND: " + thumbCursor.getString(thumbKindIndex) +
                                        " DATA: " + thumbCursor.getString(thumbDataIndex) +
                                        " ThumbURI_type: " + tmpEntity.thumbURI_type);
                            entityList.add(tmpEntity);
                            break;
                        }
                    }
                }
                Log.d(TAG, "Created Cursor with " + entityList.size() + " videos+thumb");
            } else if(videoCursor.getCount() > 0) {
                Log.d(TAG, "No thumbnails returned - using only videos");
                while(videoCursor.moveToNext()) {
                    MediaEntityWrapper tmpEntity = new MediaEntityWrapper();
                    tmpEntity.masterId = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                    tmpEntity.parent = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));
                    tmpEntity.mimeType = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
                    tmpEntity.masterDataPath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                    tmpEntity.size = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
                    tmpEntity.thumbURI_type = MultiPicker.ThumbURI_Type.NONE;
                    entityList.add(tmpEntity);
                }
            }
            videoCursor.close();
            thumbCursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.reverse(entityList);
        return entityList;
    }

    /* *************************************************** */
    /* (2) Deliver the results to the registered listener  */
    /*                                                     */
    /* *************************************************** */

    public void deliverResult(List<MediaEntityWrapper> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<MediaEntityWrapper> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }

    /* **************************************************** */
    /* (3) Implement the Loader’s state-dependent behavior  */
    /*                                                      */
    /* **************************************************** */

    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        // Begin monitoring the underlying data source.
        if (mObserver == null) {
            mObserver = new SampleObserver(new Handler());
            getContext().getContentResolver().registerContentObserver(MediaStore.Video.Media.getContentUri("external"), true, mObserver);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            Log.d(TAG, "VideoBaseLoader.forceLoad called");
            forceLoad();
        }
    }

    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }


    protected void onReset() {
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mObserver != null) {
            // TODO: unregister the observer
            getContext().getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }

    public void onCanceled(List<MediaEntityWrapper> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<MediaEntityWrapper> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }

    /* *************************************************************** */
    /* (4) Observer which receives notifications when the data changes */
    /*                                                                 */
    /* *************************************************************** */

    //TODO Implement proper ContentObserver
    private class SampleObserver extends ContentObserver {
        public SampleObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            onContentChanged();
            Log.d(TAG, "SampleObserver.onChange fired");
        }
    }
}
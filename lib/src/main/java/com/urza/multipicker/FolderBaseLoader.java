package com.urza.multipicker;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Juraj Hasik on 16.7.2014.
 */
public class FolderBaseLoader extends AsyncTaskLoader<List<MediaFolder>> {

    final static String TAG = FolderBaseLoader.class.getSimpleName();

    // We hold a reference to the Loader’s data here.
    private List<MediaFolder> mData;
    private SampleObserver mObserver;
    private int mediaType;

    public FolderBaseLoader(Context ctx) {
        // Loaders may be used across multiple Activities (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
    }

    public FolderBaseLoader(Context context, int mediaType) {
        super(context);
        this.mediaType = mediaType;
    }

    /* ************************************************ */
    /* (1) A task that performs the asynchronous load   */
    /*                                                  */
    /* ************************************************ */

    @Override
    public List<MediaFolder> loadInBackground() {
        // This method is called on a background thread and should generate a
        // new set of data to be delivered back to the client.
        Map<String, MediaFolder> folderMap = new TreeMap<String, MediaFolder>();

        // TODO: Perform the query here and add the results to 'data'.
        Log.d(TAG, "Querying media Files...");
        try {
            final Uri baseContentUri = MediaStore.Files.getContentUri("external");
            Log.d(TAG, "BaseUri: " + baseContentUri.toString());
            final String[] columns = {
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.PARENT,
                    MediaStore.Files.FileColumns.DATA
            };
            final String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
            String[] selectionArgs = new String[]{""};
            if (mediaType == MultiPicker.IMAGE_LOADER) {
                selectionArgs[0] = String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
            } else if (mediaType == MultiPicker.VIDEO_LOADER) {
                selectionArgs[0] = String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            }
            final String orderBy = MediaStore.Files.FileColumns.PARENT + " ASC, "
                    + MediaStore.Files.FileColumns._ID + " DESC";

            Cursor mediaCursor = getContext().getContentResolver().query(
                    baseContentUri, columns,
                    selection, selectionArgs, orderBy);

            if (mediaCursor != null && mediaCursor.getCount() > 0) {
                Log.d(TAG, "Returned " + mediaCursor.getCount() + " media files");

                int parentIndex = mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT);
                int idIndex = mediaCursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                int mediaTypeIndex = mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
                int filePathIndex = mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

                while (mediaCursor.moveToNext()) {

                    String parent = mediaCursor.getString(parentIndex);
                    String id = mediaCursor.getString(idIndex);
                    String mediaType = mediaCursor.getString(mediaTypeIndex);

                    //TODO Assign folder thumbnail by DATE and(or) DATE_MODIFIED
                    if (!folderMap.containsKey(parent)) {
                        MediaFolder mediaFolder = new MediaFolder(parent);
                        Log.d(TAG, "For parent " + parent + " thumb is video: "
                                + (Integer.parseInt(mediaType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
                        if (Integer.parseInt(mediaType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            Log.d(TAG, "Getting video thumb for parent folder: " + parent);
                            final Uri baseThumbUri = MediaStore.Video.Thumbnails.getContentUri("external");
                            final String[] columnsThumb = {
                                    MediaStore.Video.Thumbnails._ID,
                                    MediaStore.Video.Thumbnails.DATA,
                                    MediaStore.Video.Thumbnails.VIDEO_ID,
                                    MediaStore.Video.Thumbnails.KIND
                            };
                            final String selectionThumb = MediaStore.Video.Thumbnails.VIDEO_ID + " = ? AND " +
                                    MediaStore.Video.Thumbnails.KIND + " IN (?, ?, ?)";
                            final String[] selectionArgsThumb = {
                                    id,
                                    String.valueOf(MediaStore.Video.Thumbnails.MINI_KIND),
                                    String.valueOf(MediaStore.Video.Thumbnails.MICRO_KIND),
                                    String.valueOf(MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)
                            };
                            Cursor thumbCursor = getContext().getContentResolver().query(
                                    baseThumbUri,
                                    columnsThumb,
                                    selectionThumb,
                                    selectionArgsThumb,
                                    null
                            );

                            if (thumbCursor != null && thumbCursor.getCount() > 0) {
                                thumbCursor.moveToNext();
                                int thumbIdIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails._ID);
                                int thumbKindIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.KIND);
                                int thumbDataIndex = thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA);
                                /*
                                 * Apparently mimeTypes of Thumbnails are NOT guaranteed
                                 */
                                String[] mimeType = {
                                        getContext().getContentResolver().getType(ContentUris
                                                .withAppendedId(baseThumbUri, Long.parseLong(thumbCursor.getString(thumbIdIndex)))),
                                        getContext().getContentResolver().getType(
                                                Uri.fromFile(new File(thumbCursor.getString(thumbDataIndex))))};

                                if (mimeType[0] != null && mimeType[0].startsWith("image/")) {
                                    mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.CONTENT;
                                    mediaFolder.setThumbnail(ContentUris.withAppendedId(baseThumbUri,
                                            Long.parseLong(thumbCursor.getString(thumbIdIndex))));
                                } else if (mimeType[1] != null && mimeType[1].startsWith("image/")) {
                                    mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.FILE;
                                    mediaFolder.setThumbnail(Uri.fromFile(new File(thumbCursor.getString(thumbDataIndex))));
                                } else if (new File(thumbCursor.getString(thumbDataIndex)).exists()) {
                                    mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.FILE_NULL_MIME;
                                    mediaFolder.setThumbnail(Uri.fromFile(new File(thumbCursor.getString(thumbDataIndex))));
                                } else {
                                    //SOLUTION FOR UIL 1.9.4+
                                    mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.NONE;
                                    mediaFolder.setOrigVideoUri(ContentUris.withAppendedId(baseContentUri, Long.parseLong(id)));
                                }
                                //DEBUG
                                if(mediaFolder.thumbURI_type.equals(MultiPicker.ThumbURI_Type.NONE))
                                    Log.d(TAG, "Thumbnail entry invalid -  image file does not exist, " +
                                            "falling back to original video file");
                                else
                                    Log.d(TAG, "FOUND video Thumb for folder " + parent +
                                            " Thumb ID: " + thumbCursor.getString(thumbIdIndex) +
                                            " KIND: " + thumbCursor.getString(thumbKindIndex) +
                                            " DATA: " + thumbCursor.getString(thumbDataIndex) +
                                            " ThumbURI_type: " + mediaFolder.thumbURI_type);
                                thumbCursor.close();
                            } else {
                                Log.d(TAG, "NO Thumb found for video id: " + id + ". Passing in original video Uri.");
                                mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.NONE;
                                mediaFolder.setOrigVideoUri(ContentUris.withAppendedId(baseContentUri, Long.parseLong(id)));
                            }
                        } else {
                            /*
                             * There is tiny amount of folders shown at any time,
                             * is it worth fetching thumbnails for each one in case
                             * of images ?
                             */
                            mediaFolder.thumbURI_type = MultiPicker.ThumbURI_Type.CONTENT;
                            mediaFolder.setThumbnail(ContentUris.withAppendedId(baseContentUri, Long.parseLong(id)));
                        }
                        mediaFolder.setCountOfContents(1);
                        folderMap.put(parent, mediaFolder);
                    } else {
                        folderMap.get(parent).incrementCountOfContents();
                    }
                }
                mediaCursor.close();
            }

            //TODO Use one query with IN selection (probably limited to 999 arguments)
            for (MediaFolder folder : folderMap.values()) {

                final Uri baseUri = MediaStore.Files.getContentUri("external");
                final Uri folderUri = ContentUris.withAppendedId(baseUri, Long.parseLong(folder.getIndex()));
                final String[] columns2 = {
                        MediaStore.MediaColumns.DATA
                };
                Cursor folderCursor = getContext().getContentResolver().query(folderUri, columns2, null, null, null);
                folderCursor.moveToNext();
                int dataIndex = folderCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                //fix for sdCard - root of external storage (file id 0)
                if (Long.parseLong(folder.getIndex()) != 0) {
                    String folderPath = folderCursor.getString(dataIndex);
                    folder.setDirName(folderPath.substring(folderPath.lastIndexOf('/') + 1, folderPath.length()));
                } else
                    folder.setDirName("0");
                folderCursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Returned " + folderMap.values().size() + " media folders");
        return new ArrayList<MediaFolder>(folderMap.values());
    }

    /* **************************************************** */
    /* (2) Deliver the results to the registered listener   */
    /*                                                      */
    /* **************************************************** */

    @Override
    public void deliverResult(List<MediaFolder> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<MediaFolder> oldData = mData;
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

    /* ***************************************************** */
    /*  (3) Implement the Loader’s state-dependent behavior  */
    /*                                                       */
    /* ***************************************************** */

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        // Begin monitoring the underlying data source.
        if (mObserver == null) {
            mObserver = new SampleObserver(new Handler());
            getContext().getContentResolver().registerContentObserver(MediaStore.Images.Media.getContentUri("external"), true, mObserver);
            getContext().getContentResolver().registerContentObserver(MediaStore.Video.Media.getContentUri("external"), true, mObserver);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            Log.d(TAG, "forceLoad called");
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
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
            getContext().getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }

    @Override
    public void onCanceled(List<MediaFolder> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<MediaFolder> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }

    /* ***************************************************************** */
    /*  (4) Observer which receives notifications when the data changes  */
    /*                                                                   */
    /* ***************************************************************** */

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



package com.urza.multipicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.net.IDN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by urza on 17.7.2014.
 */
public class MediaFolderAdapter extends BaseAdapter {

    static final String TAG = MediaFolderAdapter.class.getSimpleName();

    private LayoutInflater mLayoutInflater;
    private List<MediaFolder> mFolderList;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private DisplayImageOptions displayImageOptions;
    private HashMap<Long, Integer> idToPositionMap;

    private HashMap<String, List<MediaEntityWrapper>> selection;

    private IdToPositionEvent assignedEvent;

    public MediaFolderAdapter(Context context, int MEDIA_TYPE, IdToPositionEvent e) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mFolderList = new ArrayList<MediaFolder>();
        idToPositionMap = new HashMap<Long, Integer>();
        assignedEvent = e;

        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.empty_photo)
                .showImageForEmptyUri(R.drawable.empty_photo)
                .showImageOnFail(R.drawable.broken_file_icon)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public void setData(List<MediaFolder> data) {
        mFolderList = data;

        if (mFolderList != null) {
            for (int i = 0; i <= mFolderList.size() - 1; i++) {
                idToPositionMap.put(Long.parseLong(mFolderList.get(i).getIndex()), i);
            }
        }
        notifyDataSetChanged();
        assignedEvent.assigned();
    }

    public int getCount() {
        if (mFolderList == null) {
            return 0;
        } else {
            return mFolderList.size();
        }
    }

    //Adapter has stable Ids but overriding this method causes error on
    //AbsListView.confirmCheckedPositionsById()
    /*public boolean hasStableIds() {
        return true;
    }*/

    public Object getItem(int pos) {
        return mFolderList.get(pos);
    }

    public long getItemId(int pos) {
        return Long.parseLong(mFolderList.get(pos).getIndex());
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final MediaFolderHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_media_folder, parent, false);
            viewHolder = new MediaFolderHolder();
            viewHolder.mCount = (TextView) convertView.findViewById(R.id.mediaFolderCount);
            viewHolder.mName = (TextView) convertView.findViewById(R.id.mediaFolderName);
            viewHolder.mNumOfSelected = (TextView) convertView.findViewById(R.id.mediaFolderNumOfSelected);
            viewHolder.mFolderThumb = (ImageView) convertView.findViewById(R.id.folderThumb);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MediaFolderHolder) convertView.getTag();
        }

        viewHolder.mCount.setText("Items: " + mFolderList.get(position).getCountOfContents());
        viewHolder.mCount.setMovementMethod(null);
        viewHolder.mName.setText(mFolderList.get(position).getDirName());
        viewHolder.mName.setMovementMethod(null);
        if (selection.get(mFolderList.get(position).getIndex()) != null) {
            int numOfSelectedEntities = selection.get(mFolderList.get(position).getIndex()).size();
            viewHolder.mNumOfSelected.setText("Selected items: " + numOfSelectedEntities);
        } else {
            viewHolder.mNumOfSelected.setText("Selected items: " + 0);
        }

        switch (mFolderList.get(position).thumbURI_type) {
            case CONTENT: {
                imageLoader.displayImage(
                        mFolderList.get(position).getThumbnail().toString(),
                        viewHolder.mFolderThumb,
                        displayImageOptions);
                break;
            }
            case FILE: {
                imageLoader.displayImage(
                        Uri.decode(mFolderList.get(position).getThumbnail().toString()),
                        viewHolder.mFolderThumb,
                        displayImageOptions);
                break;
            }
            case FILE_NULL_MIME: {
                imageLoader.displayImage(
                        Uri.decode(mFolderList.get(position).getThumbnail().toString()),
                        viewHolder.mFolderThumb,
                        displayImageOptions);
                break;
            }
            case NONE: {
                imageLoader.displayImage(
                        mFolderList.get(position).getOrigVideoUri().toString(),
                        viewHolder.mFolderThumb,
                        displayImageOptions);
                break;
            }
        }

        return convertView;
    }

    public HashMap<String, List<MediaEntityWrapper>> getSelection() {
        return selection;
    }

    public int getPostionForId(long id) {
        return idToPositionMap.get(id);
    }

    public void setSelection(HashMap<String, List<MediaEntityWrapper>> selection) {
        this.selection = selection;
        Log.d(TAG, "selection: " + selection);
    }

    public void updateSelection(HashMap<String, List<MediaEntityWrapper>> selection, String parentIndex, AdapterView adapterView) {
        this.selection = selection;
        Log.d(TAG, "selection: " + selection);
        Log.d(TAG, "Update folderView for index: " + parentIndex);
        int visiblePosition = adapterView.getFirstVisiblePosition();
        View view = adapterView.getChildAt(idToPositionMap.get(Long.parseLong(parentIndex)) - visiblePosition);
        getView(idToPositionMap.get(Long.parseLong(parentIndex)), view, adapterView);
    }

    public class MediaFolderHolder {
        TextView mCount;
        TextView mName;
        TextView mNumOfSelected;
        ImageView mFolderThumb;
    }

    public interface IdToPositionEvent {
        void assigned();
    }
}

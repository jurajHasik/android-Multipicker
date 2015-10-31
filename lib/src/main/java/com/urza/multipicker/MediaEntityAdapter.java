package com.urza.multipicker;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Juraj Hasik on 26.7.2014.
 */
public class MediaEntityAdapter extends BaseAdapter implements MediaEntityBaseAdapter {

    static final String TAG = MediaEntityAdapter.class.getSimpleName();

    private int loaderType;
    private LayoutInflater mLayoutInflater;
    private List<MediaEntityWrapper> mEntityList;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private DisplayImageOptions displayImageOptions;
    private HashMap<String, List<MediaEntityWrapper>> selection;
    private HashMap<Long, Integer> idToPositionMap;

    public MediaEntityAdapter(Context context, int loaderType) {
        this.loaderType = loaderType;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEntityList = new ArrayList<MediaEntityWrapper>();
        idToPositionMap = new HashMap<Long, Integer>();

        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.empty_photo)
                .showImageOnFail(R.drawable.broken_file_icon)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public void setData(List<MediaEntityWrapper> data) {
        mEntityList = data;

        if (mEntityList != null) {
            for (int i = 0; i <= mEntityList.size() - 1; i++) {
                idToPositionMap.put(Long.parseLong(mEntityList.get(i).masterId), i);
            }
        }

        notifyDataSetChanged();
    }

    public int getPositionForId(long id) {
        return idToPositionMap.get(id);
    }

    @Override
    public int getCount() {
        if (mEntityList == null) {
            return 0;
        } else {
            return mEntityList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return mEntityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mEntityList.get(position).masterId);
    }

    //Adapter has stable Ids but overriding this method causes error on
    //AbsListView.confirmCheckedPositionsById()
    /*public boolean hasStableIds() {
        return true;
    }*/

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final EntityHolder viewHolder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_media_entity, parent, false);
            viewHolder = new EntityHolder();
            viewHolder.mThumb = (SquareImageView) convertView.findViewById(R.id.entityThumb);
            /*
                Optional information available
            viewHolder.mId = (TextView) convertView.findViewById(R.id.entityId);
            viewHolder.mMime = (TextView) convertView.findViewById(R.id.entityMimeType);
            viewHolder.mContentUri = (TextView) convertView.findViewById(R.id.entityContentUri);
            viewHolder.mData = (TextView) convertView.findViewById(R.id.entityData);
            viewHolder.mThumbId = (TextView) convertView.findViewById(R.id.thumbId);
            viewHolder.mThumbData = (TextView) convertView.findViewById(R.id.thumbData);*/
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (EntityHolder) convertView.getTag();
        }

        /*
            Optional information available
        viewHolder.mId.setText("Id: " + mEntityList.get(position).masterId);
        viewHolder.mMime.setText("MimeType: " + mEntityList.get(position).mimeType);
        viewHolder.mContentUri.setText("ContentUri: " + ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"),
                Long.parseLong(mEntityList.get(position).masterId));
        viewHolder.mData.setText("Data: " + mEntityList.get(position).masterDataPath);
        viewHolder.mThumbId.setText("Thumb id: " + mEntityList.get(position).thumbId);
        viewHolder.mThumbData.setText("Thumb data: " + mEntityList.get(position).thumbDataPath);*/

        switch (mEntityList.get(position).thumbURI_type) {
            case CONTENT: {
                imageLoader.displayImage(
                        ContentUris.withAppendedId(MediaStore.Images.Thumbnails.getContentUri("external"),
                                Long.parseLong(mEntityList.get(position).thumbId)).toString(),
                        viewHolder.mThumb,
                        displayImageOptions);
                break;
            }
            case FILE: {
                imageLoader.displayImage(
                        Uri.decode(Uri.fromFile(new File(mEntityList.get(position).getThumbDataPath())).toString()),
                        viewHolder.mThumb,
                        displayImageOptions);
                break;
            }
            case FILE_NULL_MIME: {
                imageLoader.displayImage(
                        Uri.decode(Uri.fromFile(new File(mEntityList.get(position).getThumbDataPath())).toString()),
                        viewHolder.mThumb,
                        displayImageOptions);
                break;
            }
            case NONE: {
                imageLoader.displayImage(
                        //Using file:// style Uri instead of content:// makes this independent on media type
                        Uri.decode(Uri.fromFile(new File(mEntityList.get(position).getMasterDataPath())).toString()),
                        viewHolder.mThumb,
                        displayImageOptions);
                break;
            }
        }
        return convertView;
    }

    public void removeAndRedraw(int position, AbsListView absListView){
        mEntityList.remove(position);
        //We click on view to remove it from AbsListView data
        //and so it changes the checked state of a view on position to false
        //however since new and still checked item occupies current view
        //we need to change its checked state back to true
        absListView.setItemChecked(position, true);
        notifyDataSetChanged();
    }

    public void setSelection(HashMap<String, List<MediaEntityWrapper>> selection) {
        this.selection = selection;
    }

    private class EntityHolder {
        ImageView mThumb;
        /*
            Another possible information for display
        TextView mId;
        TextView mMime;
        TextView mContentUri;
        TextView mData;
        TextView mThumbId;
        TextView mThumbData;*/
    }

}

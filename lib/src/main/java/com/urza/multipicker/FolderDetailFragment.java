package com.urza.multipicker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a single Folder detail screen.
 * This fragment is always contained in a {@link FolderListActivityFragmented}
 * in two-pane mode (on tablets) or on handsets.
 */
public class FolderDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MediaEntityWrapper>> {

    static final String TAG = FolderDetailFragment.class.getSimpleName();
    
    public static final String ARG_ITEM_ID = "item_id";
    OnEntitySelectedListener mCallback;
    private int MEDIA_TYPE;
    private String parentIndex;
    private GridView mGridView;
    private MediaEntityBaseAdapter mAdapter;
    private HashMap<String, List<MediaEntityWrapper>> selection;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FolderDetailFragment() {
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnEntitySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName()
                    + " must implement OnEntitySelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            parentIndex = getArguments().getString(ARG_ITEM_ID);
        }
        MEDIA_TYPE = getArguments().getInt(MultiPicker.MEDIATYPE_CHOICE);
        selection = (HashMap) getArguments().getSerializable(MultiPicker.SELECTION);
        if (selection != null) Log.d(TAG, "Successfully loaded selection from args");
        else Log.d(TAG, "No selection was included in args");
        Log.d(TAG, "selection: " + selection.toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_folder_detail, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.mediaEntityGrid);
        //TODO Set number of Columns dynamically according to display px & density
        //if(((FolderListActivityFragmented) getActivity()).ismTwoPane()) {}
        mGridView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (selection.get(parentIndex).contains(mAdapter.getItem(position))) {
                            selection.get(parentIndex).remove(mAdapter.getItem(position));
                            Log.d(TAG, "Item was CHECKED so its REMOVED");
                            mCallback.onEntitySelected(selection, parentIndex);
                        } else {
                            selection.get(parentIndex).add((MediaEntityWrapper) mAdapter.getItem(position));
                            Log.d(TAG, "Item was ADDED");
                            mCallback.onEntitySelected(selection, parentIndex);
                        }
                        Log.d(TAG, "Checked item Id: " + id);
                        Log.d(TAG, "Checked item Ids: " + Arrays.toString(((GridView) parent).getCheckedItemIds()));
                        Log.d(TAG, "Num of checked items: " + ((GridView) parent).getCheckedItemCount());
                    }
                }
        );

        return rootView;
    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        mAdapter = new MediaEntityAdapter(getActivity(), MEDIA_TYPE);
        mAdapter.setSelection(selection);
        mGridView.setAdapter(mAdapter);
        //TODO Listener is too Strict on flinging
        mGridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));

        getLoaderManager().initLoader(MEDIA_TYPE, null, this);
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    public Loader<List<MediaEntityWrapper>> onCreateLoader(int loaderID, Bundle bundle) {

        switch (loaderID) {
            case MultiPicker.IMAGE_LOADER: {
                // Returns a new ImageBaseLoader
                Log.d(TAG, "ImageBaseLoader Created");
                return new ImageBaseLoader(getActivity(), parentIndex);
            }
            case MultiPicker.VIDEO_LOADER: {
                Log.d(TAG, "VideoBaseLoader Created");
                return new VideoBaseLoader(getActivity(), parentIndex);
            }
            default:
                Log.d(TAG, "Invalid parent " + parentIndex);
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<MediaEntityWrapper>> loader, List<MediaEntityWrapper> data) {
        mAdapter.setData(data);
        SparseBooleanArray previouslyCheckedItems = mGridView.getCheckedItemPositions();
        Log.d(TAG, "Checked items: " + previouslyCheckedItems.toString());
        //Erase previous checked states on views
        for (int i=0;i<=previouslyCheckedItems.size()-1;i++) {
            mGridView.setItemChecked(previouslyCheckedItems.keyAt(i), false);
        }
        for (MediaEntityWrapper entity : selection.get(parentIndex)) {
            mGridView.setItemChecked(mAdapter.getPositionForId(Long.parseLong(entity.masterId)), true);
        }
        Log.d(TAG, "onLoadFinished");
    }

    @Override
    public void onLoaderReset(Loader<List<MediaEntityWrapper>> loader) {
        mAdapter.setData(null);
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onPause");
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Log.d(TAG, "selection: " + selection.toString());
    }

    // Container Activity must implement this interface
    public interface OnEntitySelectedListener {
        void onEntitySelected(HashMap<String, List<MediaEntityWrapper>> selection, String parentIndex);
    }
}

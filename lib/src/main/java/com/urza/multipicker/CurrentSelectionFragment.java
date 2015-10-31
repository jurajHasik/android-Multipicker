package com.urza.multipicker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Juraj Hasik on 29.7.2014.
 */
public class CurrentSelectionFragment extends Fragment {

    static final String TAG = CurrentSelectionFragment.class.getSimpleName();

    //Not sure about this
    FolderDetailFragment.OnEntitySelectedListener mCallback;
    private int MEDIA_TYPE;
    private GridView mGridView;
    private MediaEntityBaseAdapter mAdapter;
    private HashMap<String, List<MediaEntityWrapper>> selection;
    private boolean onResumeFirstTime = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CurrentSelectionFragment() {
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (FolderDetailFragment.OnEntitySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName()
                    + " must implement OnEntitySelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        ((FolderListActivityFragmented) getActivity()).setCurrentFolderId(FolderListActivityFragmented.IN_SELECTION);
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
        View rootView = inflater.inflate(R.layout.fragment_selection_detail, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.entitySelectionGrid);
        mGridView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String parentIndex = ((MediaEntityWrapper) mAdapter.getItem(position)).parent;
                        if (selection.get(parentIndex).contains(mAdapter.getItem(position))) {
                            selection.get(parentIndex).remove(mAdapter.getItem(position));
                            mCallback.onEntitySelected(selection, parentIndex);
                            mAdapter.removeAndRedraw(position, mGridView);
                        }
                        Log.d(TAG, "Checked item Id: " + id);
                        //Does not work since hasStableIds() of underlying adapter is not overriden
                        //Log.d(TAG, "Checked item Ids: " + Arrays.toString(((GridView) parent).getCheckedItemIds()));
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
        List<MediaEntityWrapper> selectedEntities = new ArrayList<MediaEntityWrapper>();
        for(List<MediaEntityWrapper> folderSelection : selection.values()){
            selectedEntities.addAll(folderSelection);
        }
        mAdapter.setData(selectedEntities);
        mGridView.setAdapter(mAdapter);
        //TODO Listener is too Strict on flinging
        mGridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));

        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onResume() {
        super.onResume();
        if (!onResumeFirstTime) {
            for(int i=0; i <= mAdapter.getCount()-1;i++){
                mGridView.setItemChecked(i, true);
            }
            onResumeFirstTime = true;
        }
        Log.d(TAG, "onResume");
    }

    public void onSaveInstanceState(Bundle outstate) {
        super.onSaveInstanceState(outstate);
        outstate.putSerializable(MultiPicker.SELECTION, selection);
        outstate.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
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
}

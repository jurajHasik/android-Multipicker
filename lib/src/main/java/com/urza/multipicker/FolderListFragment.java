package com.urza.multipicker;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.HashMap;
import java.util.List;

/**
 * A list fragment representing a list of Folders. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link FolderDetailFragment}.x
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class FolderListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<MediaFolder>>, ToggleSelectionEvent, MediaFolderAdapter.IdToPositionEvent {

    static final String TAG = FolderListFragment.class.getSimpleName();
    
    private static final int MEDIA_FOLDER_LOADER = 0;
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private MediaFolderAdapter mAdapter;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id, int position) {
        }
    };
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * The current activated item position and id, since the position
     * might become inaccurate if the media files are changed
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private long mActivatedId = ListView.INVALID_POSITION;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FolderListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folder_list, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        FolderListActivityFragmented fA = ((FolderListActivityFragmented) getActivity());
        /*
         * If we are in twoPane mode, get notification from FolderListActivityFragmented
         * about toggleSelection
         */
        boolean twoPane = getResources().getBoolean(R.bool.twoPane);
        if(twoPane) fA.setSelectionEvent(this);
        if (fA.getSelection() == null)
            Log.d(TAG, "Got null selection");
        mAdapter = new MediaFolderAdapter(getActivity(), fA.getMEDIA_TYPE(), this);
        mAdapter.setSelection(fA.getSelection());
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(MEDIA_FOLDER_LOADER, null, this);
    }

    @Override
    public Loader<List<MediaFolder>> onCreateLoader(int arg0, Bundle arg1) {
        Log.d(TAG, "onCreateLoader");
        return new FolderBaseLoader(getActivity(), ((FolderListActivityFragmented) getActivity()).getMEDIA_TYPE());
    }

    @Override
    public void onLoadFinished(Loader<List<MediaFolder>> arg0, List<MediaFolder> data) {
        mAdapter.setData(data);
        Log.d(TAG, "onLoadFinished");
    }

    @Override
    public void onLoaderReset(Loader<List<MediaFolder>> arg0) {
        mAdapter.setData(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
        boolean twoPane = getResources().getBoolean(R.bool.twoPane);
        setActivateOnItemClick(twoPane);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Activities containing this fragment must implement its callbacks.
        if (!(context instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onPause");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }


    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(Long.toString(id), position);
        mActivatedPosition = position;
        mActivatedId = id;
        Log.d(TAG, "Folder with Id " + Long.toString(id) + " selected");
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }



    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }
        mActivatedPosition = position;
    }

    public void setmActivatedId(long mActivatedId) {
        this.mActivatedId = mActivatedId;
    }

    public long getmActivatedId() {
        return mActivatedId;
    }

    public void setAdapterSelection(HashMap<String, List<MediaEntityWrapper>> selection) {
        mAdapter.setSelection(selection);
    }

    public void updateAdapterSelection(HashMap<String, List<MediaEntityWrapper>> selection, String parentIndex) {
        Log.d(TAG, "selection: " + selection);
        mAdapter.updateSelection(selection, parentIndex, getListView());
    }

    @Override
    public void selectionToggled(boolean b) {
        Log.d(TAG, "selectionToggled "+b);
        if(b)
            getListView().setItemChecked(mActivatedPosition, false);
        else
            getListView().setItemChecked(mActivatedPosition, true);
    }

    @Override
    public void assigned() {
        if(mActivatedId != ListView.INVALID_POSITION) {
            mActivatedPosition = mAdapter.getPostionForId(mActivatedId);
        }
        View rootView = getView();
        if(rootView != null) {
            setActivatedPosition(mActivatedPosition);
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(String id, int position);
    }

}

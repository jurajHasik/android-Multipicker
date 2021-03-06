package com.urza.multipicker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.urza.multipicker.FolderDetailFragment.ARG_FOLDER_NAME;

/**
 * An activity representing a list of Folders. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link FolderDetailFragment} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link FolderListFragment} and the item details
 * (if present) is a {@link FolderDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link FolderListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class FolderListActivityFragmented extends FragmentActivity
        implements FolderListFragment.Callbacks, FolderDetailFragment.OnEntitySelectedListener {

    final static String TAG = FolderListActivityFragmented.class.getSimpleName();
    final static String TAG_TWOPANE = "2";

    private static final String WAS_TWO_PANE = "WAS_TWO_PANE";
    private static final String DETAIL_ID = "DETAIL_ID";
    private static final String ACTIVATED_ID = "ACTIVATED_ID";

    private HashMap<String, List<MediaEntityWrapper>> selection;

    private int MEDIA_TYPE;
    private boolean twoPane;
    private long activatedFolderId = ListView.INVALID_POSITION;
    private String activatedFolderName;
    /*
     * Tracking what folder is user navigating
     * IN_SELECTION for "Current selection"
     * PRISTINE for not selecting any folder yet
     * 0..N real folder id
     */
    private int detailId = PRISTINE;
    static final int PRISTINE = -1;
    static final int IN_SELECTION = -2;
    //When no FolderListFragment in twoPane mode does not exist
    private static ToggleSelectionEvent dummyEvent = new ToggleSelectionEvent() {
        @Override
        public void selectionToggled(boolean b) {

        }
    };
    private ToggleSelectionEvent selectionEvent = dummyEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate - twoPane: " + getResources().getBoolean(R.bool.twoPane));
        if(savedInstanceState == null) {
            /*
             * This fork is executed when the Activity's onCreate runs for a first time
             */
            twoPane = getResources().getBoolean(R.bool.twoPane);
            if (twoPane) {
                FolderListFragment folderListFragment = new FolderListFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.folder_list_container,
                        folderListFragment, FolderListFragment.TAG+TAG_TWOPANE).commit();
            } else {
                // Create a new Fragment to be placed in the activity layout
                FolderListFragment folderListFragment = new FolderListFragment();

                // In case this activity was started with special instructions from an
                // Intent, pass the Intent's extras to the fragment as arguments
                folderListFragment.setArguments(getIntent().getExtras());

                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, folderListFragment, FolderListFragment.TAG).commit();
            }

            // TODO: If exposing deep links into your app, handle intents here.
            MEDIA_TYPE = getIntent().getIntExtra(MultiPicker.MEDIATYPE_CHOICE, 0);
            if (MEDIA_TYPE == 0) {
                Log.d(TAG, "Unsupported Media type chosen");
            } else {
                Log.d(TAG, "Chosen Media type: " + MEDIA_TYPE);
            }
            Bundle args = getIntent().getExtras();
            if (args != null && args.containsKey(MultiPicker.SELECTION)) {
                selection = (HashMap) args.getSerializable(MultiPicker.SELECTION);
                Log.d(TAG, "Retaining selection from bundle");
            } else {
                selection = new HashMap<String, List<MediaEntityWrapper>>();
                Log.d(TAG, "Creating new empty selection");
            }
        } else {
            /*
             * This fork is executed when the Activity is "resurrected". We leave creation
             * of Fragments to onRestoreInstanceState
             */
            selection = (HashMap) savedInstanceState.getSerializable(MultiPicker.SELECTION);
            MEDIA_TYPE = savedInstanceState.getInt(MultiPicker.MEDIATYPE_CHOICE);
            detailId = savedInstanceState.getInt(DETAIL_ID, PRISTINE);
            activatedFolderId = savedInstanceState.getLong(ACTIVATED_ID, ListView.INVALID_POSITION);
            activatedFolderName = savedInstanceState.getString(ARG_FOLDER_NAME);
        }
        Log.d(TAG, "selection: " + selection.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        twoPane = getResources().getBoolean(R.bool.twoPane);
        boolean wasTwoPane = savedInstanceState.getBoolean(WAS_TWO_PANE);
        Log.d(TAG, "onRestoreInstanceState - twoPane: " + twoPane);
        Log.d(TAG, "Previous state - twoPane: " + wasTwoPane
                + " detailId: " + detailId);
        FragmentManager fm = getSupportFragmentManager();

        if (twoPane && !wasTwoPane) {
            FolderListFragment fL = new FolderListFragment();
            fL.setmActivatedId(activatedFolderId);
            fm.beginTransaction().replace(R.id.folder_list_container,
                    fL, FolderListFragment.TAG + TAG_TWOPANE).commit();
            /*
             * If portrait -> landscape configuration change happened
             * we have 3 options
             * a) User has not selected any folder yet - FolderListFragment
             * b) User is navigating through selected folder - FolderDetailFragment
             * c) User is navigating through current selection - SelectionDetailFragment
             */
            switch (detailId) {
                case PRISTINE: {
                    //We always have FolderListFragment in twoPane mode
                    break;
                }
                case IN_SELECTION: {
                    Bundle args = new Bundle();
                    args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
                    args.putSerializable(MultiPicker.SELECTION, selection);
                    CurrentSelectionFragment f = new CurrentSelectionFragment();
                    f.setArguments(args);
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.replace(R.id.folder_detail_container_fragmented, f, CurrentSelectionFragment.TAG + TAG_TWOPANE);
                    transaction.commit();
                    toggleSelectedButtons(true);
                    break;
                }
                default: {
                    Bundle args = new Bundle();
                    args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
                    args.putString(FolderDetailFragment.ARG_FOLDER_ID, String.valueOf(detailId));
                    args.putString(ARG_FOLDER_NAME, activatedFolderName);
                    args.putSerializable(MultiPicker.SELECTION, selection);
                    FolderDetailFragment f = new FolderDetailFragment();
                    f.setArguments(args);
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.replace(R.id.folder_detail_container_fragmented, f, FolderDetailFragment.TAG + TAG_TWOPANE);
                    transaction.commit();
                }
            }
        }

        if (!twoPane && wasTwoPane) {
            /*
             * We revert back to dummy interface, since FolderListFragment in single pane mode
             * does not highlight its list items
             */
            selectionEvent = dummyEvent;
            /*
             * There are three options when coming from landscape mode
             * a) User has not selected any folder yet - FolderListFragment
             * b) User is navigating through selected folder - FolderDetailFragment
             * c) User is navigating through current selection - SelectionDetailFragment
             */
            switch (detailId) {
                case PRISTINE: {
                    /*
                     * First, remove Everything on BackStack
                     */
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    FolderListFragment f = new FolderListFragment();
                    f.setArguments(getIntent().getExtras());
                    fm.beginTransaction()
                            .add(R.id.fragment_container, f, FolderListFragment.TAG).commit();
                    break;
                }
                case IN_SELECTION: {
                    /*
                     * Get previousId from which the Selection was accessed
                     * a) previousID == PRISTINE
                     * b) previousID >= 0, therefore current detailID == IN_SELECTION was accessed
                     * from FolderDetail with id = previousId
                     *
                     * BackStackEntry has Name in form of detailId's: "from"->"to"
                     */
                    String[] ids = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName().split("->", 2);
                    if (!ids[1].contentEquals(String.valueOf(IN_SELECTION)))
                        throw new IllegalStateException("Invalid FragmentTransaction - " + ids[0] + "->" + ids[1]);
                    int previousId = Integer.valueOf(ids[0]);
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    FolderListFragment f0 = new FolderListFragment();
                    f0.setArguments(getIntent().getExtras());
                    fm.beginTransaction()
                            .add(R.id.fragment_container, f0, FolderListFragment.TAG).commit();

                    if (previousId >= 0) {
                        // b) Selection comes from FolderDetail for Folder id = previousId
                        Bundle args = new Bundle();
                        args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
                        args.putString(FolderDetailFragment.ARG_FOLDER_ID, String.valueOf(previousId));
                        args.putString(ARG_FOLDER_NAME, activatedFolderName);
                        args.putSerializable(MultiPicker.SELECTION, selection);
                        FolderDetailFragment f1 = new FolderDetailFragment();
                        f1.setArguments(args);
                        FragmentTransaction transaction = fm.beginTransaction();
                        transaction.replace(R.id.fragment_container, f1, FolderDetailFragment.TAG);
                        transaction.addToBackStack(PRISTINE + "->" + previousId);
                        transaction.commit();
                    }

                    Bundle args = new Bundle();
                    args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
                    args.putSerializable(MultiPicker.SELECTION, selection);
                    CurrentSelectionFragment f2 = new CurrentSelectionFragment();
                    f2.setArguments(args);
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.replace(R.id.fragment_container, f2, CurrentSelectionFragment.TAG);
                    switch (previousId) {
                        // a)
                        case PRISTINE:
                            transaction.addToBackStack(PRISTINE + "->" + IN_SELECTION);
                            break;
                        // b)
                        default:
                            transaction.addToBackStack(previousId + "->" + IN_SELECTION);
                    }
                    transaction.commit();
                    toggleSelectedButtons(true);

                    break;
                }
                default: {
                    /*
                     * First, remove Everything on BackStack
                     */
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    FolderListFragment f0 = new FolderListFragment();
                    f0.setArguments(getIntent().getExtras());
                    fm.beginTransaction()
                            .add(R.id.fragment_container, f0, FolderListFragment.TAG).commit();

                    Bundle args = new Bundle();
                    args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
                    args.putString(FolderDetailFragment.ARG_FOLDER_ID, String.valueOf(detailId));
                    args.putString(ARG_FOLDER_NAME, activatedFolderName);
                    args.putSerializable(MultiPicker.SELECTION, selection);
                    FolderDetailFragment f1 = new FolderDetailFragment();
                    f1.setArguments(args);
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.replace(R.id.fragment_container, f1, FolderDetailFragment.TAG);
                    /*
                     * Create backStack with single transaction from FolderList to FolderDetail
                     */
                    transaction.addToBackStack(PRISTINE + "->" + detailId);
                    transaction.commit();
                }
            }
        }
    }


    public void onSaveInstanceState(Bundle outstate) {
        super.onSaveInstanceState(outstate);
        Log.d(TAG, "onSaveInstanceState");
        Log.d(TAG, "selection: " + selection.toString());
        Log.d(TAG, "saving - twoPane: " + getResources().getBoolean(R.bool.twoPane) + " detailId: " + detailId);
        outstate.putSerializable(MultiPicker.SELECTION, selection);
        outstate.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
        /*
         * Apparently, when onSaveInstanceState is called due to the configuration change
         * landscape <-> portrait, the res/values-*-land/* are not in place
         */
        outstate.putBoolean(WAS_TWO_PANE, twoPane);
        outstate.putInt(DETAIL_ID, detailId);
        outstate.putLong(ACTIVATED_ID, activatedFolderId);
        outstate.putString(ARG_FOLDER_NAME, activatedFolderName);
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Log.d(TAG, "selection: " + selection.toString());
    }

    public void onBackPressed(){
        if(getResources().getBoolean(R.bool.twoPane)) {
            /*
             * Depends on what logic you want to use. Current implementation
             * finishes FolderListActivityFragmented
             */
            finish();
        } else {
            if (detailId == IN_SELECTION) toggleSelectedButtons(false);
            super.onBackPressed();
        }
    }

    /**
     * Callback method from {@link FolderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id, int position, String name) {
        detailId = Integer.valueOf(id);
        activatedFolderId = Long.parseLong(id);
        activatedFolderName = name;
        if (getResources().getBoolean(R.bool.twoPane)) {
            Log.d(TAG, "onItemSelected for TwoPane layout");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
            args.putString(FolderDetailFragment.ARG_FOLDER_ID, id);
            args.putString(FolderDetailFragment.ARG_FOLDER_NAME, name);
            if (!selection.containsKey(id) || selection.get(id) == null)
                selection.put(id, new ArrayList<MediaEntityWrapper>());
            args.putSerializable(MultiPicker.SELECTION, selection);
            FolderDetailFragment f = new FolderDetailFragment();
            f.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.folder_detail_container_fragmented, f, FolderDetailFragment.TAG + TAG_TWOPANE);
            //transaction.addToBackStack("folderDetail");
            transaction.commit();
            // If the selection was toggled, revert back to gallery state
            toggleSelectedButtons(false);
        } else {
            // In single-pane mode, simply start the detail fragment
            // for the selected item ID.
            Bundle args = new Bundle();
            args.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
            args.putString(FolderDetailFragment.ARG_FOLDER_ID, id);
            args.putString(FolderDetailFragment.ARG_FOLDER_NAME, name);
            if (!selection.containsKey(id) || selection.get(id) == null) {
                Log.d(TAG, "Created selection list for folder id: " + id);
                selection.put(id, new ArrayList<MediaEntityWrapper>());
            }
            args.putSerializable(MultiPicker.SELECTION, selection);
            // / Create fragment and give it an argument specifying the folder it should show
            FolderDetailFragment f = new FolderDetailFragment();
            f.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, f, FolderDetailFragment.TAG);
            transaction.addToBackStack(PRISTINE + "->" + detailId);

            // Commit the transaction
            transaction.commit();
        }
    }

    public void toggleSelection(View view) throws IOException, XmlPullParserException {
        //Is the toggle on?
        //boolean on = ((ToggleButton) view).isChecked();
        boolean on = view.isActivated();

        if (!on) {
            // Create fragment showing currently selected media entities
            view.setActivated(true);
            toggleSelectedButtons(true);
            Bundle selectionInfo = new Bundle();
            selectionInfo.putInt(MultiPicker.MEDIATYPE_CHOICE, MEDIA_TYPE);
            selectionInfo.putSerializable(MultiPicker.SELECTION, selection);
            CurrentSelectionFragment f = new CurrentSelectionFragment();
            f.setArguments(selectionInfo);
            if (getResources().getBoolean(R.bool.twoPane)) {
                selectionEvent.selectionToggled(true);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.folder_detail_container_fragmented, f,
                        CurrentSelectionFragment.TAG+TAG_TWOPANE);
                transaction.addToBackStack(detailId +"->"+IN_SELECTION);
                transaction.commit();
            } else {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, f, CurrentSelectionFragment.TAG);
                transaction.addToBackStack(detailId +"->"+IN_SELECTION);
                transaction.commit();
            }
            detailId = IN_SELECTION;
        } else {
            // Return back to gallery
            view.setActivated(false);
            toggleSelectedButtons(false);
            if (getResources().getBoolean(R.bool.twoPane)) {
                selectionEvent.selectionToggled(false);
                getSupportFragmentManager().popBackStack();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }
    }

    public void toggleSelectedButtons(boolean isSelection) {
        if(isSelection) {
            ((Button) findViewById(R.id.toggleGallery)).setTextColor(Color.WHITE);
            ((Button) findViewById(R.id.toggleSelected)).setTextColor(Color.BLACK);
            findViewById(R.id.toggleGallery).setActivated(true);
            findViewById(R.id.toggleSelected).setActivated(true);
        } else {
            ((Button) findViewById(R.id.toggleGallery)).setTextColor(Color.BLACK);
            ((Button) findViewById(R.id.toggleSelected)).setTextColor(Color.WHITE);
            findViewById(R.id.toggleGallery).setActivated(false);
            findViewById(R.id.toggleSelected).setActivated(false);
        }
    }

    public void goBack(View v){
        onBackPressed();
    }

    public void confirmSelection(View view) {
        //Handle the final selection user has chosen
        //for example finish the activity and include selection in result
        Log.d(TAG, "Selection confirmed: " + selection.toString());
        Intent result = new Intent("com.urza.mediapicker.RESULT_ACTION");
        Bundle selectedMedia = new Bundle();
        selectedMedia.putSerializable(MultiPicker.SELECTION, selection);
        result.putExtras(selectedMedia);
        setResult(FragmentActivity.RESULT_OK, result);
        finish();
    }

    @Override
    public void onEntitySelected(HashMap<String, List<MediaEntityWrapper>> selection, String parentIndex) {
        this.selection = selection;
        Log.d(TAG, "selection: " + selection);
        if (getResources().getBoolean(R.bool.twoPane)) {
            ((FolderListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.folder_list_container)).updateAdapterSelection(selection, parentIndex);
        }
    }

    public int getMEDIA_TYPE() {
        return MEDIA_TYPE;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public HashMap<String, List<MediaEntityWrapper>> getSelection() {
        return selection;
    }

    public void setSelection(HashMap<String, List<MediaEntityWrapper>> selection) {
        this.selection = selection;
    }

    public void setSelectionEvent(ToggleSelectionEvent selectionEvent) {
        this.selectionEvent = selectionEvent;
    }

    void DEBUGBACKSTACK() {
        /* DEBUG
         * Check what is recorded in FragmentManager backStack
         */
        FragmentManager fm = getSupportFragmentManager();
        Log.d(TAG, "BackStackEntry count: " + fm.getBackStackEntryCount());
        for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
            Log.d(TAG, "BackStackEntry[" + i + "] id: " + fm.getBackStackEntryAt(i).getId()
                    + " name: " + fm.getBackStackEntryAt(i).getName());
        }
    }
}

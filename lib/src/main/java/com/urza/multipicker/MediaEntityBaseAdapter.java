package com.urza.multipicker;

import android.widget.AbsListView;
import android.widget.ListAdapter;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Juraj Hasik on 25.7.2014.
 */
public interface MediaEntityBaseAdapter extends ListAdapter {

    void setData(List<MediaEntityWrapper> data);

    void setSelection(HashMap<String, List<MediaEntityWrapper>> selection);

    int getPositionForId(long id);

    void removeAndRedraw(int position, AbsListView adapterView);
}

package com.urza.multipicker;

import android.net.Uri;

/**
 * Created by Juraj Hasik on 16.7.2014.
 */
public class MediaFolder {

    String index;
    String dirName;
    int countOfContents;
    MultiPicker.ThumbURI_Type thumbURI_type;
    Uri thumbnailUri;
    Uri origVideoUri;

    int numOfSelectedItems;

    public MediaFolder(String index) {
        this.index = index;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public int getCountOfContents() {
        return countOfContents;
    }

    public void setCountOfContents(int countOfContents) {
        this.countOfContents = countOfContents;
    }

    public void incrementCountOfContents() {
        this.countOfContents = this.countOfContents + 1;
    }

    public Uri getThumbnail() {
        return thumbnailUri;
    }

    public void setThumbnail(Uri thumbnail) {
        this.thumbnailUri = thumbnail;
    }

    public Uri getOrigVideoUri() {
        return origVideoUri;
    }

    public void setOrigVideoUri(Uri origVideoUri) {
        this.origVideoUri = origVideoUri;
    }

    public int getNumOfSelectedItems() {
        return numOfSelectedItems;
    }

    public void setNumOfSelectedItems(int numOfSelectedItems) {
        this.numOfSelectedItems = numOfSelectedItems;
    }
}


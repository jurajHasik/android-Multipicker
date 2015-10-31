package com.urza.multipicker;

import java.io.Serializable;

/**
 * Created by Juraj Hasik on 25.7.2014.
 */
public class MediaEntityWrapper implements Serializable {

    String masterId;
    String thumbId;
    String mimeType;
    String parent;
    String masterDataPath;
    String thumbDataPath;
    long size;
    MultiPicker.ThumbURI_Type thumbURI_type;

    public boolean equals(Object o){
        if(o instanceof MediaEntityWrapper) {
            if (((MediaEntityWrapper) o).getMasterId().equals(this.getMasterId())) return true;
        }
        return false;
    }

    public long getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getMasterId() {
        return masterId;
    }

    public void setMasterId(String masterId) {
        this.masterId = masterId;
    }

    public String getThumbId() {
        return thumbId;
    }

    public void setThumbId(String thumbId) {
        this.thumbId = thumbId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getMasterDataPath() {
        return masterDataPath;
    }

    public void setMasterDataPath(String masterDataPath) {
        this.masterDataPath = masterDataPath;
    }

    public String getThumbDataPath() {
        return thumbDataPath;
    }

    public void setThumbDataPath(String thumbDataPath) {
        this.thumbDataPath = thumbDataPath;
    }

    public String toString(){
        return masterId;
    }
}

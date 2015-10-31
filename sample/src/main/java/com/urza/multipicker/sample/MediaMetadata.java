package com.urza.multipicker.sample;

import java.io.Serializable;

/**
 * Created by Juraj Hasik on 9.11.2014.
 */
public class MediaMetadata implements Serializable {

    String masterId;
    String filePath;
    String mimeType;
    long fileSize;

    public String getMasterId() {
        return masterId;
    }

    public void setMasterId(String id) {
        this.masterId = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

}

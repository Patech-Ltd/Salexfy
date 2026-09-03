package com.patechltd.salexfypos.drive;

public class DriveBackupEntry {
    public final String fileId;
    public final String name;
    public final long createdTime;
    public final long sizeBytes;

    public DriveBackupEntry(String fileId, String name, long createdTime, long sizeBytes) {
        this.fileId = fileId;
        this.name = name;
        this.createdTime = createdTime;
        this.sizeBytes = sizeBytes;
    }
}

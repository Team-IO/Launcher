package com.skcraft.launcher.builder;


/**
 * Stores all the data for a mod (json object)
 */
class HashObject {
    private  String Hash;
    private  String Location;
    private  String To;
    private  long Size;

    void setHash(String hash) {
        this.Hash = hash;
    }

    void setLocation(String location) {
        Location = location;
    }

    void setTo(String to) {
        To = to;
    }

    void setSize(long size) {
        Size = size;
    }

    String getLocation() {
        return Location;
    }

    String getTo() {
        return To;
    }

    long getSize() {
        return Size;
    }

    String getHash() {
        return Hash;
    }
}

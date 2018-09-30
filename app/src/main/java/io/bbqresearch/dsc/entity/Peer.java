package io.bbqresearch.dsc.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "peers")
public class Peer {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    @ColumnInfo(name = "peer_name")
    private String peer_name;
    @NonNull
    @ColumnInfo(name = "last_seen")
    private String last_seen;
    @NonNull
    @ColumnInfo(name = "location_lat")
    private String gps_lat;
    @NonNull
    @ColumnInfo(name = "location_long")
    private String gps_long;
    @NonNull
    @ColumnInfo(name = "gps_alt")
    private String gps_alt;
    @NonNull
    @ColumnInfo(name = "rssi")
    private String rssi;
    @NonNull
    @ColumnInfo(name = "snr")
    private String snr;

    public Peer(@NonNull String peer_name,
                @NonNull String last_seen,
                @NonNull String gps_lat,
                @NonNull String gps_long,
                @NonNull String gps_alt,
                @NonNull String rssi,
                @NonNull String snr) {
        this.peer_name = peer_name;
        this.last_seen = last_seen;
        this.gps_lat = gps_lat;
        this.gps_long = gps_long;
        this.gps_alt = gps_alt;
        this.rssi = rssi;
        this.snr = snr;
    }

    @NonNull
    public String getPeer_name() {
        return peer_name;
    }

    @NonNull
    public String getLast_seen() {
        return last_seen;
    }

    @NonNull
    public String getGps_lat() {
        return gps_lat;
    }

    @NonNull
    public String getGps_long() {
        return gps_long;
    }

    @NonNull
    public String getGps_alt() {
        return gps_alt;
    }

    @NonNull
    public String getRssi() {
        return rssi;
    }

    @NonNull
    public String getSnr() {
        return snr;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

package io.bbqresearch.roomwordsample.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "msg")
    private String msg;

    @NonNull
    @ColumnInfo(name = "author")
    private String author;

    @NonNull
    @ColumnInfo(name = "orig_timestamp")
    private int origTimestamp;

    @NonNull
    @ColumnInfo(name = "recv_timestamp")
    private int recvTimestamp;


    public Message(@NonNull String msg,
                   @NonNull String author,
                   @NonNull int origTimestamp,
                   @NonNull int recvTimestamp) {
        this.id = 0;
        this.msg = msg;
        this.author = author;
        this.origTimestamp = origTimestamp;
        this.recvTimestamp = recvTimestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return this.msg;
    }

    public String getAuthor() {
        return this.author;
    }

    @NonNull
    public int getOrigTimestamp() {
        return origTimestamp;
    }

    @NonNull
    public int getRecvTimestamp() {
        return recvTimestamp;
    }

}

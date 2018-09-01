package io.bbqresearch.dsc.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "msgcypher")
    private String msgcypher;

    @NonNull
    @ColumnInfo(name = "msg")
    private String msg;

    @NonNull
    @ColumnInfo(name = "author")
    private String author;

    @NonNull
    @ColumnInfo(name = "orig_timestamp")
    private long origTimestamp;

    @NonNull
    @ColumnInfo(name = "recv_timestamp")
    private long recvTimestamp;


    @NonNull
    @ColumnInfo(name = "isFromHere")
    private boolean isFromHere = false;

    public Message(@NonNull String msgcypher,
                   @NonNull String msg,
                   @NonNull String author,
                   @NonNull long origTimestamp,
                   @NonNull long recvTimestamp,
                   @NonNull boolean isFromHere) {
        this.msgcypher = msgcypher;
        this.msg = msg;
        this.author = author;
        this.origTimestamp = origTimestamp;
        this.recvTimestamp = recvTimestamp;
        this.isFromHere = isFromHere;
    }

    public String getMsg() {
        return this.msg;
    }

    public String getAuthor() {
        return this.author;
    }

    @NonNull
    public long getOrigTimestamp() {
        return origTimestamp;
    }

    @NonNull
    public long getRecvTimestamp() {
        return recvTimestamp;
    }

    @NonNull
    public boolean isFromHere() {
        return isFromHere;
    }

    @NonNull
    public String getMsgcypher() {
        return msgcypher;
    }

}

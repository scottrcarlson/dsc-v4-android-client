package io.bbqresearch.dsc.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import io.bbqresearch.dsc.entity.Message;

@Dao
public interface MessageDao {

    @Insert
    void insert(Message message);

    @Query("delete from messages")
    void deleteAll();

    @Query("select * from messages order by recv_timestamp asc")
    LiveData<List<Message>> getAllMessages();

}

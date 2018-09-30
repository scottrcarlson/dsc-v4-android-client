package io.bbqresearch.dsc.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import io.bbqresearch.dsc.entity.Peer;


@Dao
public interface PeerDao {

    @Insert
    void insert(Peer peer);

    @Query("delete from peers")
    void deleteAll();

    @Query("select * from peers order by last_seen asc")
    LiveData<List<Peer>> getAllPeerData();

    @Query("select * from peers group by peer_name order by last_seen desc")
    LiveData<List<Peer>> getLastSeenPeers();

}

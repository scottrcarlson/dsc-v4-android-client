package io.bbqresearch.dsc.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import io.bbqresearch.dsc.dao.PeerDao;
import io.bbqresearch.dsc.entity.Peer;

@Database(entities = {Peer.class}, version = 1)
public abstract class PeerRoomDatabase extends RoomDatabase {

    private static PeerRoomDatabase INSTANCE;
    //Create call back for when database is first opened.
    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    public static PeerRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PeerRoomDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PeerRoomDatabase.class, "peers")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract PeerDao peerDao();

    //clear and populate database
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final PeerDao mDao;

        PopulateDbAsync(PeerRoomDatabase db) {
            mDao = db.peerDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            return null;
        }
    }
}

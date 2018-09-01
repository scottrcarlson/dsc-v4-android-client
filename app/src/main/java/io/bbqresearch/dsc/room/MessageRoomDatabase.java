package io.bbqresearch.dsc.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import io.bbqresearch.dsc.dao.MessageDao;
import io.bbqresearch.dsc.entity.Message;

@Database(entities = {Message.class}, version = 5)
public abstract class MessageRoomDatabase extends RoomDatabase {

    private static MessageRoomDatabase INSTANCE;
    //Create call back for when database is first opened.
    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    public static MessageRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MessageRoomDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MessageRoomDatabase.class, "messages")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract MessageDao messageDao();

    //clear and populate database
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final MessageDao mDao;

        PopulateDbAsync(MessageRoomDatabase db) {
            mDao = db.messageDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            //mDao.deleteAll();
            //Message message = new Message("Hello","Joe",0,10);
            //mDao.insert(message);
            return null;
        }
    }
}

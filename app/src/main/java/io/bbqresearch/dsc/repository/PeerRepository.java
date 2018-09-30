package io.bbqresearch.dsc.repository;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

import io.bbqresearch.dsc.dao.PeerDao;
import io.bbqresearch.dsc.entity.Peer;
import io.bbqresearch.dsc.room.PeerRoomDatabase;
import io.bbqresearch.dsc.service.DscServiceUpgrade;


public class PeerRepository extends BroadcastReceiver {
    private final static String TAG = MessageRepository.class.getSimpleName();
    private DscServiceUpgrade dscService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscServiceUpgrade.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }
    };
    private PeerDao mPeerDao;
    private final BroadcastReceiver dscUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (DscServiceUpgrade.ACTION_NEW_BEACON.equals(action)) {
                try {
                    JSONObject nodeRoot = new JSONObject(intent.getStringExtra(DscServiceUpgrade.EXTRA_DATA));
                    JSONObject payload = nodeRoot.getJSONObject("payload");
                    Peer peer = new Peer(payload.getString("author"),
                            payload.getString("sent_time"),
                            payload.getString("lat"),
                            payload.getString("long"),
                            "none",
                            payload.getString("rssi"),
                            payload.getString("snr")
                    );

                    new insertAsyncTask(mPeerDao).execute(peer);

                } catch (Exception e) {
                    Log.e(TAG, "Inbound NewMsg Error: " + Log.getStackTraceString(e));
                }
            }
        }
    };
    private LiveData<List<Peer>> mAllPeers;
    private LiveData<List<Peer>> mLastSeenPeers;

    public PeerRepository(Application application) {
        PeerRoomDatabase db = PeerRoomDatabase.getDatabase(application);
        mPeerDao = db.peerDao();
        mAllPeers = mPeerDao.getAllPeerData();
        mLastSeenPeers = mPeerDao.getLastSeenPeers();
        Intent gattServiceIntent = new Intent(application, DscServiceUpgrade.class);
        application.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        application.registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DscServiceUpgrade.ACTION_NEW_BEACON);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive Called: " + intent.getAction());
        /***** For start Service  ****/
        Intent myIntent = new Intent(context, DscServiceUpgrade.class);
        context.startService(myIntent);
    }

    public LiveData<List<Peer>> getmAllPeers() {
        return mAllPeers;
    }

    public LiveData<List<Peer>> getLastSeen() {
        return mLastSeenPeers;
    }

    public void insert(Peer peer) {
        new insertAsyncTask(mPeerDao).execute(peer);
    }

    public void deleteAll() {
        mPeerDao.deleteAll();
    }

    private static class insertAsyncTask extends AsyncTask<Peer, Void, Void> {
        private PeerDao mAsyncTaskDao;

        insertAsyncTask(PeerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Peer... params) {
            try {
                mAsyncTaskDao.insert(params[0]);
            } catch (Exception e) {

            }
            return null;
        }
    }
}
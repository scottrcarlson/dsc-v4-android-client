package io.bbqresearch.dsc.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import io.bbqresearch.dsc.entity.Peer;
import io.bbqresearch.dsc.repository.PeerRepository;

public class PeerViewModel extends AndroidViewModel {

    private PeerRepository mRepository;
    private LiveData<List<Peer>> mLastSeenPeers;

    public PeerViewModel(Application application) {
        super(application);
        mRepository = new PeerRepository(application);
        mLastSeenPeers = mRepository.getLastSeen();
    }

    public LiveData<List<Peer>> getLastSeenPeers() {
        return mLastSeenPeers;
    }

}
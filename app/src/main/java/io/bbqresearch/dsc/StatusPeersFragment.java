package io.bbqresearch.dsc;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.bbqresearch.dsc.adapter.PeerListAdapter;
import io.bbqresearch.dsc.entity.Peer;
import io.bbqresearch.dsc.viewmodel.PeerViewModel;
import io.bbqresearch.roomwordsample.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatusPeersFragment extends Fragment {

    private PeerViewModel mPeerViewModel;

    public StatusPeersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_status_peers, container, false);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mPeerViewModel = ViewModelProviders.of(this).get(PeerViewModel.class);
        final RecyclerView recyclerView = getView().findViewById(R.id.peer_recycleview);
        final PeerListAdapter adapter = new PeerListAdapter(this.getContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        //Observe LiveData returned by getAllWords in ViewModel. If this activity is in the foreground
        //it will call onChanged

        mPeerViewModel.getLastSeenPeers().observe(this, new Observer<List<Peer>>() {
            @Override
            public void onChanged(@Nullable final List<Peer> peers) {
                // Update the cached copy of the peers in the adapter.
                adapter.setPeers(peers);
            }
        });

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.peer_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.action_delete_peers:
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        mPeerViewModel.deleteAll();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}

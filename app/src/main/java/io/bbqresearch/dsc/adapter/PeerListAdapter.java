package io.bbqresearch.dsc.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.bbqresearch.dsc.entity.Peer;
import io.bbqresearch.roomwordsample.R;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    private final LayoutInflater mInflater;
    private List<Peer> mPeers;

    public PeerListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }


    @Override
    public PeerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        itemView = mInflater.inflate(R.layout.peer_list_item, parent, false);

        return new PeerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PeerViewHolder holder, int position) {
        if (mPeers != null) {
            Peer current = mPeers.get(position);
            long time = System.currentTimeMillis() / 1000L;
            Log.d("TESING", "time: " + time + " lastseen" + current.getLast_seen());


            long seen_since = (time - Long.parseLong(current.getLast_seen())) / 60;
            String seen_msg = "";
            if (seen_since > 0) seen_msg = seen_since + " minutes ago";
            else seen_msg = "now";
            holder.peerLastSeen.setText(seen_msg);
            holder.peerNameView.setText(current.getPeer_name());
            holder.peerRssi.setText(current.getRssi());
            holder.peerSnr.setText(current.getSnr());

        } else {
            // Covers the case of data not being ready yet.
            holder.peerNameView.setText("No Peers");
        }
    }

    public void setPeers(List<Peer> peers) {
        mPeers = peers;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mPeers != null)
            return mPeers.size();
        else return 0;
    }

    class PeerViewHolder extends RecyclerView.ViewHolder {
        private final TextView peerLastSeen;
        private final TextView peerNameView;
        private final TextView peerRssi;
        private final TextView peerSnr;

        private PeerViewHolder(View itemView) {
            super(itemView);
            peerLastSeen = itemView.findViewById(R.id.textLastSeen);
            peerNameView = itemView.findViewById(R.id.textPeerName);
            peerRssi = itemView.findViewById(R.id.textRssi);
            peerSnr = itemView.findViewById(R.id.textSnr);
        }
    }
}
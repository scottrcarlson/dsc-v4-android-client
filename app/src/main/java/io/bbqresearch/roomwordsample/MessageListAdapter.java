package io.bbqresearch.roomwordsample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.bbqresearch.roomwordsample.entity.Message;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {

    private final LayoutInflater mInflater;
    private List<Message> mMessages;

    private int SENT_TYPE = 0;
    private int RECV_TYPE = 1;
    MessageListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    //Returns the view type of the item at position for the purposes of view recycling.
    @Override
    public int getItemViewType(int position) {
        if (mMessages.get(position).isFromHere()) {
            return SENT_TYPE;
        } else {
            return RECV_TYPE;
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == SENT_TYPE) {
            itemView = mInflater.inflate(R.layout.sent_message, parent, false);
        } else {
            itemView = mInflater.inflate(R.layout.recieved_message, parent, false);
        }

        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        if (mMessages != null) {
            Message current = mMessages.get(position);
            holder.messageItemView.setText(current.getMsg());
            holder.messageAuthorView.setText(current.getAuthor());

        } else {
            // Covers the case of data not being ready yet.
            holder.messageItemView.setText("No Messages");
        }
    }

    void setMessages(List<Message> messages) {
        mMessages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mMessages != null)
            return mMessages.size();
        else return 0;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageItemView;
        private final TextView messageAuthorView;
        private MessageViewHolder(View itemView) {
            super(itemView);
            messageItemView = itemView.findViewById(R.id.textView);
            messageAuthorView = itemView.findViewById(R.id.textAuthor);
        }
    }
}
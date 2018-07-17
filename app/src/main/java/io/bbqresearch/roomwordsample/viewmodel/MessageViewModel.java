package io.bbqresearch.roomwordsample.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import io.bbqresearch.roomwordsample.entity.Message;
import io.bbqresearch.roomwordsample.repository.MessageRepository;

public class MessageViewModel extends AndroidViewModel {

    private MessageRepository mRepository;

    private LiveData<List<Message>> mAllMessages;

    public MessageViewModel(Application application) {
        super(application);
        mRepository = new MessageRepository(application);
        mAllMessages = mRepository.getAllMessages();


    }

    public LiveData<List<Message>> getAllMessages() {
        return mAllMessages;
    }

    public void deleteAll() {
        mRepository.deleteAll();
    }

    public void insert(Message message) {
        mRepository.insert(message);
    }
}
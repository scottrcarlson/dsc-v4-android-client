package io.bbqresearch.dsc.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import io.bbqresearch.dsc.entity.Message;
import io.bbqresearch.dsc.repository.MessageRepository;

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
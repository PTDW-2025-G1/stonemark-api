package pt.estga.chatbot.telegram;

public interface ChatLock {

    void lock(long chatId);

    void unlock(long chatId);
}
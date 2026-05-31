package pt.estga.chatbot.telegram;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ChatLock {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void lock(long chatId) {
        ReentrantLock lock = locks.computeIfAbsent(chatId, k -> new ReentrantLock());
        lock.lock();
    }

    public void unlock(long chatId) {
        ReentrantLock lock = locks.get(chatId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
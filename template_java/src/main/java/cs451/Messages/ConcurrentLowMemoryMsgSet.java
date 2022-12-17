package cs451.Messages;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLowMemoryMsgSet {
    // Map<agreementId, Map<sourceId, MessageHash>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<Integer, Boolean>> messages;
    private final AtomicInteger flushedUntil = new AtomicInteger(0);
    private final int vs;

    public ConcurrentLowMemoryMsgSet(int p, int vs) {
        this.messages = new ConcurrentHashMap<>(p);
        this.vs = vs;
    }

    public boolean add(MessageToBeSent e) {
        if (e.getMessage().getAgreementId() < flushedUntil.get()) {
            return false;
        }
        boolean wasAdded = messages.putIfAbsent(e.getMessage().getAgreementId(),
                ConcurrentHashMap.newKeySet(vs)) == null;
        wasAdded |= messages.get(e.getMessage().getAgreementId()).add(e.hashCode());
        return wasAdded;
    }

    public boolean contains(MessageToBeSent e) {
        ConcurrentHashMap.KeySetView<Integer, Boolean> set = messages.get(e.getMessage().getAgreementId());
        return e.getMessage().getAgreementId() < flushedUntil.get()
                || set != null && set.contains(e.hashCode());
    }

    public synchronized void flush(int agreementId) {
        messages.remove(agreementId);
        flushedUntil.compareAndSet(Integer.max(flushedUntil.get(), agreementId), agreementId);
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [messages=" + messages + ", vs=" + vs + "]";
    }
}

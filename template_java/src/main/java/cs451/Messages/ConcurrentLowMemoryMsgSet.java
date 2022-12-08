package cs451.Messages;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLowMemoryMsgSet {
    // Map<agreementId, Map<sourceId, MessageHash>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<Integer, Boolean>> messages;
    private final int vs;

    public ConcurrentLowMemoryMsgSet(int p, int vs) {
        this.messages = new ConcurrentHashMap<>(p);
        this.vs = vs;
    }

    public boolean add(Message e) {
        boolean wasAdded = messages.putIfAbsent(e.getAgreementId(), ConcurrentHashMap.newKeySet(vs)) == null;
        wasAdded |= messages.get(e.getAgreementId()).add(e.hashCode());
        return wasAdded;
    }

    public boolean contains(Message e) {
        return messages.containsKey(e.getAgreementId()) && messages.get(e.getAgreementId()).contains(e.hashCode());
    }

    public synchronized void flush(int agreementId) {
        if (messages.containsKey(agreementId)) {
            messages.get(agreementId).clear();
            messages.remove(agreementId);
        }
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [messages=" + messages + ", vs=" + vs + "]";
    }
}

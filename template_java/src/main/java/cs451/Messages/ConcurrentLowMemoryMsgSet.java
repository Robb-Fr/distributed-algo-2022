package cs451.Messages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cs451.Host;

public class ConcurrentLowMemoryMsgSet {
    // Map<agreementId, Map<sourceId, MessageHash>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Short, ConcurrentHashMap.KeySetView<Integer, Boolean>>> messages;
    private final int nbHosts;
    private final int vs;

    public ConcurrentLowMemoryMsgSet(Map<Short, Host> hostsMap, int p, int vs) {
        this.nbHosts = hostsMap.size();
        this.messages = new ConcurrentHashMap<>(p);
        this.vs = vs;
    }

    public boolean add(Message e) {
        boolean wasAdded = messages.putIfAbsent(e.getAgreementId(), new ConcurrentHashMap<>(nbHosts)) == null;
        wasAdded |= messages.get(e.getAgreementId()).putIfAbsent(e.getSourceId(),
                ConcurrentHashMap.newKeySet(vs)) == null;
        wasAdded |= messages.get(e.getAgreementId()).get(e.getSourceId()).add(e.hashCode());
        return wasAdded;
    }

    public boolean contains(Message e) {
        return messages.containsKey(e.getAgreementId()) && messages.get(e.getAgreementId()).containsKey(e.getSourceId())
                && messages.get(e.getAgreementId()).get(e.getSourceId()).contains(e.hashCode());
    }

    public synchronized void flush(int agreementId) {
        if (messages.containsKey(agreementId)) {
            messages.get(agreementId).clear();
            messages.remove(agreementId);
        }
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [messages=" + messages + ", nbHosts=" + nbHosts + "]";
    }

}

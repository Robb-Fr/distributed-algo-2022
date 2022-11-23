package cs451.Messages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Constants;
import cs451.Host;

public class ConcurrentLowMemoryMsgSet<M extends Message> {
    private final ConcurrentHashMap<Short, ConcurrentHashMap.KeySetView<Message, Boolean>> delivered;
    private final ConcurrentHashMap<Short, AtomicInteger> deliveredUntil;

    public ConcurrentLowMemoryMsgSet(Map<Short, Host> hostsMap) {
        this.delivered = new ConcurrentHashMap<>(hostsMap.size());
        this.deliveredUntil = new ConcurrentHashMap<>(hostsMap.size());
        int N = hostsMap.size();
        for (short host : hostsMap.keySet()) {
            this.delivered.put(host, ConcurrentHashMap.newKeySet(N * Constants.MAX_OUT_OF_ORDER_DELIVERY));
            this.deliveredUntil.put(host, new AtomicInteger(0));
        }
    }

    public synchronized void flush(short host, int deliveredUntil) {
        int newMax = this.deliveredUntil.get(host).updateAndGet(i -> Integer.max(i, deliveredUntil));
        this.delivered.get(host).removeIf(m -> m.getId() < newMax);
    }

    public synchronized boolean add(M e) {
        return delivered.get(e.getSourceId()).add(e);
    }

    public synchronized boolean remove(M e) {
        return delivered.get(e.getSourceId()).remove(e);
    }

    public synchronized boolean contains(M e) {
        return deliveredUntil.get(e.getSourceId()).get() >= e.getId() || delivered.get(e.getSourceId()).contains(e);
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [delivered=" + delivered + ", deliveredUntil=" + deliveredUntil + "]";
    }

}

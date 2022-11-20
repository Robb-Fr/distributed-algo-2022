package cs451.Messages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Constants;
import cs451.Host;

public class ConcurrentLowMemoryMsgSet<M extends Message> {
    private final ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<Message, Boolean>> delivered;
    private final ConcurrentHashMap<Integer, AtomicInteger> deliveredUntil;

    public ConcurrentLowMemoryMsgSet(Map<Integer, Host> hostsMap) {
        this.delivered = new ConcurrentHashMap<>(hostsMap.size());
        this.deliveredUntil = new ConcurrentHashMap<>(hostsMap.size());
        for (Host host : hostsMap.values()) {
            this.delivered.put(host.getId(), ConcurrentHashMap.newKeySet(2 * Constants.MAX_OUT_OF_ORDER_DELIVERY));
            this.deliveredUntil.put(host.getId(), new AtomicInteger(0));
        }
    }

    public synchronized void flush(Host host, int deliveredUntil) {
        int newMax = this.deliveredUntil.get(host.getId()).updateAndGet(i -> Integer.max(i, deliveredUntil));
        this.delivered.get(host.getId()).removeIf(m -> m.getId() < newMax);
    }

    public synchronized boolean add(M e) {
        return delivered.get(e.getSourceId()).add(e);
    }

    public synchronized boolean remove(M e) {
        return delivered.get(e.getSourceId()).remove(e);
    }

    public synchronized boolean contains(M e) {
        boolean deliveredAbove = deliveredUntil.get(e.getSourceId()).get() >= e.getId();
        return deliveredAbove || delivered.get(e.getSourceId()).contains(e);
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [delivered=" + delivered + ", deliveredUntil=" + deliveredUntil + "]";
    }

}

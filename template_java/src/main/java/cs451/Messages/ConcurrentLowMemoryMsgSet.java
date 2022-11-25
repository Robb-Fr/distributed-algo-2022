package cs451.Messages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;

public class ConcurrentLowMemoryMsgSet<M extends Message> {
    private final ConcurrentHashMap<Short, ConcurrentSkipListSet<M>> delivered;
    private final ConcurrentHashMap<Short, AtomicInteger> deliveredUntil;

    public ConcurrentLowMemoryMsgSet(Map<Short, Host> hostsMap) {
        this.delivered = new ConcurrentHashMap<>(hostsMap.size());
        this.deliveredUntil = new ConcurrentHashMap<>(hostsMap.size());
        // int N = hostsMap.size();
        for (short host : hostsMap.keySet()) {
            this.delivered.put(host, new ConcurrentSkipListSet<>());
            this.deliveredUntil.put(host, new AtomicInteger(0));
        }
    }

    public synchronized void flush(short host, M deliveredFrom, M deliveredUntil) {
        // int newMax = this.deliveredUntil.get(host).updateAndGet(i -> Integer.max(i,
        // deliveredUntil.getId()));
        this.delivered.get(host).subSet(deliveredFrom, deliveredUntil).clear();
    }

    public boolean add(M e) {
        return deliveredUntil.get(e.getSourceId()).get() < e.getId() && delivered.get(e.getSourceId()).add(e);
    }

    public boolean contains(M e) {
        return deliveredUntil.get(e.getSourceId()).get() >= e.getId() || delivered.get(e.getSourceId()).contains(e);
    }

    @Override
    public String toString() {
        return "ConcurrentLowMemoryMsgSet [delivered=" + delivered + ", deliveredUntil=" + deliveredUntil + "]";
    }

}

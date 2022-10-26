package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BestEffortBroadcast implements Deliverable {
    private final PerfectLink link;
    private final Map<Integer, Host> hostsMap;
    private final Deliverable parent;

    public BestEffortBroadcast(byte myId, Map<Integer, Host> hostsMap, Deliverable parent,
            AtomicReference<DatagramSocket> socket) {
        if (parent == null || hostsMap == null || socket == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have null parent or a null self host or a null socket reference or null hosts");
        }
        this.link = new PerfectLink(myId, hostsMap, this, socket);
        this.hostsMap = hostsMap;
        this.parent = parent;
    }

    public BestEffortBroadcast(byte myId, Map<Integer, Host> hostsMap) throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.link = new PerfectLink(myId, hostsMap);
        this.hostsMap = hostsMap;
        this.parent = null;
    }

    public void broadcast(Message m) throws InterruptedException {
        for (Host host : hostsMap.values()) {
            link.sendPerfect(m, host);
        }
    }

    @Override
    public void deliver(Message m) {
        parent.deliver(m);
    }

}

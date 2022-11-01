package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BestEffortBroadcast implements Deliverable, SocketGiver {
    private final int myId;
    private final PerfectLink link;
    private final Map<Integer, Host> hostsMap;
    private final Deliverable parent;

    /**
     * Constructor for a Beb to be given to a receiver
     * 
     * @param myId
     * @param hostsMap
     * @param parent
     * @param socket
     */
    public BestEffortBroadcast(int myId, Map<Integer, Host> hostsMap, Deliverable parent,
            AtomicReference<DatagramSocket> socket) {
        if (parent == null || hostsMap == null || socket == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have null parent or a null self host or a null socket reference or null hosts");
        }
        this.myId = myId;
        this.link = new PerfectLink(myId, hostsMap, this, socket);
        this.hostsMap = hostsMap;
        this.parent = parent;
    }

    /**
     * Constructor for a Beb to be given to a sender
     * 
     * @param myId
     * @param hostsMap
     * @throws SocketException
     * @throws UnknownHostException
     */
    public BestEffortBroadcast(int myId, Map<Integer, Host> hostsMap) throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.myId = myId;
        this.link = new PerfectLink(myId, hostsMap);
        this.hostsMap = hostsMap;
        this.parent = null;
    }

    public void broadcast(Message m) throws InterruptedException {
        if (m == null) {
            throw new IllegalArgumentException("Cannot broadcast a null message");
        }
        for (Host host : hostsMap.values()) {
            if (host.getId() == myId) {
                deliver(m);
            } else {
                link.sendPerfect(m, host);
            }
        }
    }

    @Override
    public void deliver(Message m) {
        if (parent == null) {
            throw new IllegalAccessError("Cannot access the parent of a sender");
        }
        parent.deliver(m);
    }

    @Override
    public AtomicReference<DatagramSocket> getSocket() {
        return link.getSocket();
    }

}

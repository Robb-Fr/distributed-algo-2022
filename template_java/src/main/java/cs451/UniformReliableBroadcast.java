package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class UniformReliableBroadcast implements Deliverable, SocketGiver, PendingGiver {
    private final BestEffortBroadcast beb;
    private final int myId;
    private final Map<Integer, Host> hostsMap;
    private final Deliverable parent;
    private final Set<Message> delivered;
    private final AtomicReference<Set<Message>> pending;
    private final Map<Message, Set<Host>> ack;

    /**
     * Constructor for an Urb to be given to a receiver
     * 
     * @param myId
     * @param hostsMap
     * @param parent
     * @param socket
     * @param pending
     */
    public UniformReliableBroadcast(int myId, Map<Integer, Host> hostsMap, Deliverable parent,
            AtomicReference<DatagramSocket> socket, AtomicReference<Set<Message>> pending) {
        if (parent == null || hostsMap == null || socket == null || pending == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have null parent or a null self host or a null socket reference or null hosts or null pending");
        }
        this.pending = pending;
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.beb = new BestEffortBroadcast(myId, hostsMap, parent, socket);
        this.parent = parent;
        this.delivered = null;
        this.ack = null;
    }

    /**
     * Constructor to be given to a sender
     * 
     * @param myId
     * @param hostsMap
     * @throws SocketException
     * @throws UnknownHostException
     */
    public UniformReliableBroadcast(int myId, Map<Integer, Host> hostsMap)
            throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.myId = myId;
        this.pending = new AtomicReference<>(new HashSet<>());
        this.delivered = new HashSet<>();
        this.ack = new HashMap<>();
        this.hostsMap = hostsMap;
        this.beb = new BestEffortBroadcast(myId, hostsMap);
        this.parent = null;
    }

    public void broadcast(Message m) throws InterruptedException {
        if (m == null) {
            throw new IllegalArgumentException("Cannot broadcast a null message");
        }
        pending.get().add(m);
        beb.broadcast(m);
    }

    @Override
    public void deliver(Message m) {
        if (parent == null || ack == null) {
            throw new IllegalAccessError("Sender cannot deliver");
        }
        if (!ack.containsKey(m)) {
            ack.put(m, new HashSet<>(hostsMap.size()));
        }
        ack.get(m).add(hostsMap.get(m.getSenderId()));
        if (!pending.get().contains(m)) {
            pending.get().add(m);
            Message newMessage = m.withUpdatedSender(myId);
            try {
                beb.broadcast(newMessage);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("Failed to broadcast");
            }
        }
        tryDeliver(m);
    }

    @Override
    public AtomicReference<DatagramSocket> getSocket() {
        return beb.getSocket();
    }

    @Override
    public AtomicReference<Set<Message>> getPending() {
        return pending;
    }

    private boolean canDeliver(Message m) {
        if (ack == null) {
            throw new IllegalAccessError("Sender cannot deliver");
        }
        int N = hostsMap.size();
        return ack.containsKey(m) && ack.get(m).size() > (N / 2);
    }

    private void tryDeliver(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }
        if (delivered == null || parent == null) {
            throw new IllegalAccessError("Sender cannot deliver a message");
        }
        if (pending.get().contains(m) && canDeliver(m) && !delivered.contains(m)) {
            delivered.add(m);
            parent.deliver(m);
        }
    }

}

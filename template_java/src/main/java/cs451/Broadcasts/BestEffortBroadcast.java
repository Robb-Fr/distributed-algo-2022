package cs451.Broadcasts;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import cs451.Host;
import cs451.Messages.Message;
import cs451.States.PlState;
import cs451.States.PlStateGiver;

public class BestEffortBroadcast implements Deliverable, PlStateGiver, Flushable {
    private final PerfectLink link;
    private final Map<Short, Host> hostsMap;
    private final Deliverable parent;
    private final ActorType type;
    private Thread plThread = null;

    /**
     * Constructor for a Beb to be given to a sender
     */
    public BestEffortBroadcast(short myId, Map<Short, Host> hostsMap)
            throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map or parent");
        }
        this.link = new PerfectLink(myId, hostsMap);
        this.hostsMap = hostsMap;
        this.type = ActorType.SENDER;
        this.parent = null;
    }

    /**
     * Constructor for a Beb to be given to a receiver
     */
    public BestEffortBroadcast(short myId, Map<Short, Host> hostsMap, Deliverable parent, PlState plState) {
        if (parent == null || hostsMap == null || plState == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have null parent or a null self host or a null socket reference or null hosts");
        }
        this.link = new PerfectLink(myId, hostsMap, this, plState);
        this.hostsMap = hostsMap;
        this.type = ActorType.RECEIVER;
        this.parent = parent;
    }

    public void broadcast(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot broadcast a null message");
        }
        if (type != ActorType.SENDER) {
            throw new IllegalStateException("You need to be a sender to broadcast");
        }
        for (short host : hostsMap.keySet()) {
            link.addToSend(m, host);
        }
    }

    public void startPl() {
        plThread = new Thread(link);
        plThread.start();
    }

    public void interruptPl() {
        if (plThread != null) {
            plThread.interrupt();
        }
    }

    @Override
    public void deliver(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver null messages");
        }
        if (type != ActorType.RECEIVER) {
            throw new IllegalStateException("Sender cannot deliver messages");
        }
        parent.deliver(m);
    }

    @Override
    public PlState getPlState() {
        return link.getPlState();
    }

    @Override
    public void flush(short host, int deliveredUntil) {
        link.flush(host, deliveredUntil);
    }

}

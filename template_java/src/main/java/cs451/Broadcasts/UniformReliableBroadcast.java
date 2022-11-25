package cs451.Broadcasts;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs451.Host;
import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;
import cs451.States.PlState;
import cs451.States.PlStateGiver;
import cs451.States.UrbSate;
import cs451.States.UrbStateGiver;
import cs451.Constants;

public class UniformReliableBroadcast implements Deliverable, PlStateGiver, UrbStateGiver, Runnable, Flushable {
    private final BestEffortBroadcast beb;
    private final short myId;
    private final Map<Short, Host> hostsMap;
    private final int halfHosts;
    private final Deliverable parent;
    private final ConcurrentHashMap.KeySetView<Message, Boolean> urbPending;
    private final ConcurrentLowMemoryMsgSet<Message> urbDelivered;
    private final ConcurrentHashMap<Message, ConcurrentHashMap.KeySetView<Short, Boolean>> urbAck;
    private final ConcurrentLinkedQueue<Message> urbToBroadcast;
    private final ConcurrentLinkedQueue<Message> urbToDeliver;
    private final ConcurrentLinkedQueue<Message> urbToRetryDeliver = new ConcurrentLinkedQueue<>();
    private final ActorType type;
    private long previousFlush = System.currentTimeMillis();
    private long previousRetryFlush = System.currentTimeMillis();

    /**
     * Constructor to be given to a sender
     */
    public UniformReliableBroadcast(short myId, Map<Short, Host> hostsMap)
            throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.myId = myId;
        this.urbPending = ConcurrentHashMap.newKeySet(Constants.MAX_OUT_OF_ORDER_DELIVERY);
        this.urbDelivered = new ConcurrentLowMemoryMsgSet<>(hostsMap);
        this.urbAck = new ConcurrentHashMap<>(2 * Constants.MAX_OUT_OF_ORDER_DELIVERY);
        this.hostsMap = hostsMap;
        this.beb = new BestEffortBroadcast(myId, hostsMap);
        this.urbToBroadcast = new ConcurrentLinkedQueue<>();
        this.type = ActorType.SENDER;
        this.halfHosts = hostsMap.size() / 2;
        this.parent = null;
        this.urbToDeliver = null;
    }

    /**
     * Constructor for an Urb to be given to a receiver
     */
    public UniformReliableBroadcast(short myId, Map<Short, Host> hostsMap,
            Deliverable parent, PlState plState, UrbSate state) {
        if (parent == null || hostsMap == null || plState == null || state == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have argument to constructor");
        }
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.beb = new BestEffortBroadcast(myId, hostsMap, this, plState);
        this.urbPending = state.getUrbPending();
        this.parent = parent;
        this.urbDelivered = state.getUrbDelivered();
        this.urbAck = state.getUrbAck();
        this.urbToBroadcast = state.getUrbToBroadcast();
        this.urbToDeliver = new ConcurrentLinkedQueue<>();
        this.type = ActorType.RECEIVER;
        this.halfHosts = hostsMap.size() / 2;
    }

    public void broadcast(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot broadcast a null message");
        }
        if (type != ActorType.SENDER) {
            throw new IllegalStateException("Only a sender can broadcast message");
        }
        urbPending.add(m);
        urbToBroadcast.add(m);
    }

    @Override
    public void deliver(Message m) {
        if (type != ActorType.RECEIVER) {
            throw new IllegalStateException("Sender cannot deliver messages");
        }
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }

        urbAck.putIfAbsent(m, ConcurrentHashMap.newKeySet(hostsMap.size()));

        urbAck.get(m).add(m.getSenderId());
        if (!urbPending.contains(m)) {
            urbPending.add(m);
            // the receiver should not re-broadcast the message itself in order to not block
            // and be able to deliver next message
            urbToBroadcast.add(m.withUpdatedSender(myId));
        }
        urbToDeliver.add(m);
    }

    @Override
    public void run() {
        try {
            beb.startPl();
            if (type == ActorType.SENDER) {
                while (true) {
                    Message m = urbToBroadcast.poll();
                    if (m == null) {
                        Thread.sleep(Constants.URB_SLEEP_BEFORE_NEXT_POLL);
                    } else {
                        beb.broadcast(m);
                    }
                    if ((System.currentTimeMillis() - previousFlush) > Constants.TIME_BEFORE_FLUSH) {
                        tryFlushPendingAndAck();
                        previousFlush = System.currentTimeMillis();
                    }
                }
            } else if (type == ActorType.RECEIVER) {
                while (true) {
                    Message m = urbToDeliver.poll();
                    if (m != null) {
                        if (!urbDelivered.contains(m) && !tryDeliver(m)) {
                            urbToRetryDeliver.add(m);
                        }
                        if ((System.currentTimeMillis() - previousRetryFlush) > Constants.URB_SLEEP_BEFORE_NEXT_POLL) {
                            while ((m = urbToRetryDeliver.poll()) != null) {
                                if (!tryDeliver(m)) {
                                    urbToDeliver.add(m);
                                }
                            }
                            previousFlush = System.currentTimeMillis();
                        }
                    } else {
                        Thread.sleep(Constants.URB_SLEEP_BEFORE_NEXT_POLL);
                    }
                }
            } else {
                throw new IllegalStateException("Unhandled ActorType");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted sender PL");
            e.printStackTrace();
            beb.interruptPl();
            return;
        }
    }

    @Override
    public UrbSate getUrbState() {
        return new UrbSate(urbPending, urbDelivered, urbAck, urbToBroadcast);
    }

    @Override
    public PlState getPlState() {
        return beb.getPlState();
    }

    @Override
    public void flush(short host, int deliveredUntil) {
        urbDelivered.flush(host, deliveredUntil);
        beb.flush(host, deliveredUntil);
    }

    private synchronized boolean tryDeliver(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }
        if (!urbDelivered.contains(m) && urbPending.contains(m) && urbAck.containsKey(m)
                && urbAck.get(m).size() > halfHosts) {
            urbDelivered.add(m);
            parent.deliver(m);
            return true;
        }
        return false;
    }

    private synchronized void tryFlushPendingAndAck() {
        urbPending.removeIf(m -> urbDelivered.contains(m));
        urbAck.keySet().removeIf(m -> urbDelivered.contains(m));
    }

}

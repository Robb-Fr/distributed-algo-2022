package cs451.Broadcasts;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.Messages.Message;
import cs451.Messages.Message.PayloadType;
import cs451.States.PlState;
import cs451.States.PlStateGiver;
import cs451.States.UrbSate;
import cs451.States.UrbStateGiver;
import cs451.Constants;

public class FifoUniformReliableBroadcast implements Deliverable, PlStateGiver, UrbStateGiver {
    private final UniformReliableBroadcast urb;
    private AtomicInteger lsn = new AtomicInteger(0);
    private final int nbHosts;
    private final Deliverable parent;
    private final ConcurrentHashMap<Short, AtomicInteger> next;
    private final ConcurrentHashMap.KeySetView<Message, Boolean> fifoPending;
    private long previousFlush = System.currentTimeMillis();
    private final ActorType type;
    private Thread urbThread = null;

    /**
     * Constructor for a sender
     */
    public FifoUniformReliableBroadcast(short myId, Map<Short, Host> hostsMap)
            throws SocketException, UnknownHostException {
        this.urb = new UniformReliableBroadcast(myId, hostsMap);
        this.next = new ConcurrentHashMap<>(hostsMap.size());
        for (short host : hostsMap.keySet()) {
            this.next.put(host, new AtomicInteger(1));
        }
        this.type = ActorType.SENDER;
        this.nbHosts = hostsMap.size();
        this.fifoPending = null;
        this.parent = null;
    }

    /**
     * Constructor for a receiver
     * 
     */
    public FifoUniformReliableBroadcast(short myId, Map<Short, Host> hostsMap, Deliverable parent, PlState plState,
            UrbSate urbState, ConcurrentHashMap<Short, AtomicInteger> next) {
        this.urb = new UniformReliableBroadcast(myId, hostsMap, this, plState, urbState);
        this.fifoPending = ConcurrentHashMap.newKeySet(hostsMap.size() * Constants.MAX_OUT_OF_ORDER_DELIVERY);
        this.next = next;
        this.parent = parent;
        this.type = ActorType.RECEIVER;
        this.nbHosts = hostsMap.size();
    }

    public void broadcast(Message m) throws InterruptedException {
        if (m == null) {
            throw new IllegalArgumentException("Cannot broadcast a null message");
        }
        if (type != ActorType.SENDER) {
            throw new IllegalStateException("Only a sender can broadcast messages");
        }
        lsn.getAndSet(m.getId());
        while ((next.reduceValues(nbHosts,
                val -> lsn.get() > val.get() + Constants.MAX_OUT_OF_ORDER_DELIVERY ? 0 : 1,
                (val, acc) -> val + acc)) <= nbHosts / 2) {
            Thread.sleep(Constants.FIFO_SLEEP_BEFORE_RESEND);
        }
        urb.broadcast(m);
        System.out.println("Fifo Broadcast " + m);
    }

    public void startUrb() {
        urbThread = new Thread(urb, "URB " + type);
        urbThread.start();
    }

    public void interruptUrb() {
        if (urbThread != null) {
            urbThread.interrupt();
        }
    }

    public ConcurrentHashMap<Short, AtomicInteger> getFifoNext() {
        return next;
    }

    @Override
    public synchronized void deliver(Message m) {
        if (m == null) {
            System.err.println("Cannot deliver a null message");
        }
        if (type != ActorType.RECEIVER) {
            throw new IllegalStateException("Only a receiver can deliver messages");
        }
        fifoPending.add(m);
        short source = m.getSourceId();
        Message nextMsg = null;
        AtomicInteger nextForSource = next.get(source);
        while (fifoPending.remove(nextMsg = m.withUpdatedId(nextForSource.get()))) {
            nextForSource.getAndIncrement();
            parent.deliver(nextMsg);
        }
        // System.out.println("FIFO pending " + fifoPending);
        if ((System.currentTimeMillis() - previousFlush) > Constants.TIME_BEFORE_FLUSH) {
            int newId = Integer.max(0,
                    nextForSource.get() - 2);
            urb.flush(source, new Message(0, source, m.getSenderId(), PayloadType.CONTENT),
                    new Message(newId, source, m.getSenderId(), PayloadType.CONTENT));
            previousFlush = System.currentTimeMillis();
        }
    }

    @Override
    public UrbSate getUrbState() {
        return urb.getUrbState();
    }

    @Override
    public PlState getPlState() {
        return urb.getPlState();
    }
}

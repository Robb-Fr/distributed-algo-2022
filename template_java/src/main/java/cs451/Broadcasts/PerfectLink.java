package cs451.Broadcasts;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import cs451.Host;
import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;
import cs451.Messages.MessageToBeSent;
import cs451.Messages.MessageTupleWithSender;
import cs451.States.PlState;
import cs451.States.PlStateGiver;
import cs451.Constants;

import java.net.DatagramSocket;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;

public class PerfectLink implements Closeable, PlStateGiver, Runnable, Flushable {
    private final AtomicReference<DatagramSocket> socket;
    private final Host thisHost;
    private final Map<Short, Host> hostsMap;
    private final Deliverable parent;
    private final ConcurrentLowMemoryMsgSet<MessageTupleWithSender> delivered;
    private final ConcurrentLowMemoryMsgSet<MessageTupleWithSender> plAcked;
    private final ConcurrentLinkedQueue<MessageToBeSent> toSend;
    private final ConcurrentLinkedQueue<MessageToBeSent> toRetry;
    private final ActorType type;
    private long previousFlush = System.currentTimeMillis();
    private long TIMEOUT_BEFORE_RETRY;
    private final int MAX_ON_THE_FLY;

    /**
     * Constructor for a perfect link belonging to a sender
     */
    public PerfectLink(short myId, Map<Short, Host> hostsMap) throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.hostsMap = hostsMap;
        this.thisHost = this.hostsMap.get(myId);
        InetAddress thisHostIp = InetAddress.getByName(this.thisHost.getIp());
        this.socket = new AtomicReference<DatagramSocket>(new DatagramSocket(thisHost.getPort(), thisHostIp));
        this.socket.get().setSoTimeout(Constants.SOCKET_TIMEOUT);
        this.plAcked = new ConcurrentLowMemoryMsgSet<>(hostsMap);
        this.type = ActorType.SENDER;
        this.toSend = new ConcurrentLinkedQueue<>();
        this.toRetry = new ConcurrentLinkedQueue<>();
        int sleep_val = Constants.PL_SLEEP_BEFORE_RESEND + (hostsMap.size() / Constants.THRESHOLD_NB_HOST_FOR_BACK_OFF);
        this.TIMEOUT_BEFORE_RETRY = sleep_val * sleep_val;
        this.MAX_ON_THE_FLY = 2 * Constants.MAX_OUT_OF_ORDER_DELIVERY * Constants.MAX_OUT_OF_ORDER_DELIVERY
                * hostsMap.size()
                * hostsMap.size();
        this.parent = null;
        this.delivered = null;
    }

    /**
     * Constructor for a perfect link belonging to a receiver
     */
    public PerfectLink(short myId, Map<Short, Host> hostsMap, Deliverable parent,
            PlState state) {
        if (parent == null || hostsMap == null || state == null) {
            throw new IllegalArgumentException(
                    "Cannot have null arguments to constructor");
        }
        this.hostsMap = hostsMap;
        this.thisHost = this.hostsMap.get(myId);
        this.socket = state.getPlSocket();
        this.plAcked = state.getPlAcked();
        this.toSend = state.getToSend();
        this.delivered = new ConcurrentLowMemoryMsgSet<>(hostsMap);
        this.parent = parent;
        this.type = ActorType.RECEIVER;
        this.TIMEOUT_BEFORE_RETRY = -1;
        this.MAX_ON_THE_FLY = -1;
        this.toRetry = null;
    }

    /**
     * Sends the given message to the given host making sure the delivery is
     * "perfect"
     * Validity, No duplication, No Creation
     */
    public void addToSend(Message message, short dest) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot send with null arguments");
        }
        toSend.add(message.preparedForSending(dest));
    }

    @Override
    public void close() {
        socket.get().close();

    }

    @Override
    public void run() {
        try {
            if (type == ActorType.SENDER) {
                while (true) {
                    MessageToBeSent mToSend = toSend.poll();
                    if (mToSend == null) {
                        Thread.sleep(Constants.PL_SLEEP_BEFORE_RESEND);
                    } else {
                        Message m = mToSend.getMessage();
                        sendMessage(mToSend);
                        if (!m.isAck()) {
                            if (toRetry.size() > 4 * MAX_ON_THE_FLY) {
                                Thread.sleep(Constants.PL_SLEEP_BEFORE_RETRY);
                            }
                            toRetry.add(mToSend);
                        }
                        if ((System.currentTimeMillis() - previousFlush) > TIMEOUT_BEFORE_RETRY) {
                            int retried = 0;
                            while ((mToSend = toRetry.poll()) != null && retried < MAX_ON_THE_FLY) {
                                if (!plAcked.contains(mToSend.getAckForThisMessage())) {
                                    toSend.add(mToSend);
                                    retried++;
                                }
                            }
                            previousFlush = System.currentTimeMillis();
                            if (2 * retried >= MAX_ON_THE_FLY) {
                                TIMEOUT_BEFORE_RETRY <<= 2;
                                System.out.println("Changed Timeout to " + TIMEOUT_BEFORE_RETRY);
                            }
                        }
                    }
                }
            } else if (type == ActorType.RECEIVER) {
                while (true) {
                    receiveAndDeliver();
                }
            } else {
                throw new IllegalStateException("Unhandled ActorType");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted sender PL");
            e.printStackTrace();
            return;
        }
    }

    @Override
    public PlState getPlState() {
        return new PlState(socket, plAcked, toSend);
    }

    @Override
    public void flush(short host, int deliveredUntil) {
        delivered.flush(host, deliveredUntil);
        plAcked.flush(host, deliveredUntil);
    }

    /**
     * Implements the delivery of a perfect link to ensure the message is delivered
     * to the parent with the Validity, No duplication and No creation properties.
     */
    private void receiveAndDeliver() {
        if (type != ActorType.RECEIVER) {
            throw new IllegalStateException("Sender cannot deliver messages");
        }
        Message m = receiveMessage();
        if (m != null && !(delivered.contains(m.tupleWithSender())) && !m.isAck()) {
            parent.deliver(m);
            delivered.add(m.tupleWithSender());
        }
    }

    /**
     * Primitive for sending a message without the Validity property
     */
    private void sendMessage(MessageToBeSent message) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "Cannot send null message or message to null host");
        }
        Host dest = hostsMap.get(message.getDest());
        byte[] msgBytes = message.getSerializedMsg();
        DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, dest.getHostsSocket());
        try {
            socket.get().send(packet);
        } catch (IOException e) {
            System.err.println("Error while sending the message");
            e.printStackTrace();
        }
    }

    /**
     * Primitive for receiving a message and sending an ACK on correct reception but
     * without the Validity property. Can be used to receive ACK for a specific
     * message only, or return null for anything else received
     */
    private Message receiveMessage() {
        // we should only have sent packets not exceeding this size
        byte[] buf = new byte[Constants.SERIALIZED_MSG_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.get().receive(packet);
        } catch (IOException e) {
            // packet not received, report only the non Timeout error
            if (!(e instanceof SocketTimeoutException)) {
                System.err.println("Error while receiving the packet");
                e.printStackTrace();
            }
            return null;
        }
        Message m = Message.deserialize(packet.getData());
        if (m != null) {
            // we check we received an actual message
            if (m.isAck()) {
                plAcked.add(m.tupleWithSender());
            } else {
                // if the received message is not an ACK itself, we can send an ACK
                addToSend(m.ackForThisMessage(thisHost.getId()), m.getSenderId());
                return m;
            }
        }
        return null;
    }

}
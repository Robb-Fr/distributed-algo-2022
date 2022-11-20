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
import java.net.InetSocketAddress;

public class PerfectLink implements Closeable, PlStateGiver, Runnable, Flushable {
    private final AtomicReference<DatagramSocket> socket;
    private final Host thisHost;
    private final Map<Integer, Host> hostsMap;
    private final Deliverable parent;
    private final ConcurrentLowMemoryMsgSet<MessageTupleWithSender> delivered;
    private final ConcurrentLowMemoryMsgSet<MessageTupleWithSender> plAcked;
    private final ConcurrentLinkedQueue<MessageToBeSent> toSend;
    private final ActorType type;

    /**
     * Constructor for a perfect link belonging to a sender
     */
    public PerfectLink(int myId, Map<Integer, Host> hostsMap) throws SocketException, UnknownHostException {
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
        this.parent = null;
        this.delivered = null;
    }

    /**
     * Constructor for a perfect link belonging to a receiver
     */
    public PerfectLink(int myId, Map<Integer, Host> hostsMap, Deliverable parent,
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
    }

    /**
     * Sends the given message to the given host making sure the delivery is
     * "perfect"
     * Validity, No duplication, No Creation
     */
    public void addToSend(Message message, int dest) {
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
                        Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                    } else {
                        Message m = mToSend.getMessage();
                        int dest = mToSend.getDest();
                        if (m.isAck()) {
                            sendMessage(mToSend);
                        } else if (!plAcked.contains(m.ackForThisMessage(dest).tupleWithSender())) {
                            sendMessage(mToSend);
                            // we have not acked the message, we will have to re-check for it
                            toSend.add(mToSend);
                        }
                    }
                    Thread.sleep(Constants.SLEEP_BEFORE_RESEND);
                }
            } else if (type == ActorType.RECEIVER) {
                while (true) {
                    receiveAndDeliver();
                    Thread.sleep(Constants.SLEEP_BEFORE_RECEIVE);
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
    public void flush(Host host, int deliveredUntil) {
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
        InetSocketAddress socketDest = new InetSocketAddress(dest.getInetAddress(), dest.getPort());
        DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, socketDest);
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
        byte[] buf = new byte[Constants.MAX_DATAGRAM_LENGTH];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.get().receive(packet);
        } catch (IOException e) {
            // packet not received, report only the non Timeout error
            if (!(e instanceof SocketTimeoutException)) {
                System.err.println("Error while receiving the packet");
                e.printStackTrace();
            }
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
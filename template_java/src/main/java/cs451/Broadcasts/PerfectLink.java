package cs451.Broadcasts;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs451.Host;
import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;
import cs451.Messages.MessageToBeSent;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.PlState;
import cs451.States.PlStateGiver;
import cs451.Constants;

import java.net.DatagramSocket;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;

public class PerfectLink implements Closeable, PlStateGiver, Runnable {
    private final DatagramSocket socket;
    private final short myId;
    private final Map<Short, Host> hostsMap;
    private final ActorType type;
    private final Deliverable parent;
    private final ConcurrentLowMemoryMsgSet delivered;
    private final ConcurrentLowMemoryMsgSet acked;
    private final ConcurrentLinkedQueue<MessageToBeSent> toSend;
    private final ConcurrentLinkedQueue<MessageToBeSent> toRetry;
    private long timeoutBeforeResend = Constants.PL_TIMEOUT_BEFORE_RESEND;
    private final int vs;
    private final int ds;

    /**
     * Constructor for a perfect link belonging to a sender
     */
    public PerfectLink(short myId, Map<Short, Host> hostsMap, LatticeConfig config)
            throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.myId = myId;
        this.hostsMap = hostsMap;
        Host thisHost = this.hostsMap.get(myId);
        InetAddress thisHostIp = InetAddress.getByName(thisHost.getIp());
        this.socket = new DatagramSocket(thisHost.getPort(), thisHostIp);
        this.socket.setSoTimeout(Constants.SOCKET_TIMEOUT);
        this.acked = new ConcurrentLowMemoryMsgSet(config.getP(), config.getVs());
        this.type = ActorType.SENDER;
        this.toSend = new ConcurrentLinkedQueue<>();
        this.toRetry = new ConcurrentLinkedQueue<>();
        this.vs = config.getVs();
        this.ds = config.getDs();
        this.parent = null;
        this.delivered = null;
    }

    /**
     * Constructor for a perfect link belonging to a receiver
     */
    public PerfectLink(short myId, Map<Short, Host> hostsMap, Deliverable parent, LatticeConfig config,
            PlState state) {
        if (parent == null || hostsMap == null || config == null || state == null) {
            throw new IllegalArgumentException(
                    "Cannot have null arguments to constructor");
        }
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.socket = state.getPlSocket();
        this.acked = state.getPlAcked();
        this.toSend = state.getPlToSend();
        this.delivered = new ConcurrentLowMemoryMsgSet(config.getP(), config.getVs());
        this.parent = parent;
        this.type = ActorType.RECEIVER;
        this.vs = config.getVs();
        this.ds = config.getDs();
        this.toRetry = null;
    }

    public void addToSend(Message message, short dest) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot send with null arguments");
        }
        toSend.add(message.toSendTo(dest));
    }

    public void flush(int agreementId) {
        acked.flush(agreementId);
        delivered.flush(agreementId);
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void run() {
        try {
            if (type == ActorType.SENDER) {
                while (true) {
                    runSenderPl();
                }
            } else if (type == ActorType.RECEIVER) {
                while (true) {
                    runReceiverPl();
                }
            } else {
                throw new IllegalStateException("Unhandled ActorType");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted PL");
            e.printStackTrace();
            return;
        }
    }

    @Override
    public PlState getPlState() {
        return new PlState(socket, acked, toSend);
    }

    public void runSenderPl() throws InterruptedException {
        MessageToBeSent mToSend = toSend.poll();
        if (mToSend != null) {
            Message m = mToSend.getMessage();
            sendMessage(mToSend);
            mToSend.setTimeOfSending(System.currentTimeMillis());
            // we add to retry if not an ACK
            if (!m.isAck()) {
                toRetry.add(mToSend);
            }
            boolean retried = false;
            int toRetrySize = toRetry.size();
            for (int i = 0; i < toRetrySize; ++i) {
                mToSend = toRetry.poll();
                if ((System.currentTimeMillis() - mToSend.getTimeOfSending()) > timeoutBeforeResend) {
                    if (!acked.contains(mToSend.getMessage())) {
                        toSend.add(mToSend);
                        retried = true;
                    }
                } else {
                    toRetry.add(mToSend);
                }
            }
            if (retried) {
                timeoutBeforeResend <<= 1;
                System.out.println("Changed Timeout to " + timeoutBeforeResend);
            }
        } else {
            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        }
    }

    public void runReceiverPl() {
        receiveAndDeliver();
    }

    private void receiveAndDeliver() {
        if (type != ActorType.RECEIVER) {
            throw new IllegalStateException("Sender cannot deliver messages");
        }
        Message m = receiveMessage();
        if (m != null && !(delivered.contains(m)) && !m.isAck()) {
            parent.deliver(m);
            delivered.add(m);
        }
    }

    private void sendMessage(MessageToBeSent message) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "Cannot send null message or message to null host");
        }
        Host dest = hostsMap.get(message.getDest());
        byte[] msgBytes = message.getSerializedMsg();
        DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, dest.getHostsSocket());
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error while sending the message");
            e.printStackTrace();
        }
    }

    private Message receiveMessage() {
        // we should only have sent packets not exceeding this size
        byte[] buf = new byte[Constants.MSG_SIZE_NO_VALUES + Integer.BYTES * (vs + ds + 1)];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
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
                acked.add(m);
            } else {
                // if the received message is not an ACK itself, we can send an ACK
                addToSend(m.ack(myId), m.getSenderId());
                return m;
            }
        }
        return null;
    }

}
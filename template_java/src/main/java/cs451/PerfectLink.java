package cs451;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cs451.Message.PayloadType;

import java.net.DatagramSocket;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class PerfectLink implements Closeable {
    private final AtomicReference<DatagramSocket> socket;
    private final int myId;
    private final Host thisHost;
    private final Map<Integer, Host> hosts;
    private final Deliverable parent;
    private final Set<Message> delivered;

    /**
     * Constructor for a perfect link belonging to a receiver
     * 
     * @param thisHost
     * @param hostsMap
     * @param parent
     * @param socket
     */
    public PerfectLink(int myId, Map<Integer, Host> hostsMap, Deliverable parent,
            AtomicReference<DatagramSocket> socket) {
        if (parent == null || hostsMap == null || socket == null) {
            throw new IllegalArgumentException(
                    "A receiver cannot have null parent or a null self host or a null socket reference or null hosts");
        }
        this.myId = myId;
        this.hosts = hostsMap;
        this.thisHost = this.hosts.get(this.myId);
        this.socket = socket;
        this.parent = parent;
        this.delivered = new HashSet<>();
    }

    /**
     * Constructor for a perfect link belonging to a sender
     * 
     * @param thisHost
     * @param hosts
     * @throws SocketException
     * @throws UnknownHostException
     */
    public PerfectLink(int myId, Map<Integer, Host> hostsMap) throws SocketException, UnknownHostException {
        if (hostsMap == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.myId = myId;
        this.hosts = hostsMap;
        this.thisHost = this.hosts.get(this.myId);
        InetAddress thisHostIp = InetAddress.getByName(this.thisHost.getIp());
        this.socket = new AtomicReference<DatagramSocket>(new DatagramSocket(thisHost.getPort(), thisHostIp));
        this.socket.get().setSoTimeout(Constants.SOCKET_TIMEOUT);
        this.parent = null;
        this.delivered = null;
    }

    /**
     * Sends the given message to the given host making sure the delivery is
     * "perfect"
     * Validity, No duplication, No Creation
     * 
     * @param message : the message to be sent
     * @param dest    : the host that should receive the message
     * @throws InterruptedException
     */
    public void sendPerfect(Message message, Host dest) throws InterruptedException {
        if (dest == null || message == null) {
            throw new IllegalArgumentException(
                    "Cannot send null message or message to null host");
        }
        do {
            sendMessage(message, dest);
            Thread.sleep(Constants.SLEEP_BEFORE_RESEND);
        } while (receiveMessage(message.getId()) == null);
        System.out.println("Perfectly sent");
    }

    /**
     * Implements the delivery of a perfect link to ensure the message is delivered
     * to the parent with the Validity, No duplication and No creation properties.
     */
    public void receiveAndDeliver() {
        if (parent == null || delivered == null) {
            System.err.println("Cannot deliver for this perfect link as it belongs to sender");
        }
        Message m = receiveMessage(-1);
        if (m != null && !(delivered.contains(m)) && !m.isAck()) {
            parent.deliver(m);
            delivered.add(m);
            System.out.println("Delivered message : " + m);
        }
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.get().close();
        }

    }

    public AtomicReference<DatagramSocket> getSocket() {
        return socket;
    }

    /**
     * Primitive for sending a message without the Validity property
     * 
     * @param message
     * @param dest
     */
    private void sendMessage(Message message, Host dest) {
        if (dest == null || message == null) {
            throw new IllegalArgumentException(
                    "Cannot send null message or message to null host");
        }
        byte[] msgBytes = message.serialize();
        if (msgBytes == null) {
            return;
        }
        if (msgBytes.length > Constants.MAX_DATAGRAM_LENGTH) {
            throw new IndexOutOfBoundsException("Sent packet exceeds maximum accepted packet size");
        }
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
     * without the Validity property
     * 
     * @param ackExpectedForMsg : indicates for which message id we are trying to receive an
     *                  ACK for. If we just receive any message to be delivered we
     *                  indicate a negative value
     * @return : the received message if correctly received
     */
    private Message receiveMessage(int ackExpectedForMsg) {
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
            if (ackExpectedForMsg < 0) {
                // if the message is not expected to be an ACK
                if (!m.isAck()) {
                    // if the received message is not an ACK itself, we can send an ACK
                    sendMessage(new Message(m.getId(), thisHost.getId(), PayloadType.ACK), hosts.get(m.getSenderId()));
                }
            } else {
                // we expect specifically an ACK for the message with id ackForMsg
                if (!m.isAckForMsg(ackExpectedForMsg)) {
                    // the received message is not the ACK we expect
                    return null;
                }
            }
            // we return m if it is not expected to be an ack or if it is the ACK we
            // expected
            return m;
        }
        return null;
    }

}
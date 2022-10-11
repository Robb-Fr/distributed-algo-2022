package cs451;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cs451.Message.PayloadType;

import java.net.DatagramSocket;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class PerfectLink implements Closeable {
    private final DatagramSocket socket;
    private final Host thisHost;
    private final Map<Integer, Host> hosts;
    private final Deliverable parent;
    private final Set<Message> delivered;

    /**
     * Constructor for a perfect link belonging to a receiver
     * 
     * @param parent
     * @throws UnknownHostException
     * @throws SocketException
     */
    public PerfectLink(Host thisHost, Deliverable parent) throws UnknownHostException, SocketException {
        if (parent == null || thisHost == null) {
            throw new IllegalArgumentException("A receiver cannot have null parent or a null self host");
        }
        this.thisHost = thisHost;
        InetAddress thisHostIp = InetAddress.getByName(thisHost.getIp());
        this.socket = new DatagramSocket(thisHost.getPort(), thisHostIp);
        this.hosts = null;
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
    public PerfectLink(Host thisHost, Map<Integer, Host> hosts) throws SocketException, UnknownHostException {
        if (thisHost == null || hosts == null) {
            throw new IllegalArgumentException("A sender cannot have null self host or hosts map");
        }
        this.thisHost = thisHost;
        InetAddress thisHostIp = InetAddress.getByName(thisHost.getIp());
        this.socket = new DatagramSocket(thisHost.getPort(), thisHostIp);
        this.hosts = hosts;
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
        if (thisHost == null || hosts == null) {
            System.err.println("Cannot send through this perfect link as it belongs to a receiver");
        }
        Message m = null;
        do {
            sendMessage(message, dest);
            Thread.sleep(2);
        } while ((m = receiveMessage()) == null || !m.isAckForMsg(message.getId()));
    }

    /**
     * Implements the delivery of a perfect link to ensure the message is delivered
     * to the parent with the Validity, No duplication and No creation properties.
     */
    public void receiveAndDeliver() {
        if (parent == null || delivered == null) {
            System.err.println("Cannot deliver for this perfect link as it belongs to sender");
        }
        Message m = receiveMessage();
        if (m != null && !(delivered.contains(m)) && !m.isAck()) {
            parent.deliver(m);
            delivered.add(m);
        }
    }

    /**
     * Primitive for sending a message without the Validity property
     * 
     * @param message
     * @param dest
     */
    private void sendMessage(Message message, Host dest) {
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
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error while sending the message");
            e.printStackTrace();
        }
    }

    /**
     * Primitive for receiving a message and sending an ACK on correct reception but
     * without the Validity property
     * 
     * @return : the received message if correctly received
     */
    private Message receiveMessage() {
        // we should only have sent packets not exceeding this size
        byte[] buf = new byte[Constants.MAX_DATAGRAM_LENGTH];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            // packet not delivered
            System.err.println("Error while receiving the packet");
            e.printStackTrace();
        }
        Message m = Message.deserialize(packet.getData());
        if (m != null) {
            sendMessage(new Message(m.getId(), thisHost.getId(), PayloadType.ACK), hosts.get(m.getSenderId()));
        }
        return m;
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }

    }

}
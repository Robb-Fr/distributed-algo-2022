package cs451;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;

import cs451.Message.PayloadType;

import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class PerfectLink {
    private final DatagramSocket socket;
    private final Host thisHost;
    private final Deliverable parent;
    private final HashSet<Message> delivered;

    public PerfectLink(Deliverable parent, Host thisHost) throws UnknownHostException, SocketException {
        if (parent == null || thisHost == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        InetAddress thisHostIp = InetAddress.getByName(thisHost.getIp());
        this.thisHost = thisHost;
        this.socket = new DatagramSocket(thisHost.getPort(), thisHostIp);
        this.parent = parent;
        this.delivered = new HashSet<>();
    }

    public void sendPerfect(Message message, Host dest) throws UnknownHostException, IOException, InterruptedException {
        Message m = null;
        do {
            sendMessage(message, dest);
            Thread.sleep(2);
        } while ((m = receiveMessage()) == null || !m.isAckForMsg(message.getId()));
    }

    public void receiveAndDeliver() {
        Message m = receiveMessage();
        if (m != null && !(delivered.contains(m)) && !m.isAck()) {
            parent.deliver(m);
            delivered.add(m);
        }
    }

    private void sendMessage(Message message, Host dest) throws UnknownHostException, IOException {
        byte[] msgBytes = message.serialize();
        if (msgBytes.length > Constants.MAX_DATAGRAM_LENGTH) {
            throw new IndexOutOfBoundsException("Sent packet exceeds maximum accepted packet size");
        }
        InetSocketAddress socketDest = new InetSocketAddress(dest.getInetAddress(), dest.getPort());
        DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, socketDest);
        socket.send(packet);
    }

    private Message receiveMessage() {
        // we should only have sent packets not exceeding this size
        byte[] buf = new byte[Constants.MAX_DATAGRAM_LENGTH];
        DatagramPacket packet = new DatagramPacket(buf, Constants.MAX_DATAGRAM_LENGTH);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            // packet not delivered
            e.printStackTrace();
            return null;
        }
        Message m = null;
        try {
            m = Message.deserialize(packet.getData());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Unable to deserialize the received message");
            return null;
        }
        if (m != null) {
            try {
                sendMessage(new Message(m.getId(), thisHost, PayloadType.ACK), m.getSender());
            } catch (IOException e) {
                // failed send ACK, not a problem, we will retry when it is resent
            }
        }

        return m;
    }

}
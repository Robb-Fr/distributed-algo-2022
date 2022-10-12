package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cs451.ConfigParser.PerfectLinkConfig;

public class Receiver implements Deliverable, Runnable {

    private final AtomicReference<StringBuilder> logBuilder;
    private final int selfId;
    private final ConfigParser configParser;
    private final PerfectLink link;

    /**
     * @param logBuilder
     * @param selfId
     * @param selfHost
     * @param hostsMap
     * @param configParser
     * @param socket : passes the socket built by the sender in AtomicReference
     * @throws UnknownHostException
     * @throws SocketException
     */
    public Receiver(AtomicReference<StringBuilder> logBuilder, int selfId, Host selfHost, Map<Integer, Host> hostsMap,
            ConfigParser configParser,
            AtomicReference<DatagramSocket> socket)
            throws UnknownHostException, SocketException {
        this.logBuilder = logBuilder;
        this.selfId = selfId;
        this.configParser = configParser;
        if (selfHost == null) {
            throw new IllegalArgumentException("Hosts list does not contain an host with this host's id");
        }
        this.link = new PerfectLink(selfHost, hostsMap, this, socket);
    }

    @Override
    public void deliver(Message m) {
        if (m != null) {
            logBuilder.getAndUpdate(s -> s.append("d " + m.getSenderId() + " " + m.getId() + "\n"));
        }

    }

    @Override
    public void run() {
        try {
            runPerfectLink();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void runPerfectLink() throws InterruptedException {
        PerfectLinkConfig plConf = configParser.getPerfectLinkConfig();
        if (plConf == null) {
            System.err.println("Could not read the perfect link config");
            return;
        }
        if (plConf.getReceiverId() != selfId) {
            System.out.println("I am not the receiver, no need to receive");
            return;
        } else {
            System.out.println("I am the receiver, here we go receiving");
            while (true) {
                link.receiveAndDeliver();
                Thread.sleep(Constants.SLEEP_BEFORE_RECEIVE);
            }
        }
    }

}

package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cs451.ConfigParser.PerfectLinkConfig;

public class Receiver implements Deliverable, Runnable {
    private final AtomicReference<StringBuilder> logBuilder;
    private final int myId;
    private final ConfigParser configParser;
    private final PerfectLink link;

    public Receiver(AtomicReference<StringBuilder> logBuilder, int myId, Map<Integer, Host> hostsMap,
            ConfigParser configParser,
            AtomicReference<DatagramSocket> socket)
            throws UnknownHostException, SocketException {
        this.logBuilder = logBuilder;
        this.myId = myId;
        this.configParser = configParser;
        this.link = new PerfectLink(myId, hostsMap, this, socket);
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
        if (plConf.getReceiverId() != myId) {
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

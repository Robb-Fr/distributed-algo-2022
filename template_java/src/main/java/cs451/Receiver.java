package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;

import cs451.ConfigParser.PerfectLinkConfig;

public class Receiver implements Deliverable, Runnable {

    private final AtomicReference<StringBuilder> logBuilder;
    private final int selfId;
    private final ConfigParser configParser;
    private final PerfectLink link;

    public Receiver(AtomicReference<StringBuilder> logBuilder, int selfId, Host selfHost, ConfigParser configParser)
            throws UnknownHostException, SocketException {
        this.logBuilder = logBuilder;
        this.selfId = selfId;
        this.configParser = configParser;
        if (selfHost == null) {
            throw new IllegalArgumentException("Hosts list does not contain an host with this host's id");
        }
        this.link = new PerfectLink(selfHost, this);
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
        } finally {
            link.close();

        }

    }

    private void runPerfectLink() throws InterruptedException {
        PerfectLinkConfig plConf = configParser.getPerfectLinkConfig();
        if (plConf == null) {
            System.err.println("Could not read the perfect link config");
            return;
        }
        System.out.println("Perfect link run config found : " + plConf);
        if (plConf.getReceiverId() == selfId) {
            return;
        } else {
            while (true) {
                link.receiveAndDeliver();
                Thread.sleep(1);
            }
        }
    }

}

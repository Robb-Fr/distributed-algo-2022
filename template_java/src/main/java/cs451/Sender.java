package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cs451.ConfigParser.PerfectLinkConfig;
import cs451.Message.PayloadType;

public class Sender implements Runnable {
    private final AtomicReference<StringBuilder> logBuilder;
    private final Host self;
    private final Map<Integer, Host> hostsMap;
    private final ConfigParser configParser;
    private final PerfectLink link;

    public Sender(AtomicReference<StringBuilder> logBuilder, int selfId, Map<Integer, Host> hostsMap,
            ConfigParser config)
            throws UnknownHostException, SocketException {
        this.logBuilder = logBuilder;
        this.configParser = config;
        Host selfHost = hostsMap.get(selfId);
        if (selfHost == null) {
            throw new IllegalArgumentException("Hosts list does not contain an host with this host's id");
        }
        this.self = selfHost;
        this.hostsMap = hostsMap;
        this.link = new PerfectLink(self, hostsMap);

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
        if (self.getId() == plConf.getReceiverId()) {
            // we are the receiver in this run, we have nothing to send
            return;
        } else {
            // we are a sender
            int nbMessages = plConf.getNbMessages();
            Host dest = hostsMap.get(plConf.getReceiverId());
            for (int i = 0; i < nbMessages; ++i) {
                Message m = new Message(i, this.self.getId(), PayloadType.CONTENT);
                link.sendPerfect(m, dest);
                logBuilder.getAndUpdate(s -> s.append("b " + m.getId() + "\n"));
            }
        }
    }

}

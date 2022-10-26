package cs451;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cs451.ConfigParser.PerfectLinkConfig;
import cs451.Message.PayloadType;

public class Sender implements Runnable {
    private final AtomicReference<StringBuilder> logBuilder;
    private final int myId;
    private final Map<Integer, Host> hostsMap;
    private final ConfigParser configParser;
    private final PerfectLink link;


    public Sender(AtomicReference<StringBuilder> logBuilder, int myId, Map<Integer, Host> hostsMap,
            ConfigParser config)
            throws UnknownHostException, SocketException {
        this.myId = myId;
        this.logBuilder = logBuilder;
        this.configParser = config;
        this.hostsMap = hostsMap;
        this.link = new PerfectLink(myId, hostsMap);
    }

    /**
     * @return : reference to the socket in the link
     */
    public AtomicReference<DatagramSocket> getSocket() {
        return link.getSocket();
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
        if (myId == plConf.getReceiverId()) {
            System.out.println("I am not a sender, no need to send anything");
            // we are the receiver in this run, we have nothing to send
            return;
        } else {
            System.out.println("I am a sender, here we go sending");
            // we are a sender
            int nbMessages = plConf.getNbMessages();
            Host dest = hostsMap.get(plConf.getReceiverId());
            for (int i = 1; i <= nbMessages; ++i) {
                Message m = new Message(i, myId, PayloadType.CONTENT);
                link.sendPerfect(m, dest);
                logBuilder.getAndUpdate(s -> s.append("b " + m.getId() + "\n"));
            }
        }
        System.out.println("Finished sending messages !");
    }

}

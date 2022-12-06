package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import cs451.Broadcasts.Deliverable;
import cs451.Messages.LogsBuilder;
import cs451.Messages.Message;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.PlState;

public class Receiver implements Deliverable, Runnable {
    private final LogsBuilder logsBuilder;
    private final LatticeConfig latticeConfig;
    private final ConcurrentLinkedQueue<Message> toDeliver = new ConcurrentLinkedQueue<>();

    public Receiver(String output, LogsBuilder logsBuilder, short myId, Map<Short, Host> hostsMap,
            ConfigParser configParser, PlState plState)
            throws UnknownHostException, SocketException {
        this.logsBuilder = logsBuilder;
        this.latticeConfig = configParser.getLatticeConfig();
    }

    @Override
    public void deliver(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }
        toDeliver.add(m);
    }

    @Override
    public void run() {
        System.err.println("Not implemented !");
    }

}

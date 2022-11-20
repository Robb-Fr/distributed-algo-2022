package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Broadcasts.FifoUniformReliableBroadcast;
import cs451.Messages.LogsBuilder;
import cs451.Messages.Message;
import cs451.Messages.Message.PayloadType;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.FifoConfig;
import cs451.States.PlState;
import cs451.States.PlStateGiver;
import cs451.States.UrbSate;
import cs451.States.UrbStateGiver;

public class Sender implements Runnable, UrbStateGiver, PlStateGiver {
    private final LogsBuilder logsBuilder;
    private final int myId;
    private final ConfigParser configParser;
    private final FifoUniformReliableBroadcast fifo;

    public Sender(LogsBuilder logsBuilder, int myId, Map<Integer, Host> hostsMap,
            ConfigParser config)
            throws UnknownHostException, SocketException {
        this.myId = myId;
        this.logsBuilder = logsBuilder;
        this.configParser = config;
        this.fifo = new FifoUniformReliableBroadcast(myId, hostsMap);
    }

    public ConcurrentHashMap<Host, AtomicInteger> getFifoNext() {
        return fifo.getFifoNext();
    }

    @Override
    public void run() {
        try {
            runFifoUniformReliableBroadcast();
        } catch (InterruptedException e) {
            System.err.println("Interrupted sender");
            e.printStackTrace();
            fifo.interruptUrb();
        }
    }

    private void runFifoUniformReliableBroadcast() throws InterruptedException {
        FifoConfig fifoConf = configParser.getFifoConfig();
        if (fifoConf == null) {
            System.err.println("Could not read the fifo config");
            return;
        }
        fifo.startUrb();
        for (int i = 1; i <= fifoConf.getNbMessages(); i++) {
            Message m = new Message(i, myId, PayloadType.CONTENT);
            fifo.broadcast(m);
            logsBuilder.log("b " + m.getId() + "\n");
        }
    }

    @Override
    public PlState getPlState() {
        return fifo.getPlState();
    }

    @Override
    public UrbSate getUrbState() {
        return fifo.getUrbState();
    }
}

package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Broadcasts.Deliverable;
import cs451.Broadcasts.FifoUniformReliableBroadcast;
import cs451.Messages.LogsBuilder;
import cs451.Messages.Message;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.FifoConfig;
import cs451.States.PlState;
import cs451.States.UrbSate;

public class Receiver implements Deliverable, Runnable {
    private final LogsBuilder logsBuilder;
    private final ConfigParser configParser;
    private final FifoUniformReliableBroadcast fifo;
    private final ConcurrentLinkedQueue<Message> toDeliver = new ConcurrentLinkedQueue<>();

    public Receiver(String output, LogsBuilder logsBuilder, int myId, Map<Integer, Host> hostsMap,
            ConfigParser configParser, PlState plState, UrbSate urbState,
            ConcurrentHashMap<Host, AtomicInteger> fifoNext)
            throws UnknownHostException, SocketException {
        this.logsBuilder = logsBuilder;
        this.configParser = configParser;
        this.fifo = new FifoUniformReliableBroadcast(myId, hostsMap, this, plState, urbState, fifoNext);
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
        try {
            runFifoUniformReliableBroadcast();
        } catch (InterruptedException e) {
            System.err.println("Interrupted receiver");
            e.printStackTrace();
            fifo.interruptUrb();
            return;
        }
    }

    private void runFifoUniformReliableBroadcast() throws InterruptedException {
        FifoConfig fifoConf = configParser.getFifoConfig();
        if (fifoConf == null) {
            System.err.println("Could not read the fifo config");
            return;
        }
        fifo.startUrb();
        while (true) {
            Message m = toDeliver.poll();
            if (m == null) {
                Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
            } else {
                logsBuilder.log("d " + m.getSourceId() + " " + m.getId() + "\n");
            }
            logsBuilder.tryFlush(false);
            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        }
    }

}

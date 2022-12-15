package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Broadcasts.LatticeAgreement;
import cs451.Messages.LogsBuilder;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.LatticeState;
import cs451.States.PlState;

public class Receiver implements Runnable {
    private final LogsBuilder logsBuilder;
    private final LatticeConfig latticeConfig;
    private final ConcurrentSkipListMap<Integer, Set<Integer>> toDeliver = new ConcurrentSkipListMap<>();
    private final LatticeAgreement latticeAgreement;
    private final AtomicInteger nextToDeliver = new AtomicInteger(0);

    public Receiver(LogsBuilder logsBuilder, short myId, Map<Short, Host> hostsMap,
            ConfigParser configParser, PlState plState, LatticeState latticeState)
            throws UnknownHostException, SocketException {
        this.logsBuilder = logsBuilder;
        this.latticeConfig = configParser.getLatticeConfig();
        this.latticeAgreement = new LatticeAgreement(myId, hostsMap, this, latticeConfig, latticeState, plState);
    }

    public Receiver() {
        logsBuilder = null;
        latticeConfig = null;
        latticeAgreement = null;
    }

    public void deliver(int agreementId, Set<Integer> values) {
        if (values == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }
        toDeliver.put(agreementId, Collections.unmodifiableSet(values));
    }

    @Override
    public void run() {
        Thread latticeReceiver = new Thread(latticeAgreement, "Lattice Receiver");
        latticeReceiver.start();
        try {
            while (true) {
                int next = nextToDeliver.get();
                if (toDeliver.containsKey(next)) {
                    StringBuilder logLine = new StringBuilder("");
                    for (int v : toDeliver.get(next)) {
                        logLine.append(v + " ");
                    }
                    logLine.append("\n");
                    logsBuilder.log(logLine.toString());
                    nextToDeliver.incrementAndGet();
                    toDeliver.remove(next);
                } else {
                    Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                }
                logsBuilder.tryFlush(false);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted receiver while sleeping");
            latticeReceiver.interrupt();
            e.printStackTrace();
        }
    }

}

package cs451;

import java.util.Map;
import cs451.Messages.LogsBuilder;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.PlState;
import cs451.States.PlStateGiver;

public class Sender implements Runnable, PlStateGiver {
    private final LogsBuilder logsBuilder;
    private final short myId;
    private final LatticeConfig latticeConfig;

    public Sender(LogsBuilder logsBuilder, short myId, Map<Short, Host> hostsMap,
            ConfigParser config) {
        this.myId = myId;
        this.logsBuilder = logsBuilder;
        this.latticeConfig = config.getLatticeConfig();
    }

    @Override
    public void run() {
        System.err.println("Not implemented !");
    }

    @Override
    public PlState getPlState() {
        return null;
    }
}

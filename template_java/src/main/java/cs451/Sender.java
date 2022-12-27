package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs451.Broadcasts.LatticeAgreement;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.LatticeState;
import cs451.States.LatticeStateGiver;
import cs451.States.PlState;
import cs451.States.PlStateGiver;

public class Sender implements Runnable, PlStateGiver, LatticeStateGiver {
    private final LatticeConfig latticeConfig;
    private final LatticeAgreement latticeAgreement;

    public Sender(short myId, Map<Short, Host> hostsMap,
            ConfigParser config) throws SocketException, UnknownHostException {
        this.latticeConfig = config.getLatticeConfig();
        this.latticeAgreement = new LatticeAgreement(myId, hostsMap, latticeConfig);
    }

    @Override
    public void run() {
        Thread latticeSender = new Thread(latticeAgreement, "Lattice Receiver");
        latticeSender.start();
        try {
            List<Set<Integer>> proposals = latticeConfig.getProposals();
            for (int i = 0; i < proposals.size(); ++i) {
                Set<Integer> proposal = proposals.get(i);
                while (!latticeAgreement.propose(i, proposal)) {
                    Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted sender while sleeping");
            latticeSender.interrupt();
            e.printStackTrace();
        }
    }

    @Override
    public PlState getPlState() {
        return latticeAgreement.getPlState();
    }

    @Override
    public LatticeState getLatticeState() {
        return latticeAgreement.getLatticeState();
    }
}

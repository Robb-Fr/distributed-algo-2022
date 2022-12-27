package cs451;

import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.junit.Test;

import cs451.Broadcasts.LatticeAgreement;
import cs451.Parsers.Parser;
import cs451.Parsers.ConfigParser.LatticeConfig;

public class LatticeStateTest {
    final List<LatticeAgreement> latticeSenders;
    final List<LatticeAgreement> latticeReceivers;
    final List<FakeReceiver> receivers;
    final List<LatticeConfig> configs;
    int p;
    int vs;
    int ds;

    public class FakeReceiver extends Receiver {
        Map<Integer, Set<Integer>> delivered = new TreeMap<>();

        @Override
        public void deliver(int agreementId, Set<Integer> values) {
            delivered.put(agreementId, new TreeSet<>(Set.copyOf(values)));
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            for (Entry<Integer, Set<Integer>> d : delivered.entrySet()) {
                s.append(" " + d.getKey() + " [ ");
                for (int e : d.getValue()) {
                    s.append(e + " ");
                }
                s.append("] ");
            }
            return "FakeReceiver [delivered=" + s.toString() + "]";
        }
    }

    public LatticeStateTest() {
        String[][] argsArray = new String[][] {
                "--id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/lattice-agreement-1.config"
                        .split(" "),
                "--id 2 --hosts ../example/hosts --output ../example/output/2.output ../example/configs/lattice-agreement-2.config"
                        .split(" "),
                "--id 3 --hosts ../example/hosts --output ../example/output/3.output ../example/configs/lattice-agreement-3.config"
                        .split(" ") };

        latticeSenders = new ArrayList<>(3);
        latticeReceivers = new ArrayList<>(3);
        receivers = new ArrayList<>(3);
        configs = new ArrayList<>(3);
        for (String[] args : argsArray) {
            Parser parser = new Parser(args);
            parser.parse();
            Map<Short, Host> hostsMap = parser.hostsMap();
            short myId = parser.myId();
            LatticeConfig config = parser.configParser().getLatticeConfig();
            configs.add(config);
            p = config.getP();
            vs = config.getVs();
            ds = config.getDs();
            try {
                LatticeAgreement latticeSender = new LatticeAgreement(myId, hostsMap, config);
                latticeSenders.add(latticeSender);
                FakeReceiver receiver = new FakeReceiver();
                receivers.add(receiver);
                latticeReceivers.add(
                        new LatticeAgreement(myId, hostsMap, receiver, config, latticeSender.getLatticeState(),
                                latticeSender.getPlState()));
            } catch (SocketException | UnknownHostException e) {
                System.err.println("Failed to generate new sender");
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testRunningAgreement() {
        for (int host = 0; host < 3; ++host) {
            final int hostId = host + 1;
            new Thread(latticeSenders.get(host), "Host " + hostId + " LAT Sender").start();
            new Thread(latticeReceivers.get(host), "Host " + hostId + " LAT Receiver").start();

        }
        for (int agreement = 0; agreement < p; ++agreement) {
            for (int host = 0; host < 3; ++host) {
                while (!latticeSenders.get(host).propose(agreement, configs.get(host).getProposals().get(agreement))) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted during test");
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.err.println("Test interrupted while sleeping");
            e.printStackTrace();
        }
        receivers.forEach(r -> System.out.println("Received " + r.delivered));
        for (int agreement = 0; agreement < p; ++agreement) {
            Set<Integer> unionOfProposed = new TreeSet<>();
            List<Set<Integer>> hostsDeliveredList = new ArrayList<>();
            for (int host = 0; host < 3; ++host) {
                unionOfProposed.addAll(configs.get(host).getProposals().get(agreement));
                hostsDeliveredList.add(receivers.get(host).delivered.get(agreement));
            }
            for (int host = 0; host < 3; ++host) {
                Set<Integer> hostDelivered = receivers.get(host).delivered.get(agreement);
                Set<Integer> hostProposed = configs.get(host).getProposals().get(agreement);
                // checks validity property
                assertTrue(hostDelivered.containsAll(hostProposed));
                assertTrue(unionOfProposed.containsAll(hostDelivered));

                // checks consistency property
                for (int otherHosts = 0; otherHosts < 3; ++otherHosts) {
                    assertTrue(hostDelivered.containsAll(hostsDeliveredList.get(otherHosts))
                            || hostsDeliveredList.get(otherHosts).containsAll(hostDelivered));
                }
            }
        }
    }
}

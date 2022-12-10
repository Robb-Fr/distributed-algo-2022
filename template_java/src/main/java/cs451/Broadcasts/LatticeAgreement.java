package cs451.Broadcasts;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Constants;
import cs451.Host;
import cs451.Receiver;
import cs451.Messages.Message;
import cs451.Messages.Message.EchoAck;
import cs451.Messages.Message.PayloadType;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.AgreementState;
import cs451.States.LatticeState;
import cs451.States.LatticeStateGiver;
import cs451.States.PlState;
import cs451.States.PlStateGiver;

public class LatticeAgreement implements PlStateGiver, LatticeStateGiver, Deliverable, Runnable {
    private final short myId;
    private final Map<Short, Host> hostsMap;
    private final ConcurrentHashMap<Integer, AgreementState> agreements;
    private final ConcurrentLinkedQueue<Message> toBroadcast;
    private final ConcurrentLinkedQueue<Integer> toDeliver;
    private final AtomicInteger windowPosition;
    private final PerfectLink pl;
    private final ActorType type;
    private final Receiver parent;

    public LatticeAgreement(short myId, Map<Short, Host> hostsMap, LatticeConfig config)
            throws SocketException, UnknownHostException {
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.agreements = new ConcurrentHashMap<>(2 * Constants.MAX_OUT_OF_ORDER_DELIVERY);
        this.windowPosition = new AtomicInteger(0);
        this.toBroadcast = new ConcurrentLinkedQueue<>();
        this.pl = new PerfectLink(myId, hostsMap, config);
        this.type = ActorType.SENDER;
        this.toDeliver = new ConcurrentLinkedQueue<>();
        this.parent = null;
    }

    public LatticeAgreement(short myId, Map<Short, Host> hostsMap, Receiver parent, LatticeConfig config,
            LatticeState latticeState,
            PlState plState) {
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.agreements = latticeState.getAgreements();
        this.windowPosition = latticeState.getWindowPosition();
        this.toBroadcast = latticeState.getToBroadcast();
        this.toDeliver = latticeState.getToDeliver();
        this.pl = new PerfectLink(myId, hostsMap, this, config, plState);
        this.type = ActorType.RECEIVER;
        this.parent = parent;
    }

    public boolean propose(int agreementId, Set<Integer> values) {
        if (values == null) {
            throw new IllegalArgumentException("Cannot propose null values set");
        }
        if (!agreements.containsKey(agreementId) && windowPosition.get() > Constants.MAX_OUT_OF_ORDER_DELIVERY) {
            // we cannot propose a new agreement for now : would increase window too much
            return false;
        } else {
            if (agreements.putIfAbsent(agreementId, new AgreementState(0, Collections.emptySet())) == null) {
                // we add a new agreement in the pipeline
                windowPosition.incrementAndGet();
                // toDeliver.add(agreementId);
            }
            synchronized (agreements) {
                AgreementState ag = agreements.get(agreementId);
                ag.setProposedValues(values);
                ag.incrementActiveProposalNumber();
                return toBroadcast.add(new Message(EchoAck.ECHO, myId, myId, agreementId,
                        ag.getActiveProposalNumber(), PayloadType.PROPOSAL, values));
            }
        }
    }

    @Override
    public PlState getPlState() {
        return pl.getPlState();
    }

    @Override
    public void deliver(Message m) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot deliver a null message");
        }
        synchronized (agreements) {
            int mAgreementId = m.getAgreementId();
            if (m.getPayloadType() == PayloadType.ACK) {
                if (!agreements.containsKey(mAgreementId)) {
                    throw new IllegalStateException("Should not receive an ACK for a message not in agreements");
                }
                // we increment ack count if the received ack is for the actual proposal number
                if (agreements.get(mAgreementId).getActiveProposalNumber() == m.getActivePropNumber()) {
                    agreements.get(mAgreementId).incrementAckCount();
                }
            } else if (m.getPayloadType() == PayloadType.NACK) {
                if (!agreements.containsKey(mAgreementId)) {
                    throw new IllegalStateException("Should not receive a NACK for a message not in agreements");
                }
                // we increment nack count if the received nack is for the actual proposal
                // number
                // we update the proposed values set accordingly
                if (agreements.get(mAgreementId).getActiveProposalNumber() == m.getActivePropNumber()) {
                    agreements.get(mAgreementId).unionProposedValues(m.getValues());
                    agreements.get(mAgreementId).incrementNackCount();
                }
            } else if (m.getPayloadType() == PayloadType.PROPOSAL) {
                if (agreements.putIfAbsent(mAgreementId,
                        new AgreementState(m.getActivePropNumber(), Collections.emptySet())) == null) {
                    // we add a new agreement in the pipeline
                    windowPosition.incrementAndGet();
                    // toDeliver.add(mAgreementId);
                }
                // either accepted_values ⊆ proposed_values or not
                if (agreements.get(mAgreementId).acceptedValuesIn(m.getValues())) {
                    agreements.get(mAgreementId).setAcceptedValues(m.getValues());
                    pl.addToSend(
                            new Message(EchoAck.ECHO, myId, myId, mAgreementId, m.getActivePropNumber(),
                                    PayloadType.ACK,
                                    null),
                            m.getSourceId());
                } else {
                    agreements.get(mAgreementId).unionAcceptedValues(m.getValues());
                    pl.addToSend(
                            new Message(EchoAck.ECHO, myId, myId, mAgreementId, m.getActivePropNumber(),
                                    PayloadType.NACK,
                                    agreements.get(mAgreementId).getAcceptedValues()),
                            m.getSourceId());
                }
            } else {
                throw new IllegalStateException("Cannot identify message payload type");
            }
            AgreementState ag = agreements.get(mAgreementId);
            if (ag.getNackCount() > 0 && ag.getAckCount() + ag.getNackCount() > hostsMap.size() / 2) {
                ag.incrementActiveProposalNumber();
                ag.resetAckCount();
                ag.resetNackCount();
                toBroadcast.add(new Message(EchoAck.ECHO, myId, myId, mAgreementId, ag.getActiveProposalNumber(),
                        PayloadType.PROPOSAL, ag.getProposedValues()));
            }
            if (ag.getAckCount() > hostsMap.size() / 2) {
                parent.deliver(mAgreementId, ag.getProposedValues());
                ag.deactivate();
                windowPosition.decrementAndGet();
            }
        }
    }

    @Override
    public LatticeState getLatticeState() {
        return new LatticeState(agreements, windowPosition, toBroadcast, toDeliver);
    }

    @Override
    public void run() {
        Thread plThread = new Thread(pl, "Host " + myId + " PL " + type.name());
        plThread.start();
        try {
            if (type == ActorType.SENDER) {
                while (true) {
                    runSenderLattice();
                }

            } else if (type == ActorType.RECEIVER) {
                while (true) {
                    runReceiverLattice();
                }
            } else {
                throw new IllegalStateException("Cannot identify actor type");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted " + type.name() + " LatticeAgreement");
            e.printStackTrace();
        }

    }

    private void runSenderLattice() throws InterruptedException {
        Message mToSend = toBroadcast.poll();
        if (mToSend == null) {
            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        } else {
            for (short dest : hostsMap.keySet()) {
                pl.addToSend(mToSend, dest);
            }
        }
    }

    private void runReceiverLattice() throws InterruptedException {
        // Integer agId = toDeliver.poll();
        // if (agId == null) {
        // Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        // } else {
        synchronized (agreements) {
            // AgreementState ag = agreements.get(agId);
            // if (ag.getActive()) {
            // boolean toReDeliver = true;
            // if (ag.getNackCount() > 0 && ag.getAckCount() + ag.getNackCount() >
            // hostsMap.size() / 2) {
            // ag.incrementActiveProposalNumber();
            // ag.resetAckCount();
            // ag.resetNackCount();
            // toBroadcast.add(new Message(EchoAck.ECHO, myId, myId, agId,
            // ag.getActiveProposalNumber(),
            // PayloadType.PROPOSAL, ag.getProposedValues()));
            // }
            // if (ag.getAckCount() > hostsMap.size() / 2) {
            // parent.deliver(agId, ag.getProposedValues());
            // ag.deactivate();
            // toReDeliver = false;
            // windowPosition.decrementAndGet();
            // }
            // if (toReDeliver) {
            // toDeliver.add(agId);
            // }
            // }
            // }
            int N = hostsMap.size();
            // agreements.entrySet().removeIf((entry) -> {
            //     if (!entry.getValue().getActive() && entry.getValue().getAckCount() >= N) {
            //         pl.flush(entry.getKey());
            //         return true;
            //     }
            //     return false;
            // });
        }
        Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL * 10);
    }
}

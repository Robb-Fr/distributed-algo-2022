package cs451.Broadcasts;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
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
    private final ConcurrentSkipListMap<Integer, AgreementState> agreements;
    private final ConcurrentLinkedQueue<Message> toBroadcast;
    private final ConcurrentLinkedQueue<Message> toDeliver;
    private final AtomicInteger windowSize;
    private final AtomicInteger windowBottom;
    private final PerfectLink pl;
    private final ActorType type;
    private final Receiver parent;

    public LatticeAgreement(short myId, Map<Short, Host> hostsMap, LatticeConfig config)
            throws SocketException, UnknownHostException {
        this.myId = myId;
        this.hostsMap = hostsMap;
        this.agreements = new ConcurrentSkipListMap<>();
        this.windowSize = new AtomicInteger(0);
        this.windowBottom = new AtomicInteger(0);
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
        this.windowSize = latticeState.getWindowSize();
        this.windowBottom = latticeState.getWindowBottom();
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
        if (!agreements.containsKey(agreementId) && windowSize.get() > Constants.MAX_OUT_OF_ORDER_DELIVERY) {
            // we cannot propose a new agreement for now : would increase window too much
            return false;
        } else {
            if (agreements.putIfAbsent(agreementId, new AgreementState(0, Collections.emptySet())) == null) {
                // we add a new agreement in the pipeline
                windowSize.incrementAndGet();
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
        toDeliver.add(m);
    }

    @Override
    public LatticeState getLatticeState() {
        return new LatticeState(agreements, windowSize, windowBottom, toBroadcast, toDeliver);
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
            plThread.interrupt();
            e.printStackTrace();
        }

    }

    private void runSenderLattice() throws InterruptedException {
        Message mToSend = toBroadcast.poll();
        if (mToSend == null) {
            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        } else {
            for (short dest : hostsMap.keySet()) {
                while (!pl.addToSend(mToSend, dest)) {
                    Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                }
            }
        }
    }

    private void runReceiverLattice() throws InterruptedException {
        Message m = toDeliver.poll();
        if (m == null) {
            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
        } else {
            int mAgreementId = m.getAgreementId();
            if (windowBottom.get() > mAgreementId) {
                // we moved on from this agreement
                return;
            }
            synchronized (agreements) {
                AgreementState ag = agreements.get(mAgreementId);
                if (m.getPayloadType() == PayloadType.ACK) {
                    if (!agreements.containsKey(mAgreementId)) {
                        System.err
                                .println("Should not receive an ACK for a message not in agreements : " + mAgreementId);
                        return;
                    }
                    // we increment ack count if the received ack is for the actual proposal number
                    if (ag.getActiveProposalNumber() == m.getActivePropNumber()) {
                        ag.incrementAckCount();
                    }
                } else if (m.getPayloadType() == PayloadType.NACK) {
                    if (!agreements.containsKey(mAgreementId)) {
                        System.err
                                .println("Should not receive a NACK for a message not in agreements : " + mAgreementId);
                        return;
                    }
                    // we increment nack count if the received nack is for the actual proposal
                    // number
                    // we update the proposed values set accordingly
                    if (ag.getActiveProposalNumber() == m.getActivePropNumber()) {
                        ag.unionProposedValues(m.getValues());
                        ag.incrementNackCount();
                    }
                } else if (m.getPayloadType() == PayloadType.DECIDED) {
                    if (agreements.putIfAbsent(mAgreementId,
                            new AgreementState(m.getActivePropNumber(), Collections.emptySet())) == null) {
                        // we add a new agreement in the pipeline
                        windowSize.incrementAndGet();
                    }
                    ag = agreements.get(mAgreementId);
                    ag.incrementDecidedCount();
                } else if (m.getPayloadType() == PayloadType.PROPOSAL) {
                    if (agreements.putIfAbsent(mAgreementId,
                            new AgreementState(m.getActivePropNumber(), Collections.emptySet())) == null) {
                        // we add a new agreement in the pipeline
                        windowSize.incrementAndGet();
                    }
                    ag = agreements.get(mAgreementId);
                    // either accepted_values ⊆ proposed_values or not
                    if (ag.acceptedValuesIn(m.getValues())) {
                        ag.setAcceptedValues(m.getValues());
                        while (!pl.addToSend(
                                new Message(EchoAck.ECHO, myId, myId, mAgreementId, m.getActivePropNumber(),
                                        PayloadType.ACK,
                                        null),
                                m.getSourceId())) {
                            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                        }
                    } else {
                        ag.unionAcceptedValues(m.getValues());
                        while (!pl.addToSend(
                                new Message(EchoAck.ECHO, myId, myId, mAgreementId, m.getActivePropNumber(),
                                        PayloadType.NACK,
                                        ag.getAcceptedValues()),
                                m.getSourceId())) {
                            Thread.sleep(Constants.SLEEP_BEFORE_NEXT_POLL);
                        }
                    }
                } else {
                    throw new IllegalStateException("Cannot identify message payload type");
                }
                if (ag.getActive()) {
                    if (ag.getNackCount() > 0 && ag.getAckCount() + ag.getNackCount() > hostsMap.size() / 2) {
                        ag.incrementActiveProposalNumber();
                        ag.resetAckCount();
                        ag.resetNackCount();
                        toBroadcast
                                .add(new Message(EchoAck.ECHO, myId, myId, mAgreementId, ag.getActiveProposalNumber(),
                                        PayloadType.PROPOSAL, ag.getProposedValues()));
                    }
                    if (ag.getAckCount() > hostsMap.size() / 2) {
                        parent.deliver(mAgreementId, ag.getProposedValues());
                        ag.deactivate();
                        windowSize.decrementAndGet();
                        toBroadcast.add(new Message(EchoAck.ECHO, myId, myId, mAgreementId,
                                ag.getActiveProposalNumber(), PayloadType.DECIDED, null));
                    }
                }
                if (agreements.firstKey() == windowBottom.get()
                        && agreements.firstEntry().getValue().getDecidedCount() >= hostsMap.size()) {
                    windowBottom.incrementAndGet();
                    pl.flush(agreements.firstKey());
                    agreements.remove(agreements.firstKey());
                }
            }
        }
    }
}

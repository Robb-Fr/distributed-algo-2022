package cs451.States;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Messages.Message;

public class LatticeState {
    private final ConcurrentSkipListMap<Integer, AgreementState> agreements;
    private final AtomicInteger windowSize;
    private final AtomicInteger windowBottom;
    private final ConcurrentLinkedQueue<Message> toBroadcast;
    private final ConcurrentLinkedQueue<Message> toDeliver;

    public LatticeState(ConcurrentSkipListMap<Integer, AgreementState> agreements, AtomicInteger windowSize,
            AtomicInteger windowBottom, ConcurrentLinkedQueue<Message> toBroadcast,
            ConcurrentLinkedQueue<Message> toDeliver) {
        this.agreements = agreements;
        this.windowSize = windowSize;
        this.windowBottom = windowBottom;
        this.toBroadcast = toBroadcast;
        this.toDeliver = toDeliver;
    }

    public ConcurrentSkipListMap<Integer, AgreementState> getAgreements() {
        return agreements;
    }

    public AtomicInteger getWindowSize() {
        return windowSize;
    }

    public AtomicInteger getWindowBottom() {
        return windowBottom;
    }

    public ConcurrentLinkedQueue<Message> getToBroadcast() {
        return toBroadcast;
    }

    public ConcurrentLinkedQueue<Message> getToDeliver() {
        return toDeliver;
    }

}

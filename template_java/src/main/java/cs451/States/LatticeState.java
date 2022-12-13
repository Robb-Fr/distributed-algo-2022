package cs451.States;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Messages.Message;

public class LatticeState {
    private final ConcurrentHashMap<Integer, AgreementState> agreements;
    private final AtomicInteger windowPosition;
    private final ConcurrentLinkedQueue<Message> toBroadcast;
    private final ConcurrentLinkedQueue<Message> toDeliver;

    public LatticeState(ConcurrentHashMap<Integer, AgreementState> agreements, AtomicInteger windowPosition,
            ConcurrentLinkedQueue<Message> toBroadcast, ConcurrentLinkedQueue<Message> toDeliver) {
        this.agreements = agreements;
        this.windowPosition = windowPosition;
        this.toBroadcast = toBroadcast;
        this.toDeliver = toDeliver;
    }

    public ConcurrentHashMap<Integer, AgreementState> getAgreements() {
        return agreements;
    }

    public AtomicInteger getWindowPosition() {
        return windowPosition;
    }

    public ConcurrentLinkedQueue<Message> getToBroadcast() {
        return toBroadcast;
    }

    public ConcurrentLinkedQueue<Message> getToDeliver() {
        return toDeliver;
    }

}

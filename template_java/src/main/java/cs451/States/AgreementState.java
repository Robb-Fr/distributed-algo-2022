package cs451.States;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AgreementState {
    private final AtomicBoolean active;
    private final AtomicInteger ackCount;
    private final AtomicInteger nackCount;
    private final AtomicInteger activeProposalNumber;
    private final ConcurrentHashMap.KeySetView<Integer, Boolean> proposedValues;
    private final ConcurrentHashMap.KeySetView<Integer, Boolean> acceptedValues;

    public AgreementState(int activeProposalNumber, Set<Integer> proposedValues) {
        this.active = new AtomicBoolean(true);
        this.ackCount = new AtomicInteger(0);
        this.nackCount = new AtomicInteger(0);
        this.activeProposalNumber = new AtomicInteger(activeProposalNumber);
        this.proposedValues = ConcurrentHashMap.newKeySet(proposedValues.size());
        for (int i : proposedValues) {
            this.proposedValues.add(i);
        }
        this.acceptedValues = ConcurrentHashMap.newKeySet(proposedValues.size());
    }

    public synchronized void setProposedValues(Set<Integer> newProposedValues) {
        this.proposedValues.clear();
        this.proposedValues.addAll(Set.copyOf(newProposedValues));
    }

    public synchronized void setAcceptedValues(Set<Integer> newProposedValues) {
        this.acceptedValues.clear();
        this.acceptedValues.addAll(Set.copyOf(newProposedValues));
    }

    public Set<Integer> getProposedValues() {
        return Set.copyOf(this.proposedValues);
    }

    public Set<Integer> getAcceptedValues() {
        return Set.copyOf(this.acceptedValues);
    }

    public synchronized boolean unionProposedValues(Set<Integer> newProposedValues) {
        return this.proposedValues.addAll(Set.copyOf(newProposedValues));
    }

    public synchronized boolean unionAcceptedValues(Set<Integer> newProposedValues) {
        return this.acceptedValues.addAll(Set.copyOf(newProposedValues));
    }

    public synchronized boolean acceptedValuesIn(Set<Integer> proposed) {
        return proposed.containsAll(acceptedValues);
    }

    public boolean getActive() {
        return active.get();
    }

    public void deactivate() {
        active.set(false);
    }

    public int getAckCount() {
        return ackCount.get();
    }

    public synchronized void resetAckCount() {
        ackCount.set(0);
    }

    public synchronized void incrementAckCount() {
        ackCount.incrementAndGet();
    }

    public int getNackCount() {
        return nackCount.get();
    }

    public synchronized void resetNackCount() {
        nackCount.set(0);
    }

    public synchronized void incrementNackCount() {
        nackCount.incrementAndGet();
    }

    public int getActiveProposalNumber() {
        return activeProposalNumber.get();
    }

    public synchronized void incrementActiveProposalNumber() {
        activeProposalNumber.incrementAndGet();
    }
}

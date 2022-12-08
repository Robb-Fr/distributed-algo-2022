package cs451.States;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AgreementState {
    private AtomicBoolean active;
    private AtomicInteger ackCount;
    private AtomicInteger nackCount;
    private AtomicInteger activeProposalNumber;
    private ConcurrentHashMap.KeySetView<Integer, Boolean> proposedValues;

    public AgreementState(boolean active, int ackCount, int nackCount,
            int activeProposalNumber, Set<Integer> proposedValues) {
        this.active = new AtomicBoolean(active);
        this.ackCount = new AtomicInteger(ackCount);
        this.nackCount = new AtomicInteger(nackCount);
        this.activeProposalNumber = new AtomicInteger(activeProposalNumber);
        this.proposedValues = ConcurrentHashMap.newKeySet(proposedValues.size());
        for (int i : proposedValues) {
            this.proposedValues.add(i);
        }
    }

    public boolean getActive() {
        return active.get();
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public int getAckCount() {
        return ackCount.get();
    }

    public void setAckCount(int ackCount) {
        this.ackCount.set(ackCount);
    }

    public int getNackCount() {
        return nackCount.get();
    }

    public void setNackCount(int nackCount) {
        this.nackCount.set(nackCount);
    }

    public int getActiveProposalNumber() {
        return activeProposalNumber.get();
    }

    public void setActiveProposalNumber(int activeProposalNumber) {
        this.activeProposalNumber.set(activeProposalNumber);
    }

    public ConcurrentHashMap.KeySetView<Integer, Boolean> getProposedValues() {
        return proposedValues;
    }

    public void setProposedValues(Set<Integer> proposedValues) {
        this.proposedValues.clear();
        for (int i : proposedValues) {
            this.proposedValues.add(i);
        }
    }

}

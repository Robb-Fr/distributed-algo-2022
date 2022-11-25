package cs451.States;

import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.MessageToBeSent;
import cs451.Messages.MessageTupleWithSender;

public class PlState {
    private final DatagramSocket plSocket;
    private final ConcurrentLowMemoryMsgSet<MessageTupleWithSender> plAcked;
    private final ConcurrentLinkedQueue<MessageToBeSent> toSend;

    public PlState(DatagramSocket plSocket,
            ConcurrentLowMemoryMsgSet<MessageTupleWithSender> plAcked, ConcurrentLinkedQueue<MessageToBeSent> toSend) {
        if (plSocket == null || plAcked == null || toSend == null) {
            throw new IllegalArgumentException("Cannot make plState with null argument");
        }
        this.plSocket = plSocket;
        this.plAcked = plAcked;
        this.toSend = toSend;
    }

    public DatagramSocket getPlSocket() {
        return plSocket;
    }

    public ConcurrentLowMemoryMsgSet<MessageTupleWithSender> getPlAcked() {
        return plAcked;
    }

    public ConcurrentLinkedQueue<MessageToBeSent> getToSend() {
        return toSend;
    }

}

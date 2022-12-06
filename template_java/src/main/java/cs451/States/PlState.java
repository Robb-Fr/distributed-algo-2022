package cs451.States;

import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.MessageToBeSent;

public class PlState {
    private final DatagramSocket plSocket;
    private final ConcurrentLowMemoryMsgSet plAcked;
    private final ConcurrentLinkedQueue<MessageToBeSent> plToSend;

    public PlState(DatagramSocket plSocket,
            ConcurrentLowMemoryMsgSet plAcked, ConcurrentLinkedQueue<MessageToBeSent> plToSend) {
        if (plSocket == null || plAcked == null || plToSend == null) {
            throw new IllegalArgumentException("Cannot make plState with null argument");
        }
        this.plSocket = plSocket;
        this.plAcked = plAcked;
        this.plToSend = plToSend;
    }

    public DatagramSocket getPlSocket() {
        return plSocket;
    }

    public ConcurrentLowMemoryMsgSet getPlAcked() {
        return plAcked;
    }

    public ConcurrentLinkedQueue<MessageToBeSent> getPlToSend() {
        return plToSend;
    }

}

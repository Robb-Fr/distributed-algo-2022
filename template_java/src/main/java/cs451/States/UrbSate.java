package cs451.States;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;

public class UrbSate {
    private final ConcurrentHashMap.KeySetView<Message, Boolean> urbPending;
    private final ConcurrentLowMemoryMsgSet<Message> urbDelivered;
    private final ConcurrentHashMap<Message, ConcurrentHashMap.KeySetView<Short, Boolean>> urbAck;
    private final ConcurrentLinkedQueue<Message> urbToBroadcast;

    public UrbSate(ConcurrentHashMap.KeySetView<Message, Boolean> urbPending, ConcurrentLowMemoryMsgSet<Message> urbDelivered,
            ConcurrentHashMap<Message, KeySetView<Short, Boolean>> urbAck,
            ConcurrentLinkedQueue<Message> urbToBroadcast) {
        if (urbPending == null || urbDelivered == null || urbAck == null || urbToBroadcast == null) {
            throw new IllegalArgumentException("Cannot make urbState with null argument");
        }
        this.urbPending = urbPending;
        this.urbDelivered = urbDelivered;
        this.urbAck = urbAck;
        this.urbToBroadcast = urbToBroadcast;
    }

    public ConcurrentHashMap.KeySetView<Message, Boolean> getUrbPending() {
        return urbPending;
    }

    public ConcurrentLowMemoryMsgSet<Message> getUrbDelivered() {
        return urbDelivered;
    }

    public ConcurrentHashMap<Message, ConcurrentHashMap.KeySetView<Short, Boolean>> getUrbAck() {
        return urbAck;
    }

    public ConcurrentLinkedQueue<Message> getUrbToBroadcast() {
        return urbToBroadcast;
    }
}

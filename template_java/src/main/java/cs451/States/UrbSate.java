package cs451.States;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;

public class UrbSate {
    private final ConcurrentSkipListSet<Message> urbPending;
    private final ConcurrentLowMemoryMsgSet<Message> urbDelivered;
    private final ConcurrentSkipListMap<Message, ConcurrentHashMap.KeySetView<Short, Boolean>> urbAck;
    private final ConcurrentLinkedQueue<Message> urbToBroadcast;

    public UrbSate(ConcurrentSkipListSet<Message> urbPending,
            ConcurrentLowMemoryMsgSet<Message> urbDelivered,
            ConcurrentSkipListMap<Message, KeySetView<Short, Boolean>> urbAck,
            ConcurrentLinkedQueue<Message> urbToBroadcast) {
        if (urbPending == null || urbDelivered == null || urbAck == null || urbToBroadcast == null) {
            throw new IllegalArgumentException("Cannot make urbState with null argument");
        }
        this.urbPending = urbPending;
        this.urbDelivered = urbDelivered;
        this.urbAck = urbAck;
        this.urbToBroadcast = urbToBroadcast;
    }

    public ConcurrentSkipListSet<Message> getUrbPending() {
        return urbPending;
    }

    public ConcurrentLowMemoryMsgSet<Message> getUrbDelivered() {
        return urbDelivered;
    }

    public ConcurrentSkipListMap<Message, ConcurrentHashMap.KeySetView<Short, Boolean>> getUrbAck() {
        return urbAck;
    }

    public ConcurrentLinkedQueue<Message> getUrbToBroadcast() {
        return urbToBroadcast;
    }
}

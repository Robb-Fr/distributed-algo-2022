package cs451.Messages;

import cs451.Constants;
import cs451.Host;

public class MessageToBeSent {
    private final Message message;
    private final byte[] serializedMsg;
    private final Host dest;

    public MessageToBeSent(Message m, Host dest) {
        if (m == null || dest == null) {
            throw new IllegalArgumentException("Cannot build MessageToBeSent with null argument");
        }

        byte[] msgBytes = m.serialize();
        if (msgBytes == null) {
            throw new IllegalArgumentException("Could not serialize message m");
        }
        if (msgBytes.length >= Constants.MAX_DATAGRAM_LENGTH) {
            throw new IndexOutOfBoundsException("Sent packet exceeds maximum accepted packet size");
        }
        this.serializedMsg = msgBytes;
        this.dest = dest;
        this.message = m;
    }

    public Message getMessage() {
        return message;
    }

    public byte[] getSerializedMsg() {
        return serializedMsg;
    }

    public Host getDest() {
        return dest;
    }
}

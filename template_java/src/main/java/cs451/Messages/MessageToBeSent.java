package cs451.Messages;

import cs451.Constants;

public class MessageToBeSent {
    private final Message message;
    private final byte[] serializedMsg;
    private final int dest;

    public MessageToBeSent(Message m, int dest) {
        if (m == null) {
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

    public int getDest() {
        return dest;
    }
}

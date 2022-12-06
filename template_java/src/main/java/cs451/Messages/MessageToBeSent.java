package cs451.Messages;

public class MessageToBeSent {
    private final Message message;
    private final byte[] serializedMsg;
    private final short dest;

    public MessageToBeSent(Message m, short dest) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot build MessageToBeSent with null argument");
        }

        byte[] msgBytes = m.serialize();

        if (msgBytes == null) {
            throw new IllegalArgumentException("Could not serialize message m");
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

    public short getDest() {
        return dest;
    }
}

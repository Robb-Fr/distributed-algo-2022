package cs451.Messages;

public class MessageToBeSent {
    private final Message message;
    private final byte[] serializedMsg;
    private final short dest;
    private long timeOfSending;

    public MessageToBeSent(Message m, short dest, boolean buildFull) {
        if (m == null) {
            throw new IllegalArgumentException("Cannot build MessageToBeSent with null argument");
        }
        if (buildFull) {
            byte[] msgBytes = m.serialize();

            if (msgBytes == null) {
                throw new IllegalArgumentException("Could not serialize message m");
            }

            this.serializedMsg = msgBytes;

        } else {
            this.serializedMsg = null;
        }
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

    public long getTimeOfSending() {
        return timeOfSending;
    }

    public void setTimeOfSending(long timeOfSending) {
        this.timeOfSending = timeOfSending;
    }

    @Override
    public String toString() {
        return "MessageToBeSent [message=" + message + ", dest=" + dest + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + dest;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessageToBeSent other = (MessageToBeSent) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (dest != other.dest)
            return false;
        return true;
    }
}

package cs451.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import cs451.Constants;

public class Message {
    public enum PayloadType {
        CONTENT,
        ACK;

        public final static PayloadType[] values = PayloadType.values();

        public byte byteValue() {
            if (this.ordinal() > Byte.MAX_VALUE) {
                throw new IllegalStateException("Cannot have more than 128 payload types");
            }
            return (byte) this.ordinal();
        }

        public static PayloadType fromByte(byte b) {
            if (b < 0 || b >= values.length) {
                throw new IllegalStateException("Cannot deserialize payload type");
            }
            return values[b];
        }
    }

    /**
     * Gives the message encoded in the serialized bytes given in input or null the
     * bytes can't be deserialized.
     * 
     * @param bytes : the received bytes on the socket to be deserialized
     * @return : the received message or null if deserialization failed
     */
    public static Message deserialize(byte[] bytes) {
        if (bytes.length != Constants.SERIALIZED_MSG_SIZE) {
            throw new IllegalArgumentException("Cannot deserialize the received message");
        }
        if (Arrays.equals(bytes, Constants.EMPTY_MESSAGE)) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        Message m = new Message(buffer.getInt(), buffer.getShort(), buffer.getShort(),
                PayloadType.fromByte(buffer.get()));
        return m;
    }

    private final int id;

    private final short sourceId;

    private final short senderId;

    private final PayloadType type;

    /**
     * Creates a new message where the source (original sender) will be the same as
     * the actual sender (not forwarded)
     * 
     * @param id     : the message id (basically the index). It is considered
     *               payload in the CONTENT type packets
     * @param sender : the sender of the message
     * @param type   : if the message is an ACK or a CONTENT type message
     */
    public Message(int id, short senderId, PayloadType type) {
        if (type == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        if (senderId == 0) {
            throw new IllegalArgumentException("There");
        }
        this.id = id;
        this.sourceId = senderId;
        this.senderId = senderId;
        this.type = type;
    }

    /**
     * @param id       : message id
     * @param sourceId : the id of original host sender
     * @param senderId : the id of the host actually forwarding this message
     * @param type     : either ACK or CONTENT type
     */
    public Message(int id, short sourceId, short senderId, PayloadType type) {
        if (type == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        if (sourceId == 0) {
            throw new IllegalArgumentException("There");
        }
        this.id = id;
        this.sourceId = sourceId;
        this.senderId = senderId;
        this.type = type;
    }

    /**
     * Utility for easily creating a message to be forwarded (only changes the
     * senderId)
     * 
     * @param newSenderId : the id of the host that'll forward this message
     * @return
     */
    public Message withUpdatedSender(short newSenderId) {
        return new Message(id, sourceId, newSenderId, type);
    }

    public Message withUpdatedId(int newId) {
        return new Message(newId, sourceId, senderId, type);
    }

    public Message ackForThisMessage(short idOfSenderOfThisAck) {
        return new Message(id, sourceId, idOfSenderOfThisAck, PayloadType.ACK);
    }

    public MessageTupleWithSender tupleWithSender() {
        return new MessageTupleWithSender(id, sourceId, senderId, type);
    }

    public MessageToBeSent preparedForSending(short dest) {
        return new MessageToBeSent(this, dest);
    }

    public int getId() {
        return id;
    }

    public short getSourceId() {
        return sourceId;
    }

    public short getSenderId() {
        return senderId;
    }

    public boolean isAck() {
        return type == PayloadType.ACK;
    }

    /**
     * Returns whether this message is an ACK message for the id given in argument
     * 
     * @param id : the id of the message we want to check ACK for
     * @return : wether this message is an ACK for the CONTENT message with given id
     */
    public boolean isAckForMsg(int id) {
        return type == PayloadType.ACK && this.id == id;
    }

    /**
     * Uses Java standard serialization to give the bytes corresponding to this
     * object Message
     * https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
     * 
     * @return : the serialized bytes representing this message
     * @throws IOException
     */
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.SERIALIZED_MSG_SIZE);
        buffer.putInt(id).putShort(sourceId).putShort(senderId).put(type.byteValue());
        return buffer.array();
    }

    @Override
    public String toString() {
        return "Message [id=" + id + ", sourceId=" + sourceId + ", senderId=" + senderId + ", type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + sourceId;
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
        Message other = (Message) obj;
        if (id != other.id)
            return false;
        if (sourceId != other.sourceId)
            return false;
        return true;
    }

}

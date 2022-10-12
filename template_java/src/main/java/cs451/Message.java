package cs451;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable {
    public enum PayloadType {
        CONTENT,
        ACK
    }

    /**
     * Gives the message encoded in the serialized bytes given in input or null the
     * bytes can't be deserialized.
     * 
     * @param bytes : the received bytes on the socket to be deserialized
     * @return : the received message or null if deserialization failed
     */
    public static Message deserialize(byte[] bytes) {
        // https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput in = new ObjectInputStream(bis);
            Message m = (Message) in.readObject();
            in.close();
            bis.close();
            if (m instanceof Message && m.type != null) {
                return m;
            }
        } catch (IOException | ClassNotFoundException e) {
            // ignore the error
        }
        return null;
    }

    private final int id;

    private final int senderId;

    private final PayloadType type;

    /**
     * @param id     : the message id (basically the index). It is considered
     *               payload in the CONTENT type packets
     * @param sender : the sender of the message
     * @param type   : if the message is an ACK or a CONTENT type message
     */
    public Message(int id, int senderId, PayloadType type) {
        if (type == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        this.id = id;
        this.senderId = senderId;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public int getSenderId() {
        return this.senderId;
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
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();
            return serialized;
        } catch (IOException e) {
            System.err.println("An error occurred while serializing the received object");
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public String toString() {
        return "Message [id=" + id + ", senderId=" + senderId + ", type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + senderId;
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
        if (senderId != other.senderId)
            return false;
        return true;
    }

}

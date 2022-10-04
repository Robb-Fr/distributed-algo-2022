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
     * @param bytes : the received bytes on the socket to be deserialized
     * @return : the received message or null if deserialization failed
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        // https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        Message m = null;
        try {
            in = new ObjectInputStream(bis);
            m = (Message) in.readObject();
            if (!(m instanceof Message | m.sender == null | m.type == null)) {
                throw new ClassCastException("Cannot deserialize the bytes to the class object");
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return m;
    }

    private final int id;

    private final Host sender;

    private final PayloadType type;

    /**
     * @param id     : the message id (basically the index). It is considered
     *               payload in the CONTENT type packets
     * @param sender : the sender of the message
     * @param type   : if the message is an ACK or a CONTENT type message
     */
    public Message(int id, Host sender, PayloadType type) {
        if (sender == null || type == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        this.id = id;
        this.sender = sender;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public long getSenderId() {
        return sender.getId();
    }

    public Host getSender() {
        return sender;
    }

    public boolean isAck() {
        return type == PayloadType.ACK;
    }

    /**
     * @param id : the id of the message we want to check ACK for
     * @return : wether this message is an ACK for the CONTENT message with given id
     */
    public boolean isAckForMsg(int id) {
        return type == PayloadType.ACK && this.id == id;
    }

    /**
     * https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
     * 
     * @return : the serialized bytes representing this message
     * @throws IOException
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        byte[] serialized = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            serialized = bos.toByteArray();
            if (serialized == null || serialized.length == 0) {
                throw new IOException("Unable to serialize to a non empty value");
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return serialized;

    }

    @Override
    public int hashCode() {
        // only takes into account the id (for the hash tables)
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        return true;
    }

}

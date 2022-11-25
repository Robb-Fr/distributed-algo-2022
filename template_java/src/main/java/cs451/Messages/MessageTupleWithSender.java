package cs451.Messages;

public class MessageTupleWithSender extends Message {

    public MessageTupleWithSender(int id, short sourceId, short senderId, PayloadType type) {
        super(id, sourceId, senderId, type);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getId();
        result = prime * result + getSourceId();
        result = prime * result + getSenderId();
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
        MessageTupleWithSender other = (MessageTupleWithSender) obj;
        if (super.getId() != other.getId())
            return false;
        if (getSourceId() != other.getSourceId())
            return false;
        if (getSenderId() != other.getSenderId())
            return false;
        return true;
    }

    @Override
    public int compareTo(Message o) {
        return (Short.compare(senderId, o.senderId) << 16) + super.compareTo(o);
    }

}

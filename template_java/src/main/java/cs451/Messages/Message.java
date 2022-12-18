package cs451.Messages;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import cs451.Constants;

public class Message {
    public enum EchoAck {
        ECHO,
        ACK;

        public final static EchoAck[] values = EchoAck.values();

        public static EchoAck fromByte(byte b) {
            if (b < 0 || b >= values.length) {
                throw new IllegalStateException("Cannot deserialize content type");
            }
            return values[b];
        }

        public byte byteValue() {
            if (this.ordinal() > Byte.MAX_VALUE) {
                throw new IllegalStateException("Cannot have more than 128 payload types");
            }
            return (byte) this.ordinal();
        }
    }

    public enum PayloadType {
        PROPOSAL, ACK, NACK, DECIDED;

        public final static PayloadType[] values = PayloadType.values();

        public static PayloadType fromByte(byte b) {
            if (b < 0 || b >= values.length) {
                throw new IllegalStateException("Cannot deserialize payload type");
            }
            return values[b];
        }

        public byte byteValue() {
            if (this.ordinal() > Byte.MAX_VALUE) {
                throw new IllegalStateException("Cannot have more than 128 payload types");
            }
            return (byte) this.ordinal();
        }
    }

    public static Message deserialize(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        EchoAck echoAck = EchoAck.fromByte(buffer.get());
        short senderId = buffer.getShort();
        short sourceId = buffer.getShort();
        int agreementId = buffer.getInt();
        int activePropNumber = buffer.getInt();
        PayloadType payloadType = PayloadType.fromByte(buffer.get());
        if (echoAck == EchoAck.ACK || payloadType == PayloadType.ACK || payloadType == PayloadType.DECIDED) {
            return new Message(echoAck, senderId, sourceId, agreementId, activePropNumber, payloadType, null);
        } else {
            int nbVals = buffer.getInt();
            HashSet<Integer> proposals = new HashSet<>(nbVals);
            for (int i = 0; i < nbVals; ++i) {
                proposals.add(buffer.getInt());
            }
            return new Message(echoAck, senderId, sourceId, agreementId, activePropNumber, payloadType, proposals);
        }
    }

    private final EchoAck echoAck;

    private final short senderId;

    private final short sourceId;

    private final int agreementId;

    private final int activePropNumber;

    private final PayloadType payloadType;

    private final Set<Integer> values;

    public Message(EchoAck cType, short senderId, short sourceId, int agreementId, int activePropNumber,
            PayloadType pType, Set<Integer> values) {
        if (cType == null || pType == null) {
            throw new IllegalArgumentException("You cannot create a message with null fields");
        }
        if (senderId == 0) {
            throw new IllegalArgumentException("Error occurred while deserializing");
        }
        this.echoAck = cType;
        this.senderId = senderId;
        this.sourceId = sourceId;
        this.agreementId = agreementId;
        this.activePropNumber = activePropNumber;
        this.payloadType = pType;
        if (echoAck == EchoAck.ACK || payloadType == PayloadType.ACK || payloadType == PayloadType.DECIDED) {
            this.values = null;
        } else {
            if (values == null) {
                throw new IllegalArgumentException("Cannot have null values for a proposal");
            }
            this.values = values;
        }
    }

    public boolean isAck() {
        return echoAck == EchoAck.ACK;
    }

    public boolean isAckForMsg(Message m) {
        return echoAck == EchoAck.ACK && this.equals(m);
    }

    public MessageToBeSent toSendTo(short dest, boolean buildFull) {
        return new MessageToBeSent(this, dest, buildFull);
    }

    public Message ack(short ackSenderId) {
        return new Message(EchoAck.ACK, ackSenderId, sourceId, agreementId, activePropNumber, payloadType, null);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer
                .allocate(Constants.MSG_SIZE_NO_VALUES + Integer.BYTES * (values == null ? 0 : values.size() + 1));
        buffer.put(echoAck.byteValue()).putShort(senderId).putShort(sourceId).putInt(agreementId)
                .putInt(activePropNumber).put(payloadType.byteValue());
        if (!(echoAck == EchoAck.ACK || payloadType == PayloadType.ACK || payloadType == PayloadType.DECIDED)) {
            buffer.putInt(values.size());
            for (int v : values) {
                buffer.putInt(v);
            }
        }
        return buffer.array();
    }

    @Override
    public String toString() {
        return "Message [contentType=" + echoAck + ", senderId=" + senderId + ", sourceId=" + sourceId
                + ", agreementId=" + agreementId + ", activePropNumber=" + activePropNumber + ", payloadType="
                + payloadType + ", values=" + values + "]";
    }

    public EchoAck getEchoAck() {
        return echoAck;
    }

    public short getSenderId() {
        return senderId;
    }

    public short getSourceId() {
        return sourceId;
    }

    public int getAgreementId() {
        return agreementId;
    }

    public int getActivePropNumber() {
        return activePropNumber;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public Set<Integer> getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sourceId;
        result = prime * result + agreementId;
        result = prime * result + activePropNumber;
        result = prime * result + ((payloadType == null) ? 0 : payloadType.hashCode());
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
        if (sourceId != other.sourceId)
            return false;
        if (agreementId != other.agreementId)
            return false;
        if (activePropNumber != other.activePropNumber)
            return false;
        if (payloadType != other.payloadType)
            return false;
        return true;
    }

}

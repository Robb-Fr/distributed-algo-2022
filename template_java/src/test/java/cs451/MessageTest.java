package cs451;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import cs451.Messages.Message;
import cs451.Messages.Message.EchoAck;
import cs451.Messages.Message.PayloadType;

public class MessageTest {

    private void messageDeepEquals(Message m1, Message m2) {
        assertEquals(m1.getActivePropNumber(), m2.getActivePropNumber());
        assertEquals(m1.getAgreementId(), m2.getAgreementId());
        assertEquals(m1.getEchoAck(), m2.getEchoAck());
        assertEquals(m1.getPayloadType(), m2.getPayloadType());
        assertEquals(m1.getSenderId(), m2.getSenderId());
        assertEquals(m1.getSourceId(), m2.getSourceId());
        assertEquals(m1.getValues(), m2.getValues());
    }

    @Test
    public void testSerDeClassicMessage() {
        Message m = new Message(EchoAck.ECHO, (short) 89, (short) 22, 918237647, 98765, PayloadType.PROPOSAL,
                Set.of(1, 23, 34, 1223, 123231, 123671893, 123, 1233557876, 4567, 12213, 124, 55345));
        byte[] serialized = m.serialize();
        Message deserialized = Message.deserialize(serialized);
        messageDeepEquals(m, deserialized);
    }

    @Test
    public void testSerDeClassicMessage2() {
        Message m = new Message(EchoAck.ECHO, (short) 23, (short) 3, 32432, 5432432, PayloadType.NACK,
                Set.of(784, 5675, 323310, 23482, 3, 1, 23, 32423, 234, 46));
        byte[] serialized = m.serialize();
        Message deserialized = Message.deserialize(serialized);
        messageDeepEquals(m, deserialized);
    }

    @Test
    public void testSerDeEmptyValues() {
        Message m = new Message(EchoAck.ECHO, (short) 12, (short) 432, 1, 1, PayloadType.ACK, null);
        byte[] serialized = m.serialize();
        Message deserialized = Message.deserialize(serialized);
        messageDeepEquals(m, deserialized);
    }

    @Test
    public void testSerDeEmptyValues2() {
        Message m = new Message(EchoAck.ACK, (short) 111, (short) 31, 9876, 111111, PayloadType.NACK, null);
        byte[] serialized = m.serialize();
        Message deserialized = Message.deserialize(serialized);
        messageDeepEquals(m, deserialized);
    }

}
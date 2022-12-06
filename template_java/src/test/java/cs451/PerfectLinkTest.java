package cs451;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cs451.Broadcasts.Deliverable;
import cs451.Broadcasts.PerfectLink;
import cs451.Messages.Message;
import cs451.Messages.Message.EchoAck;
import cs451.Messages.Message.PayloadType;
import cs451.Parsers.Parser;
import cs451.Parsers.ConfigParser.LatticeConfig;
import cs451.States.PlState;;

public class PerfectLinkTest {
    Parser parser = new Parser(
            "--id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/lattice-agreement-1.config"
                    .split(" "));
    Map<Short, Host> hostsMap;
    short myId;
    LatticeConfig config;

    public PerfectLinkTest() {
        parser.parse();
        hostsMap = parser.hostsMap();
        config = parser.configParser().getLatticeConfig();
        myId = parser.myId();
    }

    public class FakeParent implements Deliverable {
        private final List<Message> delivered = new ArrayList<>();

        @Override
        public void deliver(Message m) {
            delivered.add(m);
        }
    }

    @Test
    public void testPl() {
        try {
            PerfectLink senderPl = new PerfectLink(myId, hostsMap, config);
            PlState state = senderPl.getPlState();
            FakeParent fakeParent = new FakeParent();
            PerfectLink receiverPl = new PerfectLink(myId, hostsMap, fakeParent, config, state);
            Message m = new Message(EchoAck.ECHO, (short) 1, (short) 1, 3322, 985, PayloadType.PROPOSAL,
                    Set.of(1223));

            // send messages
            senderPl.addToSend(m, myId);
            senderPl.runSenderPl();
            // receive message
            receiverPl.runReceiverPl();
            // send ack
            senderPl.runSenderPl();
            // receive ack
            receiverPl.runReceiverPl();
            assertTrue(fakeParent.delivered.contains(m));

            // send messages
            senderPl.addToSend(m, myId);
            senderPl.runSenderPl();
            // receive message
            receiverPl.runReceiverPl();
            // send ack
            senderPl.runSenderPl();
            // receive ack
            receiverPl.runReceiverPl();
            assertEquals(1, fakeParent.delivered.size());

            Message m2 = new Message(EchoAck.ECHO, (short) 1, (short) 1, 32432, 5432432, PayloadType.PROPOSAL,
                    Set.of(784, 3));
            Message m3 = new Message(EchoAck.ECHO, (short) 1, (short) 1, 1, 1, PayloadType.ACK, null);
            Message m4 = new Message(EchoAck.ECHO, (short) 1, (short) 1, 9876, 111111, PayloadType.NACK,
                    Set.of(1, 34, 5453));
            senderPl.addToSend(m2, myId);
            senderPl.addToSend(m3, myId);
            senderPl.addToSend(m4, myId);
            // send message
            senderPl.runSenderPl();
            Thread.sleep(Constants.PL_TIMEOUT_BEFORE_RESEND);
            senderPl.runSenderPl();
            senderPl.runSenderPl();
            // receive message
            receiverPl.runReceiverPl();
            // send ack
            senderPl.runSenderPl();
            // receive message
            receiverPl.runReceiverPl();
            // send ack
            senderPl.runSenderPl();
            // receive message
            receiverPl.runReceiverPl();
            assertEquals(4, fakeParent.delivered.size());

            senderPl.close();
            receiverPl.close();
        } catch (SocketException | UnknownHostException | InterruptedException e) {
            e.printStackTrace();
            fail("Failed to allocate PL");
        }
    }
}

package cs451;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cs451.Messages.ConcurrentLowMemoryMsgSet;
import cs451.Messages.Message;
import cs451.Messages.Message.EchoAck;
import cs451.Messages.Message.PayloadType;
import cs451.Parsers.Parser;
import cs451.Parsers.ConfigParser.LatticeConfig;

public class MessageSetTest {
    Parser parser = new Parser(
            "--id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/lattice-agreement-1.config"
                    .split(" "));
    Map<Short, Host> hostsMap;
    LatticeConfig config;

    public MessageSetTest() {
        parser.parse();
        hostsMap = parser.hostsMap();
        config = parser.configParser().getLatticeConfig();
    }

    @Test
    public void testAdd() {
        ConcurrentLowMemoryMsgSet delivered = new ConcurrentLowMemoryMsgSet(hostsMap, config.getP(), config.getVs());
        Message m = new Message(EchoAck.ECHO, (short) 23, (short) 3, 32432, 5432432, PayloadType.NACK,
                Set.of(784, 5675, 323310, 23482, 3));
        delivered.add(m);
        assertTrue(delivered.contains(m));

        Message m2 = new Message(EchoAck.ECHO, (short) 89, (short) 22, 918237647, 98765, PayloadType.PROPOSAL,
                Set.of(1, 23, 34, 1223, 123231, 123671893, 123, 1233557876, 4567));
        assertFalse(delivered.contains(m2));
        delivered.add(m2);
        assertTrue(delivered.contains(m2));
    }
}

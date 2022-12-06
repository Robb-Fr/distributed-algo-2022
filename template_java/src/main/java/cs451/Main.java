package cs451;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Messages.LogsBuilder;
import cs451.Parsers.ConfigParser;
import cs451.Parsers.Parser;
import cs451.States.PlState;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        // displays config
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid
                + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host : parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        // running code for the assignment
        System.out.println("Doing some initialization\n");

        System.out.println("Creating output file");
        final LogsBuilder logsBuilder = new LogsBuilder(parser.output());
        final short myId = parser.myId();
        final Map<Short, Host> hostsMap = parser.hostsMap();
        final ConfigParser configParser = parser.configParser();

        System.out.println("Config content:");
        System.out.println("===============");
        System.out.println(configParser.getLatticeConfig());

        try {
            // Creates the sender
            Sender sender = new Sender(logsBuilder, myId, hostsMap, configParser);
            PlState plState = sender.getPlState();

            // Creates the receiver
            Receiver receiver = new Receiver(parser.output(), logsBuilder, myId, hostsMap, configParser,
                    plState);

            // Prepares threads to be started
            Thread senderThread = new Thread(sender);
            Thread receiverThread = new Thread(receiver);
            initSignalHandlers(logsBuilder, parser.output(), senderThread, receiverThread);

            System.out.println("Broadcasting and delivering messages...\n");

            senderThread.start();
            receiverThread.start();
        } catch (UnknownHostException | SocketException e) {
            System.err.println("Could not configure correctly the host");
            e.printStackTrace();
            System.exit(1);
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    private static void handleSignal(LogsBuilder logsBuilder, String output, Thread sender,
            Thread receiver) {
        // immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        sender.interrupt();
        receiver.interrupt();

        // https://www.geeksforgeeks.org/java-program-to-write-into-a-file/
        System.out.println("Writing output.");
        logsBuilder.tryFlush(true);
    }

    private static void initSignalHandlers(LogsBuilder logsBuilder, String output, Thread sender,
            Thread receiver) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(logsBuilder, output, sender, receiver);
            }
        });
    }
}

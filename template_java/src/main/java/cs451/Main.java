package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        AtomicReference<StringBuilder> logBuilder = new AtomicReference<>(new StringBuilder(""));

        initSignalHandlers(logBuilder, parser.output());

        // example
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

        try {
            System.out.println("Doing some initialization\n");
            Map<Integer, Host> hostsMap = parser.hostsMap();
            ConfigParser configParser = parser.configParser();
            int myId = parser.myId();
            Sender sender = new Sender(logBuilder, myId, hostsMap, configParser);
            Receiver receiver = new Receiver(logBuilder, myId, hostsMap.get(myId), configParser);
            Thread senderThread = new Thread(sender);
            Thread receiverThread = new Thread(receiver);

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

    private static void handleSignal(AtomicReference<StringBuilder> logBuilder, String output) {
        // immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        // https://www.geeksforgeeks.org/java-program-to-write-into-a-file/
        System.out.println("Writing output.");
        try {
            BufferedWriter f_writer = new BufferedWriter(new FileWriter(output));
            f_writer.append(logBuilder.get());
            // flush the buffer to make sure everything is written
            f_writer.flush();
            f_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write output");
        }
    }

    private static void initSignalHandlers(AtomicReference<StringBuilder> logBuilder, String output) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(logBuilder, output);
            }
        });
    }
}

package cs451.Messages;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import cs451.Constants;

public class LogsBuilder {
    private final AtomicReference<StringBuilder> logBuilder;
    private AtomicLong previousFlush;
    private final String output;

    public LogsBuilder(String output) {
        this.logBuilder = new AtomicReference<>(new StringBuilder(""));
        this.previousFlush = new AtomicLong(System.currentTimeMillis());
        this.output = output;
        System.out.println("Initializing output.");
        try {
            BufferedWriter f_writer = new BufferedWriter(new FileWriter(output));
            f_writer.append("");
            // flush the buffer to make sure everything is written
            f_writer.flush();
            f_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write output");
        }
    }

    public synchronized void log(String s) {
        synchronized (logBuilder) {
            logBuilder.getAndUpdate(sb -> sb.append(s));
        }
    }

    public synchronized void tryFlush(boolean force) {
        synchronized (logBuilder) {
            if ((System.currentTimeMillis() - previousFlush.get()) > Constants.TIME_BEFORE_FLUSH || force) {
                System.out.println("Writing output.");
                try {
                    BufferedWriter f_writer = new BufferedWriter(new FileWriter(output, true));
                    f_writer.append(logBuilder.getAndUpdate(s -> new StringBuilder("")));
                    // flush the buffer to make sure everything is written
                    f_writer.flush();
                    f_writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Failed to write output");
                }
                previousFlush.set(System.currentTimeMillis());
                System.gc();
            }
        }
    }

}

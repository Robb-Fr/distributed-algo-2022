package cs451.Broadcasts;

import cs451.Host;

public interface Flushable {
    public void flush(Host host, int deliveredUntil);
}

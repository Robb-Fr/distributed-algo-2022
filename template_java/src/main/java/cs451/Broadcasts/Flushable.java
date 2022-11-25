package cs451.Broadcasts;

import cs451.Messages.Message;

public interface Flushable<M extends Message> {
    public void flush(short host, M deliveredFrom, M deliveredUntil);
}

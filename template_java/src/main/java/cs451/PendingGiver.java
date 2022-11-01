package cs451;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public interface PendingGiver {
    /**
     * To be implemented by every classes that should be able to give a reference to
     * a a pending set of messages (because they need to share it between sender and
     * receiver)
     * 
     * @return an atomic reference to the pending messages owned by this object (or
     *         a
     *         sub-object)
     */
    public AtomicReference<Set<Message>> getPending();
}

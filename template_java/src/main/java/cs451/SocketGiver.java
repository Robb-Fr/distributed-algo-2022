package cs451;

import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicReference;

public interface SocketGiver {
    /**
     * To be implemented by every classes that should be able to give a reference to
     * a Datagram Socket (because they use a Perfect Link)
     * 
     * @return an atomic reference to the socket owned by this object (or a
     *         sub-object)
     */
    public AtomicReference<DatagramSocket> getSocket();
}

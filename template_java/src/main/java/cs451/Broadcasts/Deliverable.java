package cs451.Broadcasts;

import cs451.Messages.Message;

public interface Deliverable {
    /**
     * Delivers a message to this instance
     * 
     * @param m : the message to be delivered
     */
    public void deliver(Message m);
}

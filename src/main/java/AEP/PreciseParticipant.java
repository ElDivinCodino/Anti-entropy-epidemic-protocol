package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.StartGossip;
import AEP.nodeUtilities.Delta;

import java.util.ArrayList;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class PreciseParticipant extends Participant {

    // Maximum Transfer Unit: maximum number of deltas inside a single gossip message
    private int mtu = 5;

    public static enum Ordering { OLDEST, NEWEST};
    private Ordering method;

    public PreciseParticipant(String destinationPath, int id) {
        super(destinationPath, id);
        this.method = Ordering.OLDEST;
    }

    protected void startGossip(StartGossip message){
        logger.debug("First phase: Digest from " + getSender());
        // sender set to null because we do not need to answer to this message
        ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
        getSender().tell(new GossipMessage(false, storage.mtuResizeAndSort(toBeUpdated, mtu ,this.method)), null);
        // send to p the second message containing the digest (NOTE: in the paper it should be just the outdated entries that q requests to p)
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());
        logger.debug("Second phase: sending differences + digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());
            logger.debug("Gossip exchange with node " + sender() + " completed");
        } else {
            // second phase, receiving message(s) from q.
            if (getSender() == null){ // this is the message with deltas
                storage.reconciliation(message.getParticipantStates());
            }else{ // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
                getSender().tell(new GossipMessage(true, storage.mtuResizeAndSort(toBeUpdated, mtu, this.method)), self());
            }
            logger.debug("Third phase: sending differences to " + getSender());
        }
    }
}

package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.SetupMessage;
import AEP.messages.StartGossip;
import AEP.nodeUtilities.Delta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class PreciseParticipant extends Participant {

    // Maximum Transfer Unit: maximum number of deltas inside a single gossip message
    protected int mtu;

    public enum Ordering { OLDEST, NEWEST, SCUTTLEBREADTH, SCUTTLEDEPTH};
    protected Ordering method;

    public PreciseParticipant(int id) {
        super(id);
        this.method = Ordering.OLDEST;
    }

    protected void initValues(SetupMessage message){
        this.mtu = message.getMtu();
        super.initValues(message);
    }

    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());

        // send to p the second message containing own digest
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());

        logger.info("Second phase: sending digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
            getSender().tell(new GossipMessage(false, storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method)), null);

            logger.info("Fourth phase: sending differences to " + getSender());
        } else {
            // receiving message(s) from q.
            if (getSender() == getContext().system().deadLetters()) { // this is the message with deltas
                storage.reconciliation(message.getParticipantStates());
                logger.info("Gossip completed");
            } else { // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
                getSender().tell(new GossipMessage(true, storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method)), self());
                logger.info("Third phase: sending differences to " + getSender());
            }
        }
    }

    private class PreciseComparator implements Comparator<Delta> {
        @Override
        public int compare(Delta o1, Delta o2) {
            return ((Long)o1.getN()).compareTo(o2.getN());
        }
    }
}

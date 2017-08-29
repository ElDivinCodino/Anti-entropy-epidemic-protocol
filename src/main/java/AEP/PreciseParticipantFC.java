package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.StartGossip;
import AEP.nodeUtilities.Delta;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class PreciseParticipantFC extends PreciseParticipant{

    private long desiredUR;
    private long maximumUR;

    public PreciseParticipantFC(String destinationPath, int id) {
        super(destinationPath, id);
    }

    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());
        // sender set to null because we do not need to answer to this message
        ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
        getSender().tell(new GossipMessage(false,
                storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method),
                this.desiredUR,
                this.maximumUR), null);
        // send to p the second message containing the digest (NOTE: in the paper it should be just the outdated entries that q requests to p)
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());
        logger.info("Second phase: sending differences + digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());

            // here we calculate the new flow control parameters updating the local maximum update rate
            // the sender update rate gets included in the gossip message to q
            long senderMaximumUR = computeUpdateRate(message.getMaximumUR(), message.getDesiredUR());

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
            getSender().tell(new GossipMessage(false,
                    storage.mtuResizeAndSort(toBeUpdated, mtu , new PreciseComparator(), this.method),
                    0,
                    senderMaximumUR), null);

            logger.info("Fourth phase: sending differences to " + getSender());
        } else {
            // receiving message(s) from q.
            if (getSender() == null) { // this is the message with deltas
                // get the new maximum update rate computed at node p
                this.maximumUR = message.getMaximumUR();
                // update local states with deltas sent by p
                storage.reconciliation(message.getParticipantStates());
                logger.info("Gossip completed");
            } else { // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
                getSender().tell(new GossipMessage(true,
                        storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method),
                        this.desiredUR,
                        this.maximumUR), self());
                logger.info("Third phase: sending differences to " + getSender());
            }
        }
    }

    private long computeUpdateRate(long senderMaximumUR, long senderDesiredUR){
        long oldMax1 = this.maximumUR;
        long oldMax2 = senderMaximumUR;
        long maxRateAvg = (this.maximumUR + senderMaximumUR) / 2;
        if (this.desiredUR + senderDesiredUR <= this.maximumUR + senderMaximumUR){
            long delta = this.desiredUR + senderDesiredUR - this.maximumUR - senderMaximumUR;
            this.maximumUR = this.desiredUR + delta / 2;
            senderMaximumUR = senderDesiredUR + delta / 2;
        }else {  // this.desiredUR + senderDesiredUR > this.maximumUR + senderMaximumUR
            if (this.desiredUR >= maxRateAvg && senderDesiredUR >= maxRateAvg){
                // the participants both get the same value
                this.maximumUR = senderMaximumUR = maxRateAvg;
            }else if (this.desiredUR < maxRateAvg){
                this.maximumUR = this.desiredUR;
                senderMaximumUR = this.maximumUR + senderMaximumUR - this.desiredUR;
            }else{ // senderDesiredUR < maxRateAvg
                senderMaximumUR = senderDesiredUR;
                this.maximumUR = this.maximumUR + senderMaximumUR - senderDesiredUR;
            }
        }
        // this invariant must hold between updates
        assert oldMax1 + oldMax2 == this.maximumUR + senderMaximumUR;
        return senderMaximumUR;
    }

    private class PreciseComparator implements Comparator<Delta> {
        @Override
        public int compare(Delta o1, Delta o2) {
            return ((Long)o1.getN()).compareTo(o2.getN());
        }
    }
}

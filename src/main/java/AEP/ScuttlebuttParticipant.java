package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.StartGossip;
import AEP.messages.TimeoutMessage;
import AEP.nodeUtilities.Delta;
import AEP.nodeUtilities.Utilities;
import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by Francesco on 28/08/17.
 */
public class ScuttlebuttParticipant extends PreciseParticipant {

    public ScuttlebuttParticipant(String destinationPath, int id) {
        super(destinationPath, id);
    }

    // Maximum Transfer Unit: maximum number of deltas inside a single gossip message
    private int mtu = 5;

    public static enum Ordering { OLDEST, NEWEST};
    private PreciseParticipant.Ordering method;

    protected void timeoutMessage(TimeoutMessage message){
        int rndId;
        // choose a random peer excluding self
        do {
            rndId = Utilities.getRandomNum(0, ps.size() - 1);
        } while (rndId == this.id);
        ActorRef q = ps.get(rndId);

        q.tell(new StartGossip(storage.createScuttlebuttDigest()), self());
        logger.debug("Timeout: sending StartGossip to " + q);
        scheduleTimeout(1, TimeUnit.SECONDS);
    }

    protected void startGossip(StartGossip message){
        logger.debug("First phase: Digest from " + getSender());

        // send to p the second message containing own digest
        getSender().tell(new GossipMessage(false, storage.createScuttlebuttDigest()), self());

        logger.debug("Second phase: sending digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeScuttlebuttDifferences(message.getParticipantStates());
            getSender().tell(new GossipMessage(false, storage.mtuResizeAndSort(toBeUpdated, mtu ,this.method)), null);

            logger.debug("Fourth phase: sending differences to " + getSender());
        } else {
            // receiving message(s) from q.
            if (getSender() == null) { // this is the message with deltas
                storage.reconciliation(message.getParticipantStates());
                logger.debug("Gossip completed");
            } else { // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeScuttlebuttDifferences(message.getParticipantStates());
                getSender().tell(new GossipMessage(true, storage.mtuResizeAndSort(toBeUpdated, mtu, this.method)), self());
                logger.debug("Third phase: sending differences to " + getSender());
            }
        }
    }
}

package AEP;

import AEP.messages.*;
import AEP.nodeUtilities.CustomLogger;
import AEP.nodeUtilities.Delta;
import AEP.nodeUtilities.Utilities;
import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Created by Francesco on 28/08/17.
 */
public class ScuttlebuttParticipant extends PreciseParticipant {

    public ScuttlebuttParticipant(int id, CustomLogger.LOG_LEVEL level) {
        super(id, level);
    }

    protected void initValues(SetupMessage message){
        super.initValues(message);
    }

    protected void timeoutMessage(TimeoutMessage message){
        this.increaseTimeStep();
        int rndId;
        // choose a random peer excluding self
        do {
            rndId = Utilities.getRandomNum(0, ps.size() - 1);
        } while (rndId == this.id);
        ActorRef q = ps.get(rndId);

        logger.debug("P " + this.id + " starts gossip with P " + rndId);

        q.tell(new StartGossip(storage.createScuttlebuttDigest()), self());

        if (this.current_timestep != this.timesteps.get(this.timesteps.size()-1)) {
            logger.info("Timeout: sending StartGossip to " + q);
            scheduleTimeout(this.gossipRate, TimeUnit.SECONDS);
        } else {
            System.out.println("stopped process " + this.id);
        }
    }

    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());

        // store the digest of p in a TreeMap, in order to compute the differences later on, in the fourth phase
        storedDigests.put(getSender(), message.getParticipantStates());

        logger.info("Second phase: sending digest to " + getSender());

        // send to p the second message containing own digest
        getSender().tell(new GossipMessage(false, storage.createScuttlebuttDigest(), this.desiredUR, this.updateRate), self());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            logger.info("Fourth phase: reconciling and sending differences to " + getSender());

            storage.reconciliation(message.getParticipantStates(), history, this.current_timestep);

            if (this.flow_control) {
                // get the new maximum update rate computed at node p
                this.updateRate = message.getMaximumUR();
            }

            // answer with the updates p has to do, calculated from the temporary digest stored in the TreeMap.
            ArrayList<Delta> toBeUpdated = storage.computeScuttlebuttDifferences(storedDigests.get(getSender()));

            if (this.desiredUR != 0 && this.flow_control){
                localAdaptation(toBeUpdated.size());
            }

            // Sender set to null because we do not need any answer to this message
            getSender().tell(new GossipMessage(false, storage.mtuResizeAndSort(toBeUpdated, mtu, new ScuttlebuttComparator(), this.method)), null);

        } else {
            // receiving message(s) from q.
            if (getSender() == getContext().system().deadLetters()) { // this is the message with deltas
                storage.reconciliation(message.getParticipantStates(), history, this.current_timestep);

                logger.info("Reconciliation... Gossip completed");
            } else { // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeScuttlebuttDifferences(message.getParticipantStates());

                float senderupdateRate = 0;
                if (this.flow_control){
                    // here we calculate the new flow control parameters updating the local maximum update rate
                    // the sender update rate gets included in the gossip message to q
                    if (this.desiredUR == 0){
                        // TODO: check this
                        senderupdateRate = 0;
                    }else {
                        senderupdateRate = computeUpdateRate(message.getMaximumUR(), message.getDesiredUR());
                    }
                }

                if (this.desiredUR != 0 && this.flow_control){
                    localAdaptation(toBeUpdated.size());
                }

                getSender().tell(new GossipMessage(true,
                        storage.mtuResizeAndSort(toBeUpdated, mtu, new ScuttlebuttComparator(), this.method),
                        0,
                        senderupdateRate), self());

                logger.info("Third phase: sending differences to " + getSender());
            }
        }
    }

    /**
     * Comparator for Scuttlebutt: first order from the older to the newer, than in case of same version, order by participant
     */
    private class ScuttlebuttComparator implements Comparator<Delta> {
        int c;

        @Override
        public int compare(Delta o1, Delta o2) {
            c =((Integer)o1.getP()).compareTo(o2.getP());

            if(c == 0)
                c = (((Long)o1.getN()).compareTo(o2.getN()));

            return c;
        }
    }
}

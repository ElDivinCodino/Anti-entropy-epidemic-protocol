package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.ObserverUpdate;
import AEP.messages.SetupMessage;
import AEP.messages.StartGossip;
import AEP.nodeUtilities.CustomLogger;
import AEP.nodeUtilities.Delta;
import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class PreciseParticipant extends Participant {

    // Maximum Transfer Unit: maximum number of deltas inside a single gossip message
    protected int mtu;
    private List<Integer> mtuArray;

    protected TreeMap<ActorRef, ArrayList<Delta>> storedDigests;

    public enum Ordering { OLDEST, NEWEST, SCUTTLEBREADTH, SCUTTLEDEPTH};
    protected Ordering method;

    public PreciseParticipant(int id, CustomLogger.LOG_LEVEL level) {
        super(id, level);
    }

    protected void initValues(SetupMessage message){
        this.mtuArray = message.getMtu();
        // get first value as starting MTU
        this.mtu = mtuArray.get(0);
        this.method = message.getOrdering();
        super.initValues(message);
        this.storedDigests = new TreeMap<>();
    }

    protected void changeMTU(){
        if (this.current_timestep == this.timesteps.get(this.current_timestep_index)){
            this.mtu = this.mtuArray.get(this.current_timestep_index);
            System.out.println("MTU changed to " + this.mtu);
        }
    }

    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());

        // store the digest of p in a TreeMap, in order to compute the differences later on, in the fourth phase
        storedDigests.put(getSender(), message.getParticipantStates());

        // send to p the second message containing own digest
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());

        logger.info("Second phase: sending digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            ArrayList<Delta> reconciled = storage.reconciliation(message.getParticipantStates());
            observer.tell(new ObserverUpdate(this.id, this.current_timestep, reconciled, false), getSelf());

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeDifferences(this.storedDigests.get(getSender()));
            getSender().tell(new GossipMessage(false, storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method)), null);

            logger.info("Fourth phase: sending differences to " + getSender());
        } else {
            // receiving message(s) from q.
            if (getSender() == getContext().system().deadLetters()) { // this is the message with deltas
                ArrayList<Delta> reconciled = storage.reconciliation(message.getParticipantStates());
                observer.tell(new ObserverUpdate(this.id, this.current_timestep, reconciled, false), getSelf());

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

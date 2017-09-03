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
import java.util.concurrent.TimeUnit;

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

    private int phiBiggerMax;
    private int phiSmallerMax;
    private int phiBiggerCounter;
    private int phiSmallerCounter;

    private float alpha;
    private float beta;
    // ----------------------

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

        if (this.flow_control){
            this.alpha = message.getAlpha();
            this.beta = message.getBeta();
            this.phiBiggerMax = message.getPhi1();
            this.phiSmallerMax = message.getPhi2();

            this.desiredUR = this.updaterates.get(0);
        }
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
        getSender().tell(new GossipMessage(false, storage.createDigest(), this.desiredUR, this.updateRate), self());

        logger.info("Second phase: sending digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        // p sent to q the updates
        if (message.isSender()) {
            ArrayList<Delta> reconciled = storage.reconciliation(message.getParticipantStates());
            observer.tell(new ObserverUpdate(this.id, this.current_timestep, reconciled, false), getSelf());

            if (this.flow_control) {
                // in case we were not updating before and the new updateRate is > 0. Need to start updating again.
                if (this.updateRate == 0 && message.getMaximumUR() > 0){
                    scheduleUpdateTimeout(Math.round(1000/message.getMaximumUR()), TimeUnit.MILLISECONDS);
                }
                // get the new maximum update rate computed at node p
                this.updateRate = message.getMaximumUR();
            }

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeDifferences(this.storedDigests.get(getSender()));

            if (this.desiredUR != 0 && this.flow_control){
                localAdaptation(toBeUpdated.size());
            }

            getSender().tell(new GossipMessage(false,
                    storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method)), null);

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
                        storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method),
                        0,
                        senderupdateRate), self());

                logger.info("Third phase: sending differences to " + getSender());
            }
        }
    }

    protected float computeUpdateRate(float senderupdateRate, float senderDesiredUR){
        float oldMax1 = this.updateRate;
        float oldMax2 = senderupdateRate;
        float maxRateAvg = (this.updateRate + senderupdateRate) / 2;
        if (this.desiredUR + senderDesiredUR <= this.updateRate + senderupdateRate){
            float delta = this.desiredUR + senderDesiredUR - this.updateRate - senderupdateRate;
            this.updateRate = this.desiredUR + delta / 2;
            senderupdateRate = senderDesiredUR + delta / 2;
        }else {  // this.desiredUR + senderDesiredUR > this.updateRate + sender updateRate
            if (this.desiredUR >= maxRateAvg && senderDesiredUR >= maxRateAvg){
                // the participants both get the same value
                this.updateRate = senderupdateRate = maxRateAvg;
            }else if (this.desiredUR < maxRateAvg){
                this.updateRate = this.desiredUR;
                senderupdateRate = this.updateRate + senderupdateRate - this.desiredUR;
            }else{ // senderDesiredUR < maxRateAvg
                senderupdateRate = senderDesiredUR;
                this.updateRate = this.updateRate + senderupdateRate - senderDesiredUR;
            }
        }
        // this invariant must hold between updates
        assert oldMax1 + oldMax2 == this.updateRate + senderupdateRate;

        // in case we were not updating before and the new updateRate is > 0. Need to start updating again.
        if (oldMax1 == 0 && this.updateRate > 0){
            scheduleUpdateTimeout(Math.round(1000/this.updateRate), TimeUnit.MILLISECONDS);
        }

        return senderupdateRate;
    }

    protected void localAdaptation(int messageSize){
        float prev = this.updateRate;
        if (messageSize > this.mtu) {
            this.phiBiggerCounter++;
            // reset smaller counter
            this.phiSmallerCounter = 0;
        }else if (messageSize < this.mtu){
            this.phiSmallerCounter++;
            this.phiBiggerCounter = 0;
        }

        // check if a counter has surpassed the threshold
        if (this.phiBiggerCounter >= this.phiBiggerMax){
            this.updateRate = this.alpha * this.updateRate;
        }
        if (this.phiSmallerCounter >= this.phiSmallerMax){
            this.updateRate = Math.min(this.updateRate + this.beta, mtu);
        }
        // in case we were not updating before and the new updateRate is > 0. Need to start updating again.
        if (prev == 0 && this.updateRate > 0){
            scheduleUpdateTimeout(Math.round(1000/this.updateRate), TimeUnit.MILLISECONDS);
        }
    }

    private class PreciseComparator implements Comparator<Delta> {
        @Override
        public int compare(Delta o1, Delta o2) {
            return ((Long)o1.getN()).compareTo(o2.getN());
        }
    }
}

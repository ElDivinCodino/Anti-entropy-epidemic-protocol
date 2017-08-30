package AEP;

import AEP.messages.GossipMessage;
import AEP.messages.SetupMessage;
import AEP.messages.StartGossip;
import AEP.nodeUtilities.Delta;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class PreciseParticipantFC extends PreciseParticipant{

    // local adaptation variables
    private int phiBiggerMax;
    private int phiSmallerMax;
    private int phiBiggerCounter;
    private int phiSmallerCounter;

    private float alpha;
    private float beta;

    public PreciseParticipantFC(int id) {
        super(id);
    }

    protected void initValues(SetupMessage message){
        super.initValues(message);
        this.alpha = message.getAlpha();
        this.beta = message.getBeta();
        this.phiBiggerMax = message.getPhi1();
        this.phiSmallerMax = message.getPhi2();

        this.desiredUR = this.updaterates.get(0);
    }

    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());
        // sender set to null because we do not need to answer to this message
        ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());
        getSender().tell(new GossipMessage(false,
                storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method),
                this.desiredUR,
                this.updateRate), null);
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
            float senderupdateRate;
            if (this.desiredUR == 0){
                // TODO: check this
                senderupdateRate = 0;
            }else {
                senderupdateRate = computeUpdateRate(message.getMaximumUR(), message.getDesiredUR());
            }

            // answer with the updates p has to do. Sender set to null because we do not need to answer to this message
            ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());

            if (this.desiredUR != 0){
                localAdaptation(toBeUpdated.size());
            }

            getSender().tell(new GossipMessage(false,
                    storage.mtuResizeAndSort(toBeUpdated, mtu , new PreciseComparator(), this.method),
                    0,
                    senderupdateRate), null);

            logger.info("Fourth phase: sending differences to " + getSender());
        } else {
            // receiving message(s) from q.
            if (getSender() == getContext().system().deadLetters()) { // this is the message with deltas
                // get the new maximum update rate computed at node p
                this.updateRate = message.getMaximumUR();
                // update local states with deltas sent by p
                storage.reconciliation(message.getParticipantStates());
                logger.info("Gossip completed");
            } else { // digest message to respond to
                // send to q last message of exchange with deltas.
                ArrayList<Delta> toBeUpdated = storage.computeDifferences(message.getParticipantStates());

                if (this.desiredUR != 0){
                    localAdaptation(toBeUpdated.size());
                }

                getSender().tell(new GossipMessage(true,
                        storage.mtuResizeAndSort(toBeUpdated, mtu, new PreciseComparator(), this.method),
                        this.desiredUR,
                        this.updateRate), self());
                logger.info("Third phase: sending differences to " + getSender());
            }
        }
    }

    private float computeUpdateRate(float senderupdateRate, float senderDesiredUR){
        float oldMax1 = this.updateRate;
        float oldMax2 = senderupdateRate;
        float maxRateAvg = (this.updateRate + senderupdateRate) / 2;
        if (this.desiredUR + senderDesiredUR <= this.updateRate + senderupdateRate){
            float delta = this.desiredUR + senderDesiredUR - this.updateRate - senderupdateRate;
            this.updateRate = this.desiredUR + delta / 2;
            senderupdateRate = senderDesiredUR + delta / 2;
        }else {  // this.desiredUR + senderDesiredUR > this.updateRate + senderupdateRate
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
        return senderupdateRate;
    }

    private void localAdaptation(int messageSize){
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
    }

    private class PreciseComparator implements Comparator<Delta> {
        @Override
        public int compare(Delta o1, Delta o2) {
            return ((Long)o1.getN()).compareTo(o2.getN());
        }
    }
}

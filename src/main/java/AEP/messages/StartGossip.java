package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Francesco on 25/08/17.
 */
public final class StartGossip implements Serializable{

    private ArrayList<Delta> participantStates;

    // flow control parameters
    long desiredUR;
    long maximumUR;

    public StartGossip(ArrayList<Delta> participantStates) {
        this.participantStates = participantStates;
    }

    public StartGossip(ArrayList<Delta> participantStates, long desiredUR, long maximumUR) {
        this.participantStates = participantStates;
        this.desiredUR = desiredUR;
        this.maximumUR = maximumUR;
    }

    /**
     *
     * @return the TreeMap representing the digest that the starter of the gossip process wants to gossip to its peer
     */
    public ArrayList<Delta> getParticipantStates() {
        return participantStates;
    }
}

package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Francesco on 25/08/17.
 */
public final class StartGossip implements Serializable{

    private ArrayList<Delta> participantStates;

    public StartGossip(ArrayList<Delta> participantStates) {
        this.participantStates = participantStates;
    }

    /**
     *
     * @return the TreeMap representing the digest that the starter of the gossip process wants to gossip to its peer
     */
    public ArrayList<Delta> getParticipantStates() {
        return participantStates;
    }
}

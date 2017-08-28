package AEP.messages;

import AEP.Delta;
import AEP.nodeUtilities.Couple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by Francesco on 25/08/17.
 */
public class StartGossip implements Serializable{

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

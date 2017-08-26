package AEP.messages;

import AEP.nodeUtilities.Couple;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Created by Francesco on 25/08/17.
 */
public class StartGossip implements Serializable{

    private TreeMap<Integer, TreeMap<Integer, Couple>> participantStates;

    public StartGossip(TreeMap<Integer, TreeMap<Integer, Couple>> participantStates) {
        this.participantStates = participantStates;
    }

    /**
     *
     * @return the TreeMap representing the digest that the starter of the gossip process wants to gossip to its peer
     */
    public TreeMap<Integer, TreeMap<Integer, Couple>> getParticipantStates() {
        return participantStates;
    }
}

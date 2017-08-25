package AEP.messages;

import AEP.nodeUtilities.Couple;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Created by Francesco on 25/08/17.
 */
public class GossipMessage implements Serializable{


    private boolean isSender;
    private TreeMap<Integer, TreeMap<Integer, Couple>> participantStates;

    public GossipMessage(boolean isSender, TreeMap<Integer, TreeMap<Integer, Couple>> states) {
        this.isSender = isSender;
        this.participantStates = states;
    }

    public TreeMap<Integer, TreeMap<Integer, Couple>> getParticipantStates() {
        return participantStates;
    }

    public boolean isSender() {
        return isSender;
    }
}

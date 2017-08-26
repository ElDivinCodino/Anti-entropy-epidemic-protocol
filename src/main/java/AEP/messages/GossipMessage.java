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

    /**
     *
     * @return the TreeMap one wants to gossip to its peer
     */
    public TreeMap<Integer, TreeMap<Integer, Couple>> getParticipantStates() {
        return participantStates;
    }

    /**
     *
     * @return true if the sender of the message is the one who started the gossip process, false otherwise
     */
    public boolean isSender() {
        return isSender;
    }
}

package AEP.messages;

import AEP.Delta;
import AEP.nodeUtilities.Couple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by Francesco on 25/08/17.
 */
public class GossipMessage implements Serializable{


    private boolean isSender;
    private ArrayList<Delta> participantStates;

    public GossipMessage(boolean isSender, ArrayList<Delta> states) {
        this.isSender = isSender;
        this.participantStates = states;
    }

    /**
     *
     * @return the TreeMap one wants to gossip to its peer
     */
    public ArrayList<Delta> getParticipantStates() {
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

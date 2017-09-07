package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Francesco on 25/08/17.
 */
public final class GossipMessage implements Serializable{


    private boolean isSender;
    private ArrayList<Delta> participantStates;

    // flow control parameters
    float desiredUR;
    float maximumUR;

    public GossipMessage(boolean isSender, ArrayList<Delta> states) {
        this.isSender = isSender;
        this.participantStates = states;
    }

    public GossipMessage(boolean isSender, ArrayList<Delta> states, float desiredUR, float maximumUR) {
        this.isSender = isSender;
        this.participantStates = states;
        this.desiredUR = desiredUR;
        this.maximumUR = maximumUR;
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

    public float getDesiredUR() {
        return desiredUR;
    }

    public float getMaximumUR() {
        return maximumUR;
    }
}

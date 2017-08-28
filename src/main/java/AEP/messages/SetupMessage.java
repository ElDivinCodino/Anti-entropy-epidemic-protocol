package AEP.messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Francesco on 24/08/17.
 */
public class SetupMessage implements Serializable {

    private int couplesNumber;
    private List<ActorRef> ps;

    public SetupMessage(int couplesNumber, List participants) {
        this.couplesNumber = couplesNumber;
        ps = participants;
    }

    public List<ActorRef> getParticipants() {
        return ps;
    }

    public int getCouplesNumber() {
        return couplesNumber;
    }
}

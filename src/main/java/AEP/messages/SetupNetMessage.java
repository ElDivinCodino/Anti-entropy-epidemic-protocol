package AEP.messages;

import java.io.Serializable;

/**
 * Created by Francesco on 24/08/17.
 */
public class SetupNetMessage implements Serializable {

    private int participantsNumber, couplesNumber;

    public SetupNetMessage(int participantsNumber, int couplesNumber) {
        this.participantsNumber = participantsNumber;
        this.couplesNumber = couplesNumber;
    }

    public int getParticipantsNumber() {
        return participantsNumber;
    }

    public int getCouplesNumber() {
        return couplesNumber;
    }
}

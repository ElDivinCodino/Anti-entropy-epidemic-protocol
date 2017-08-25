package AEP.nodeUtilities;

import java.io.*;
import java.util.*;

/**
 * The storage where all the states of a participant are stored
 */
public class Storage {

    private ArrayList<ArrayList<Couple>> participantStates = new ArrayList<>();

    private String pathname;
    private int participantsNumber, couplesNumber;

    public Storage(String pathname, int participantsNumber, int couplesNumber) {
        this.pathname = pathname;
        this.participantsNumber = participantsNumber;
        this.couplesNumber = couplesNumber;
        initializeStates(participantsNumber, couplesNumber);
    }

    private void initializeStates(int n, int p) {

        // initialize participants' states
        for(int i = 0; i < n; i++) {
            participantStates.add(new ArrayList<Couple>());
            for(int j = 0; j < p; j++) {
                participantStates.get(i).add(new Couple(null, 0));
            }
        }
        // save the items to disk
        save();
    }

    /**
     * updates a pair in the NodeUtilities.Storage
     *
     * @param p the participant who is responsible for the Couple
     * @param key the key of the Couple
     * @param value the updated value of the Couple
     */
    public void update(String p, int key, String value, int version) {

        int participantNumber = getParticipantNumber(p);
        Couple couple = participantStates.get(participantNumber).get(key);

        couple.setValue(value);
        couple.setVersion(version);

        // save the items to disk
        save();
    }

    private int findNextVersion() {
        return 0;
    }

    /**
     *
     * @param p the entire name of the participant
     * @return the int which corresponds to the participant
     */
    private int getParticipantNumber(String p) {
        String stringNumber = p.substring(12); // delete the "Participant_" part
        return Integer.parseInt(stringNumber);
    }

    /**
     * saves the storage on a local text file
     */
    private void save() {
        try {
            FileWriter out = new FileWriter(pathname);
            out.write(this.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  overrides the java.lang.Object.toString() method, useful to manage the representation of the entire NodeUtilities.Storage
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(/*DynamoLogger.ANSI_WHITE + */"Storage: \n"/* + DynamoLogger.ANSI_RESET*/);
        sb.append("\tParticipant name|");
        for(int j = 0; j < couplesNumber; j++) {
            sb.append("\tKey " + j + "\t|");
        }
        sb.append("");
        for(int i = 0; i < participantsNumber; i++) {
            sb.append("\n\tParticipant_" + i + " \t|");
            for(int j = 0; j < couplesNumber; j++) {
                sb.append(" " + participantStates.get(i).get(j) + " |");
            }
        }

        return sb.toString();
    }

}

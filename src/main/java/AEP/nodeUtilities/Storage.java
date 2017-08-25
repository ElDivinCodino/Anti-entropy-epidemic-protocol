package AEP.nodeUtilities;

import java.io.*;
import java.util.*;

/**
 * The storage where all the states of a participant are stored
 */
public class Storage {

    private TreeMap<Integer, TreeMap<Integer, Couple>> participantStates = new TreeMap<>();

    private String pathname;
    private int participantsNumber, couplesNumber, id;

    public Storage(String pathname, int participantsNumber, int couplesNumber, int id) {
        this.pathname = pathname;
        this.participantsNumber = participantsNumber;
        this.couplesNumber = couplesNumber;
        this.id = id;
        initializeStates(participantsNumber, couplesNumber);
    }

    private void initializeStates(int n, int p) {

        Random r = new Random();

        // initialize participants' states
        for(int i = 0; i < n; i++) {
            TreeMap<Integer, Couple> newTreemap = new TreeMap<>();

            for(int j = 0; j < p; j++) {
                if (i == id) {
                    newTreemap.put(j, new Couple(Utilities.getRandomNum(0, 1000).toString(), 1));
                } else {
                    newTreemap.put(j, new Couple(null, 0));
                }
            }

            participantStates.put(i, newTreemap);
        }
        // save the items to disk
        save();
    }

    public TreeMap<Integer, TreeMap<Integer, Couple>> createDigest() {

        TreeMap<Integer, TreeMap<Integer, Couple>> digest = (TreeMap<Integer, TreeMap<Integer, Couple>>) participantStates.clone();
        for (Map.Entry<Integer, TreeMap<Integer, Couple>> participantStates : digest.entrySet()) {
            for (Map.Entry<Integer, Couple> state : participantStates.getValue().entrySet()) {
                state.getValue().setValue(null);
            }
        }

        return digest;
    }

    public void reconciliation (TreeMap<Integer, TreeMap<Integer, Couple>> peerStates) {

        for (Map.Entry<Integer, TreeMap<Integer, Couple>> state : peerStates.entrySet()) {
            for (Map.Entry<Integer, Couple> delta : state.getValue().entrySet()) {
                Couple couple = delta.getValue();
                if (couple.getVersion() > participantStates.get(state.getKey()).get(delta.getKey()).getVersion()) {
                    System.out.println("Replacing " + participantStates.get(state.getKey()).get(delta.getKey()) + " with "+ couple);
                    participantStates.get(state.getKey()).get(delta.getKey()).updateCouple(couple);
                }
            }
        }
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

        sb.append(/*CustomLogger.ANSI_WHITE + */"Storage: \n"/* + CustomLogger.ANSI_RESET*/);
        sb.append("\tParticipant name|");
        for(int j = 0; j < couplesNumber; j++) {
            sb.append("\tKey " + j + "\t|");
        }
        sb.append("\n");
        sb.append(participantStates.toString());

        return sb.toString();
    }

    public TreeMap<Integer, TreeMap<Integer, Couple>> computeDifferences(TreeMap<Integer, TreeMap<Integer, Couple>> digest) {

        TreeMap<Integer, TreeMap<Integer, Couple>> toBeUpdated = (TreeMap<Integer, TreeMap<Integer, Couple>>) participantStates.clone();

        for (Map.Entry<Integer, TreeMap<Integer, Couple>> state : digest.entrySet()) {
            for (Map.Entry<Integer, Couple> delta : state.getValue().entrySet()) {
                Couple couple = delta.getValue();
                // OPTIMIZATION: greater or equal instead of only greater because otherwise we send also couples with the same version number
                if (couple.getVersion() >= participantStates.get(state.getKey()).get(delta.getKey()).getVersion()) {
                    toBeUpdated.get(state.getKey()).get(delta.getKey()).updateCouple(new Couple(null, 0));
                }
            }
        }

        return toBeUpdated;
    }
}

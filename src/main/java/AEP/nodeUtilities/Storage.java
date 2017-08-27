package AEP.nodeUtilities;

import com.rits.cloning.Cloner;

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

    /**
     * This method provides the initialization of the data structure responsible for storing the items
     * @param n number of participants
     * @param p number of pairs for each participant
     */
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
        System.out.println(id + ": " + participantStates);
        // save the items to disk
        save();
    }

    /**
     * This method performs the digest of the storage of the participant
     * @return a TreeMap containing the states of the participants with null values
     */
    public TreeMap<Integer, TreeMap<Integer, Couple>> createDigest() {
        Cloner cloner = new Cloner();
        TreeMap<Integer, TreeMap<Integer, Couple>> digest = cloner.deepClone(participantStates);
        for (Map.Entry<Integer, TreeMap<Integer, Couple>> pStates : digest.entrySet()) {
            for (Map.Entry<Integer, Couple> delta : pStates.getValue().entrySet()) {
                delta.getValue().setValue(null);
            }
        }
        return digest;
    }

    /**
     * This method performs the reconciliation, or merging
     * i.e. updates those keys obtained from the peer that are newer w.r.t the ones it already owned
     * @param peerStates a TreeMap which has null value if the participant should not be interested in updating that key,
     *                   or a new value with an higher version number instead
     */
    public void reconciliation (TreeMap<Integer, TreeMap<Integer, Couple>> peerStates) {

        for (Map.Entry<Integer, TreeMap<Integer, Couple>> state : peerStates.entrySet()) {
            for (Map.Entry<Integer, Couple> delta : state.getValue().entrySet()) {
                Couple couple = delta.getValue();
                if (couple.getVersion() > participantStates.get(state.getKey()).get(delta.getKey()).getVersion()) {
                    participantStates.get(state.getKey()).get(delta.getKey()).updateCouple(couple);
                }
            }
        }
        save();
    }

    /**
     * TODO: will be useful further on
     * @return the next integer greater than the maximum
     */
    private int findNextVersion() {
        return 0;
    }

    /**
     * this method saves the storage on a local text file
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
     * This method provides the computation of the differences between the received digest and the current states.
     * If the received digest has Couples with higher version, it means that the peer knows a needed update; otherwise inform
     * the peer to not be interested in that information by setting its value to null
     * @param digest the digest coming from the other peer
     * @return a TreeMap indicating the values which needs to be updated
     */
    public TreeMap<Integer, TreeMap<Integer, Couple>> computeDifferences(TreeMap<Integer, TreeMap<Integer, Couple>> digest) {

        Cloner cloner = new Cloner();
        TreeMap<Integer, TreeMap<Integer, Couple>> toBeUpdated= cloner.deepClone(participantStates);
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

    /**
     *  This method overrides the java.lang.Object.toString() method, useful to manage the representation of the entire NodeUtilities.Storage
     *  @return a String which is a representation of the Storage current status
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Storage for p=").append(this.id).append(": \n");
        sb.append("\tP|");
        for(int j = 0; j < couplesNumber; j++) {
            sb.append("\tKey ").append(j).append("\t|");
        }
//        sb.append(participantStates.toString());
        for (Map.Entry<Integer, TreeMap<Integer, Couple>> state : participantStates.entrySet()) {
            sb.append("\n\t").append(state.getKey()).append("\t");
            for (Map.Entry<Integer, Couple> delta : state.getValue().entrySet()) {
                sb.append(delta.getValue()).append("\t");
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}

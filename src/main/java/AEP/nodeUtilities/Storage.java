package AEP.nodeUtilities;

import AEP.Delta;
import AEP.PreciseParticipant;
import com.rits.cloning.Cloner;

import java.io.*;
import java.util.*;

import static AEP.PreciseParticipant.Ordering;

/**
 * The storage where all the states of a participant are stored
 */
public class Storage {

    private ArrayList<Delta> participantStates = new ArrayList<>();

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

        Delta tmp;
        // initialize participants' states
        for(int i = 0; i < n; i++) {
            for(int j = 0; j < p; j++) {
                if (i == id) {
                    tmp = new Delta(i, j, Utilities.getRandomNum(0, 1000).toString(), System.currentTimeMillis());
                } else {
                    tmp = new Delta(i, j, null, 0);
                }
                participantStates.add(tmp);
            }
        }
        System.out.println(id + ": " + participantStates);
        // save the items to disk
        save();
    }

    /**
     * This method performs the digest of the storage of the participant
     * @return a TreeMap containing the states of the participants with null values
     */
    public ArrayList<Delta> createDigest() {
        ArrayList<Delta> digest = new ArrayList<>();
        for (Delta d : this.participantStates){
            digest.add(new Delta(d.getP(), d.getK(), null, d.getN()));
        }
        return digest;
    }

    /**
     * This method performs the reconciliation, or merging
     * i.e. updates those keys obtained from the peer that are newer w.r.t the ones it already owned
     * @param peerStates a TreeMap which has null value if the participant should not be interested in updating that key,
     *                   or a new value with an higher version number instead
     */
    public void reconciliation (ArrayList<Delta> peerStates) {

        System.out.println("peerStates:" + peerStates);
        int index = 0;
        for (Delta d : peerStates){
            for (; index < this.participantStates.size(); index++) {
                if (d.getP() == participantStates.get(index).getP() &&
                        d.getK() == participantStates.get(index).getK() &&
                        d.getN() >= participantStates.get(index).getN()) {
                    participantStates.get(index).setV(d.getV());
                    participantStates.get(index).setN(d.getN());
                    // increase index of local state and exit current for loop
                    // to get the next delta
                    break;
                }
            }
        }
        System.out.println(participantStates);
        save();
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
    public ArrayList<Delta> computeDifferences(ArrayList<Delta> digest) {
        ArrayList<Delta> toBeUpdated= new ArrayList<>();

        int index = 0;
        for (Delta d : digest){
            for (; index < this.participantStates.size(); index++) {
                if (d.getP() == participantStates.get(index).getP() &&
                        d.getK() == participantStates.get(index).getK() &&
                        // OPTIMIZATION: greater or equal instead of only greater
                        // because otherwise we send also couples with the same version number
                        d.getN() < participantStates.get(index).getN()) {
                    toBeUpdated.add(new Delta(
                            participantStates.get(index).getP(),
                            participantStates.get(index).getK(),
                            participantStates.get(index).getV(),
                            participantStates.get(index).getN()));
                    // increase index of local state and exit current for loop
                    // to get the next delta
                    break;
                }
            }
        }

        return toBeUpdated;
    }

    public ArrayList<Delta> mtuResizeAndSort(ArrayList<Delta> state, Ordering method){
        if (method == Ordering.OLDEST) { // ascending order (first is smallest timestamp)

        }else { // descending order (first is newest timestamp)

        }
        return null;
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
        long currentP = -1;
        for (Delta d : this.participantStates){
            if (d.getP() != currentP){
                currentP = d.getP();
                sb.append("\n\t").append(currentP).append("\t");
            }
            sb.append("(").append(d.getV()).append(", ").append(d.getN()).append(")\t");
        }
        sb.append("\n");
        return sb.toString();
    }
}

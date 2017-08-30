package AEP.nodeUtilities;

import java.io.*;
import java.util.*;

import static AEP.PreciseParticipant.Ordering;

/**
 * The storage where all the states of a participant are stored
 */
public class Storage {

    private ArrayList<Delta> participantStates = new ArrayList<>();
    private CustomLogger logger;
    private String pathname;
    private int tuplesNumber;
    private int participantNumber;
    private int id;

    public Storage(String pathname, int participantsNumber, int tuplesNumber, int id, CustomLogger logger) {
        this.pathname = pathname;
        this.tuplesNumber = tuplesNumber;
        this.participantNumber = participantsNumber;
        this.id = id;
        this.logger = logger;
        initializeStates(participantsNumber, tuplesNumber);
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
        // save the items to disk
        save();
    }


    public void update(int key, String value) {
        Delta deltaToBeUpdated = participantStates.get((id * tuplesNumber) + key);
        deltaToBeUpdated.setV(value);
        deltaToBeUpdated.setN(System.currentTimeMillis());

        logger.debug("P " + id + " updated key " + key + " with v: " + value + " t: " + deltaToBeUpdated.getN());

        save();
    }

    /**
     * This method performs the digest of the storage of the participant
     * @return a ArrayList containing the states of the participants with null values
     */
    public ArrayList<Delta> createDigest() {
        ArrayList<Delta> digest = new ArrayList<>();
        for (Delta d : this.participantStates){
            digest.add(new Delta(d.getP(), d.getK(), null, d.getN()));
        }
        return digest;
    }

    /**
     * This method performs the digest of the storage of the participant following Scuttlebutt directives
     * @return a ArrayList containing the maximum version number for each participant
     */
    public ArrayList<Delta> createScuttlebuttDigest() {
        ArrayList<Delta> digest = new ArrayList<>();

        for (int i = 0; i < participantNumber; i++){
            long higherVersion = -1;
            long key = -1;

            for (int j = i * tuplesNumber; j < (1 + i) * tuplesNumber; j++ ) {
                if (participantStates.get(j).getN() > higherVersion) {
                    higherVersion = participantStates.get(j).getN();
                    key = participantStates.get(j).getK();
                }
            }
            digest.add(new Delta(i, key, null, higherVersion));
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

        logger.debug("Reconciliation peerStates:" + peerStates);
        for (Delta d : peerStates){
            for (int index = 0; index < this.participantStates.size(); index++) {
                if (d.getP() == participantStates.get(index).getP() &&
                        d.getK() == participantStates.get(index).getK() &&
                        d.getN() > participantStates.get(index).getN()) {
                    participantStates.get(index).setV(d.getV());
                    participantStates.get(index).setN(d.getN());
                    // increase index of local state and exit current for loop
                    // to get the next delta
                    break;
                }
            }
        }
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
     * TODO: controlla il comment, ci sono degli errori di legacy
     * This method provides the computation of the differences between the received digest and the current states.
     * If the received digest has tuples with higher version, it means that the peer knows a needed update; otherwise inform
     * the peer to not be interested in that information by setting its value to null
     * @param digest the digest coming from the other peer
     * @return a TreeMap indicating the values which needs to be updated
     */
    public ArrayList<Delta> computeDifferences(ArrayList<Delta> digest) {
        ArrayList<Delta> toBeUpdated= new ArrayList<>();

        for (Delta d : digest){
            for (int index = 0; index < this.participantStates.size(); index++) {
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

    /**
     * This method provides the computation of the differences between the received digest and the current states following Scuttlebutt directives:.
     * If the received digest has tuples with maximum version lower than a value, it means that the peer needs to know some the keys that have higher version
     * for that specific participant and so need to be updated
     * @param digest the digest coming from the other peer
     * @return a TreeMap indicating the values which needs to be updated
     */
    public ArrayList<Delta> computeScuttlebuttDifferences(ArrayList<Delta> digest) {
        ArrayList<Delta> toBeUpdated= new ArrayList<>();
        for (int i = 0; i < digest.size(); i++){
            for (int index = i * tuplesNumber; index < tuplesNumber * (i + 1); index++) {
                if (digest.get(i).getP() == participantStates.get(index).getP() &&
                        digest.get(i).getN() < participantStates.get(index).getN()) {
                    toBeUpdated.add(new Delta(
                            participantStates.get(index).getP(),
                            participantStates.get(index).getK(),
                            participantStates.get(index).getV(),
                            participantStates.get(index).getN()));
                    // increase index of local state and exit current for loop
                    // to get the next delta
                }
            }
        }
        return toBeUpdated;
    }

    private TreeMap<Long, ArrayList<Delta>> statesToTreeMap(ArrayList<Delta> states){
        // represents how many deltas are available for each process.
        // Key is the process id, value is the number of deltas
        TreeMap<Long, ArrayList<Delta>> numberOfDeltas = new TreeMap<>();
        for (Delta d: states) {
            long p = d.getP();

            if (numberOfDeltas.containsKey(p)) {
                numberOfDeltas.get(p).add(d);
            } else {
                ArrayList<Delta> newArray = new ArrayList<>();
                newArray.add(d);
                numberOfDeltas.put(p, newArray);
            }
        }
        return numberOfDeltas;
    }

    public ArrayList<Delta> mtuResizeAndSort(ArrayList<Delta> state, int mtuSize, Comparator comparator, Ordering method) {

        if (state.size() <= mtuSize){
            return state;
        }
        ArrayList<Delta> mtuArrayList = new ArrayList<>();

        Collections.sort(state, comparator);

        if (method == Ordering.OLDEST) { // ascending order (first is smallest timestamp)
            mtuArrayList.addAll(state.subList(0, mtuSize));
        } else if (method == Ordering.NEWEST) { // descending order (first is newest timestamp)
            mtuArrayList.addAll(state.subList(state.size() - mtuSize - 1, state.size()));
            Collections.reverse(mtuArrayList);
        } else if (method == Ordering.SCUTTLEBREADTH) {
            TreeMap<Long, ArrayList<Delta>> numberOfDeltas = statesToTreeMap(state);
            ArrayList<Long> randomP = new ArrayList<>(numberOfDeltas.keySet());
            Collections.shuffle(randomP);
            int filled = 0;
            while (filled < mtuSize){
                for (Long i : randomP){
                    if (filled == mtuSize)
                        break;
                    if (numberOfDeltas.get(i).size() > 0) {
                        mtuArrayList.add(numberOfDeltas.get(i).get(0));
                        numberOfDeltas.get(i).remove(0);
                        filled++;
                    }
                }
            }
        } else if (method == Ordering.SCUTTLEDEPTH) {
            int randomOrder = Utilities.getRandomNum(0, 1);
            TreeMap<Long, ArrayList<Delta>> numberOfDeltas = statesToTreeMap(state);

            // while loop to decide which delta's to insert
            while (mtuArrayList.size() < mtuSize && numberOfDeltas.size() > 0) {
                int maxv = -1; // number of deltas of maximum process
                long maxp = 0;  // key of the maximum process

                for(Long i : numberOfDeltas.keySet()) {
                    if (numberOfDeltas.get(i).size() > maxv || (numberOfDeltas.get(i).size() == maxv && randomOrder == 0)) { // For participants with the same number of available deltas, random ordering among participants is used to remove bias
                        maxv = numberOfDeltas.get(i).size();
                        maxp = i;
                    }
                }

                for(int i = mtuArrayList.size(); i < mtuSize && i < numberOfDeltas.get(maxp).size(); i++) {
                    mtuArrayList.add(numberOfDeltas.get(maxp).get(i));
                }

                numberOfDeltas.remove(maxp);
            }
        }
        return mtuArrayList;
    }

    /**
     *  This method overrides the java.lang.Object.toString() method, useful to manage the representation of the entire NodeUtilities.Storage
     *  @return a String which is a representation of the Storage current status
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Storage for p=").append(this.id).append(": \n");
        sb.append("\tP|");
        for(int j = 0; j < tuplesNumber; j++) {
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

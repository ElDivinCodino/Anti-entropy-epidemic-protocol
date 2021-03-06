package AEP.nodeUtilities;

import java.io.*;
import java.util.*;

import static AEP.PreciseParticipant.Ordering;

/**
 * The storage where all the states of a participant are stored
 */
public class Storage {

    // all deltas stored for one participant
    private ArrayList<Delta> participantStates = new ArrayList<>();
    private CustomLogger logger;
    private String pathname;
    // number of keys for each participant
    private int tuplesNumber;
    // #participants for this experiment
    private int participantNumber;
    // the id of the participant owner
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
    private synchronized void initializeStates(int n, int p) {

        Delta tmp;
        long t0 = System.currentTimeMillis();
        // initialize participants' states
        for(int i = 0; i < n; i++) {
            for(int j = 0; j < p; j++) {
                if (i == id) {
                    // t0 - j to maintain the invariant about the impossibility of having same version number for different keys in the same process
                    tmp = new Delta(i, j, Utilities.getRandomNum(0, 1000).toString(), t0 - j, 0);
                } else {
                    tmp = new Delta(i, j, null, 0, 0);
                }
                participantStates.add(tmp);
            }
        }
        // save the items to disk
        save();
    }


    public synchronized Delta update(int key, String value, int ts) {
        Delta ref = participantStates.get((id * tuplesNumber) + key);

        Delta deltaToBeUpdated = new Delta(ref.getP(), ref.getK(), value, System.currentTimeMillis(), ts);

        participantStates.set(((id * tuplesNumber) + key), deltaToBeUpdated);

        logger.debug("P " + id + " updated key " + key + " with v: " + value + " t: " + deltaToBeUpdated.getN() + "at time step " + ts);

        save();
        return deltaToBeUpdated;
    }

    /**
     * This method performs the digest of the storage of the participant
     * @return a ArrayList containing the states of the participants with null values
     */
    public synchronized ArrayList<Delta> createDigest() {
        ArrayList<Delta> digest = new ArrayList<>();
        for (Delta d : this.participantStates){
            digest.add(new Delta(d.getP(), d.getK(), null, d.getN(), 0));
        }
        return digest;
    }

    /**
     * This method performs the digest of the storage of the participant following Scuttlebutt directives
     * @return a ArrayList containing the maximum version number for each participant
     */
    public synchronized ArrayList<Delta> createScuttlebuttDigest() {
        ArrayList<Delta> digest = new ArrayList<>();

        for (int i = 0; i < participantNumber; i++){
            long higherVersion = -1;
            int key = -1;

            for (int j = i * tuplesNumber; j < (1 + i) * tuplesNumber; j++ ) {
                if (participantStates.get(j).getN() > higherVersion) {
                    higherVersion = participantStates.get(j).getN();
                    key = participantStates.get(j).getK();
                }
            }
            digest.add(new Delta(i, key, null, higherVersion, 0));
        }
        return digest;
    }

    /**
     * This method performs the reconciliation, or merging
     * i.e. updates those keys obtained from the peer that are newer w.r.t the ones it already owned
     * @param peerStates a TreeMap which has null value if the participant should not be interested in updating that key,
     *                   or a new value with an higher version number instead
     */
    public synchronized void reconciliation (ArrayList<Delta> peerStates, ArrayList<ArrayList<Delta>> history, int currentTs) {
        //ArrayList<Delta> reconciled = new ArrayList<>(); // to be sent to the observer
        logger.debug("Reconciliation peerStates:" + peerStates);
        for (Delta d : peerStates){
            for (int index = 0; index < this.participantStates.size(); index++) {
                if (d.getP() == participantStates.get(index).getP() && d.getK() == participantStates.get(index).getK() && d.getN() > participantStates.get(index).getN()) {

                    Delta deltaToBeUpdated = new Delta(participantStates.get(index).getP(), participantStates.get(index).getK(), d.getV(), d.getN(), d.getTs());
                    participantStates.set(index, deltaToBeUpdated);

                    if(d.getTs() > currentTs) {
                        history.get(d.getTs()).add(deltaToBeUpdated);
                    } else {
                        history.get(currentTs).add(deltaToBeUpdated);
                    }

                    // increase index of local state and exit current for loop
                    // to get the next delta
                    index = this.participantStates.size(); // exit inner loop
                }
            }
        }
        save();
        //return reconciled;
    }

    /**
     * this method saves the storage on a local text file
     */
    private synchronized void save() {
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
     * If the received digest has tuples with higher version, it means that the peer knows a needed update
     * @param digest the digest coming from the other peer
     * @return a TreeMap indicating the values which needs to be updated
     */
    public synchronized ArrayList<Delta> computeDifferences(ArrayList<Delta> digest) {
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
                            participantStates.get(index).getN(),
                            participantStates.get(index).getTs()));
                    // increase index of local state and exit current for loop
                    // to get the next delta
                    index = this.participantStates.size(); // exit inner loop
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
    public synchronized ArrayList<Delta> computeScuttlebuttDifferences(ArrayList<Delta> digest) {
        ArrayList<Delta> toBeUpdated= new ArrayList<>();
        for (int i = 0; i < digest.size(); i++){
            for (int index = i * tuplesNumber; index < tuplesNumber * (i + 1); index++) {
                if (digest.get(i).getP() == participantStates.get(index).getP() &&
                        digest.get(i).getN() < participantStates.get(index).getN()) {
                    toBeUpdated.add(new Delta(
                            participantStates.get(index).getP(),
                            participantStates.get(index).getK(),
                            participantStates.get(index).getV(),
                            participantStates.get(index).getN(),
                            participantStates.get(index).getTs()));
                }
            }
        }
        return toBeUpdated;
    }

    private synchronized TreeMap<Long, ArrayList<Delta>> statesToTreeMap(ArrayList<Delta> states){
        // represents how many deltas are available for each process.
        TreeMap<Long, ArrayList<Delta>> mapDeltas = new TreeMap<>();
        for (Delta d: states) {
            long p = d.getP();

            if (mapDeltas.containsKey(p)) {
                mapDeltas.get(p).add(d);
            } else {
                ArrayList<Delta> newArray = new ArrayList<>();
                newArray.add(d);
                mapDeltas.put(p, newArray);
            }
        }
        return mapDeltas;
    }

    /**
     * Take as input a list of deltas (to be sent to another participant)
     * and apply an ordering rule to take MTU deltas
     */
    public synchronized ArrayList<Delta> mtuResizeAndSort(ArrayList<Delta> state, int mtuSize, Comparator<Delta> comparator, Ordering method) {

        if (state.size() <= mtuSize){
            return state;
        }
        ArrayList<Delta> mtuArrayList = new ArrayList<>();

        // sort deltas using the given comparator (different whether we are using Precise of Scuttlebutt)
        state.sort(comparator);

        switch (method){
            case OLDEST:
                // the comparator already sorted the deltas accordingly
                mtuArrayList.addAll(state.subList(0, mtuSize));
                break;
            case NEWEST:
                mtuArrayList.addAll(state.subList(state.size() - mtuSize - 1, state.size()));
                Collections.reverse(mtuArrayList);
                break;
            case SCUTTLEBREADTH:
                // take deltas from as many participants as possible.
                TreeMap<Long, ArrayList<Delta>> mapDeltas = statesToTreeMap(state);
                ArrayList<Long> randomP = new ArrayList<>(mapDeltas.keySet());
                Collections.shuffle(randomP);
                int filled = 0;
                while (filled < mtuSize){
                    for (Long i : randomP){
                        if (filled == mtuSize)
                            break;
                        if (mapDeltas.get(i).size() > 0) {
                            mtuArrayList.add(mapDeltas.get(i).get(0));
                            mapDeltas.get(i).remove(0);
                            filled++;
                        }
                    }
                }
                break;
            case SCUTTLEDEPTH:
                // start taking deltas from the participant that wants to sent the most of them
                int randomOrder = Utilities.getRandomNum(0, 1);
                TreeMap<Long, ArrayList<Delta>> mapDelta = statesToTreeMap(state);

                long[] process = new long[mapDelta.size()];  // keys of the processes
                int[] deltasNum = new int[mapDelta.size()]; // number of deltas of the processes

                int j = -1;
                // get the participants with their number of deltas
                for(Long i : mapDelta.keySet()) {
                    j++;
                    process[j] = i;
                    deltasNum[j] = mapDelta.get(i).size();
                }

                // while loop to decide which deltas to insert
                while (mtuArrayList.size() <= mtuSize) {
                    long currentMaxProcess = -1;
                    int currentMaxDelta = -1;
                    int index = -1;

                    // get the process with maximum number of deltas
                    for (int t = 0; t < deltasNum.length; t++) {
                        // For participants with the same number of available deltas,
                        // random ordering among participants is used to remove bias
                        if (deltasNum[t] > currentMaxDelta || (deltasNum[t] == currentMaxDelta && randomOrder == 0)) {
                            currentMaxDelta = deltasNum[t];
                            currentMaxProcess = process[t];
                            index = t;
                        }
                    }
                    while (currentMaxDelta > 0 && mtuArrayList.size() <= mtuSize) {
                        mtuArrayList.add(mapDelta.get(currentMaxProcess).get(0));
                        mapDelta.get(currentMaxProcess).remove(0);
                        currentMaxDelta--;
                        deltasNum[index]--;
                    }
                }
                break;
        }
        return mtuArrayList;
    }

    public synchronized ArrayList<Delta> getParticipantStates() {
        return participantStates;
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

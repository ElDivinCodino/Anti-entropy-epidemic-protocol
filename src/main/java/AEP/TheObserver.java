package AEP;

import AEP.messages.ObserverHistoryMessage;
import AEP.messages.ObserverUpdateRate;
import AEP.messages.SetupMessage;
import AEP.nodeUtilities.Delta;
import akka.actor.UntypedActor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TheObserver extends UntypedActor {

    // the number of participants in this experiment
    private int participantNumber;
    // the number of timesteps in the experiment
    private int timesteps;
    private String pathname;
    // a process for which to measure the metrics (randomly chosen)
    private int historyProcess;

    // whole history of updates and reconciliations of the experiment
    private ArrayList<ArrayList<ArrayList<Delta>>> history;

    // these array lists are #timesteps long
    // metrics
    private ArrayList<ArrayList<Integer>> maxStalePerProcess;
    private ArrayList<ArrayList<Integer>> numStalePerProcess;
    private int[] numberOfDeltasSent;
    private int[] ifGossipMessageGreaterThanMTU;
    private int[] maxStale;
    private int[] numStale;

    // keep track of the updateRate used by a chosen process at each time step
    private float[] updateRates;

    // how many processes have delivered their history
    private int countProcesses = 0;

    private void initializeObserved(SetupMessage message) {
        this.participantNumber = message.getPs().size();
        this.timesteps = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.pathname = message.getStoragePath();
        this.historyProcess = message.getChosenProcess();
        this.maxStalePerProcess = new ArrayList<>(timesteps);
        this.numStalePerProcess = new ArrayList<>(timesteps);
        this.numberOfDeltasSent = new int[timesteps];
        this.ifGossipMessageGreaterThanMTU = new int[timesteps];
        this.maxStale = new int[timesteps];
        this.numStale = new int[timesteps];
        this.updateRates = new float[timesteps];
        System.out.println("Observer chosen participant: " + this.historyProcess);

        this.history = new ArrayList<>();
        for (int i = 0; i < timesteps; i++) {
            ArrayList<ArrayList<Delta>> process = new ArrayList<>();
            maxStalePerProcess.add(new ArrayList<>());
            numStalePerProcess.add(new ArrayList<>());
            for (int j = 0; j < participantNumber; j++) {
                process.add(new ArrayList<>());
                maxStalePerProcess.get(i).add(0);
            }
            this.history.add(process);
        }
    }

    /**
     * Collect the history sent by a process and start metrics computations
     * when received all histories
     */
    private synchronized void collectHistory(ObserverHistoryMessage message){

        for (int i = 0; i < this.timesteps; i++) {
            int currentP = message.getId();
            this.history.get(i).get(currentP).addAll(message.getHistory().get(i));

            this.numberOfDeltasSent[i] += message.getNumberOfDeltas().get(i).stream().mapToInt(Integer::intValue).sum();

            if (message.getIfGreaterThanMtu()[i])
                this.ifGossipMessageGreaterThanMTU[i]++;
        }
        this.countProcesses++;
        if (countProcesses == this.participantNumber){
            // start computing metrics
            saveAndKill();
        }
    }

    /**
     * Each participants sends its update rate once per timestep
     */
    private void saveUpdateRate(ObserverUpdateRate message){
        if (message.getId() == this.historyProcess){
            // for now we take a very simple approach: just save the incoming updateRate in the
            // given time step. Possible drawback: A single node changes its UR multiple times
            // in a single ts, so we may have to average this URs to get a precise measurement.
            this.updateRates[message.getTimestep()] = message.getUpdateRate();
        }
    }

    private void saveAndKill(){

        for (int i = 0; i < participantNumber; i++) {
            computeNumStale(i);
        }
        for (int i = 0; i < timesteps; i++) {
            maxStale[i] = Collections.max(maxStalePerProcess.get(i));
            numStale[i] = ((Long)Math.round(numStalePerProcess.get(i).stream().mapToInt(val -> val).average().getAsDouble())).intValue();
        }

        save();

        context().system().terminate();
    }

    // filter out and take only the local updates if locals == true, ore the non local updates if locals == false

    /**
     * Take only local updates (locals = true) or reconliations (locals = false)
     * from an array of updates of a process (for a single timestep)
     */
    private ArrayList<Delta> getLocals (ArrayList<Delta> updates, int process, boolean locals) {
        ArrayList<Delta> localDeltas = new ArrayList<>();

        for (Delta d: updates) {
            // we want local updates so we take those deltas belonging to this process
            if (d.getP() == process && locals) {
                localDeltas.add(d);
            } else if (d.getP() != process && !locals){
                localDeltas.add(d);
            }
        }
        return localDeltas;
    }

    /**
     * Compute both the number of stale deltas and the maximum staleness
     * for a certain process, for each time step
     * @param mainProcess the process for which to compute the metrics
     */
    private void computeNumStale(Integer mainProcess) {

        // a map that keeps track of the stale deltas present in the historyProcess
        // At each timestep the reconciled deltas are removed, the ones that remain increase the stale counter
        // At each timestep we take the element with higher counter.
        // first key is the process, second key is the delta key
        TreeMap<Integer, TreeMap<Integer, Integer>> maxStaleCounter = new TreeMap<>();
        for (int j = 0; j < participantNumber; j++) {
            maxStaleCounter.put(j, new TreeMap<>());
        }

        HashSet<Delta> tmp = new HashSet<>();

        // ts loop
        for (int i = 0; i < history.size(); i++) {
            for (int j = 0; j < history.get(i).size(); j++) {
                if (j != mainProcess){
                    tmp.addAll(getLocals(this.history.get(i).get(j), j, true));
                }
            }

            HashSet<Delta> reconc = new HashSet<>();
            reconc.addAll(getLocals(this.history.get(i).get(mainProcess), mainProcess, false));
            HashSet<Delta> inter = intersection(tmp, reconc);


            assert inter.size() >= new HashSet<>(inter).size();

            int size = tmp.size();
            // we remove from all the local updates of timestep ts the
            // reconciled updates happened at historyProcess participant
            // In this way we leave inside tmp just the local updates that were
            // not propagated to historyProcess participant.
            HashSet<Delta> toBeRemoved = new HashSet<>();
            for (Delta d2 : reconc) {
                for (Delta d1 : tmp) {
                    if (d1.getP() == d2.getP() && d1.getK() == d2.getK() && d1.getN() <= d2.getN()) {
                        toBeRemoved.add(d1);
                    }
                }
            }

            boolean changed = tmp.removeAll(toBeRemoved);  // returns true if the operation changes the list

            assert size == (tmp.size() + toBeRemoved.size());

            // numStale is the number of deltas that have not yet been reconciled
            numStalePerProcess.get(i).add(tmp.size());

            // first we need to remove from the tree map the Deltas that have been reconciled
            if (changed) { // otherwise we don't even bother
                for (Delta d : inter){
                    if (maxStaleCounter.get(d.getP()).containsKey(d.getK())){
                        maxStaleCounter.get(d.getP()).remove(d.getK());
                    }
                }
            }

            /*
             tmp contains now the deltas that are stale in process historyProcess at time step i
             Insert new deltas with counter 0. Increase by 1 all counters to increase staleness of one time step
            */
            for (Delta d : tmp) {
                if (maxStaleCounter.get(d.getP()).containsKey(d.getK())){  // increase staleness of this key
                    // we do this after the for loop
                    // maxStaleCounter.get(d.getP()).put(d.getK(), maxStaleCounter.get(d.getP()).get(d.getK()) + 1);
                } else {
                    maxStaleCounter.get(d.getP()).put(d.getK(), 0);
                }
            }
            for (Integer key : maxStaleCounter.keySet()){
                maxStaleCounter.get(key).replaceAll((k, v) -> v + 1);
            }

            // now get the maximum value in the treeMap and elect that key as maximum stale
            ArrayList<Integer> maxes = new ArrayList<>();
            for (int j = 0; j < participantNumber; j++) {
                if (maxStaleCounter.get(j).size() > 0){
                    maxes.add(Collections.max(maxStaleCounter.get(j).values()));
                }
            }
            if (maxes.size() > 0){
                // at time step i per process mainProcess
                maxStalePerProcess.get(i).set(mainProcess, Collections.max(maxes));
            }
        }
    }

    /**
     * Custom method to compute intersection of two HashSets of Deltas
     */
    public HashSet<Delta> intersection(HashSet<Delta> list1, HashSet<Delta> list2) {
        HashSet<Delta> list = new HashSet<>();

        // should take only the ones which stop the staleness (i.e. are the newest)!
        for (Delta d2 : list2) {
            if (list1.contains(d2)){
                list.add(d2);
            }
        }

        return list;
    }

    private String arrayString(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxStale.length; i++) {
            sb.append(maxStale[i]).append(" ");
        }
        sb.append("\n");
        for (int i = 0; i < numStale.length; i++) {
            sb.append(numStale[i]).append(" ");
        }
        sb.append("\n");
        for (int i = 0; i < this.updateRates.length; i++) {
            sb.append(this.updateRates[i]).append(" ");
        }
        sb.append("\n");
        for (int i = 0; i < this.ifGossipMessageGreaterThanMTU.length; i++) {
            sb.append(this.ifGossipMessageGreaterThanMTU[i]).append(" ");
        }
        sb.append("\n");
        for (int i = 0; i < this.numberOfDeltasSent.length; i++) {
            sb.append(this.numberOfDeltasSent[i]).append(" ");
        }
        return sb.toString();
    }

    private void save(){
        try {
            FileWriter out = new FileWriter(pathname);
            out.write(arrayString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "SetupMessage": // initialization message
                initializeObserved((SetupMessage) message);
                break;
            case "ObserverUpdateRate":
                saveUpdateRate((ObserverUpdateRate) message);
                break;
            case "ObserverHistoryMessage":
                collectHistory((ObserverHistoryMessage) message);
                break;
        }
    }
}

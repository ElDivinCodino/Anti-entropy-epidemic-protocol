package AEP;

import AEP.messages.ObserverUpdate;
import AEP.messages.ObserverUpdateRate;
import AEP.messages.SetupMessage;
import AEP.nodeUtilities.Delta;
import AEP.nodeUtilities.Utilities;
import akka.actor.UntypedActor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by StefanoFiora on 30/08/2017.
 */
public class TheObserver extends UntypedActor {

    private int tuplesNumber;
    private int participantNumber;
    private int timesteps;
    private int finalTimestep;
    private String pathname;
    private int historyProcess;

    private ArrayList<ArrayList<ArrayList<Delta>>> history;

    // these array lists are #timesteps long
    private long[] maxStale;
    private int[] numStale;

    // keep track of the updateRate used by a chosen process at each time step
    private float[] updateRates;

    private void initializeObserved(SetupMessage message) {
        this.tuplesNumber = message.getTuplesNumber();
        this.participantNumber = message.getPs().size();
        this.finalTimestep = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.timesteps = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.pathname = message.getStoragePath();
        this.historyProcess = message.getChosenProcess();
        this.maxStale = new long[timesteps];
        this.numStale = new int[timesteps];
        this.updateRates = new float[timesteps];
        System.out.println("Observer chosen participant: " + this.historyProcess);

        this.history = new ArrayList<>();
        for (int i = 0; i < timesteps; i++) {
            ArrayList<ArrayList<Delta>> process = new ArrayList<>();
            for (int j = 0; j < participantNumber; j++) {
                process.add(new ArrayList<>());
            }
            this.history.add(process);
        }
    }

    private void saveUpdateRate(ObserverUpdateRate message){
        // TODO: now we check here for adding just the chosen process. We should do it from the actors perspective.
        if (message.getId() == this.historyProcess){
            // for now we take a very simple approach: just save the incoming updateRate in the
            // given time step. Possible drawback: A single node changes its UR multiple times
            // in a single ts, so we may have to average this URs to get a precise measurement.
            this.updateRates[message.getTimestep()] = message.getUpdateRate();
        }
    }

    private void localUpdate(Integer id, Delta d, Integer ts){

        // for the history we do not care about the local values of the selected process
        if (id != this.historyProcess){
            this.history.get(ts).get(d.getP()).add(d);
        }
    }

    private void observedUpdate(Integer id, ArrayList<Delta> updates, Integer ts, long timestamp) {

        if (ts == this.finalTimestep) {
            saveAndKill();
        }

        // TODO: Performance improvement, send local updates just from the chosen process
        if (id == this.historyProcess) {
            // history
            this.history.get(ts).get(id).addAll(updates);
            for(Delta d : updates) {
                d.setUpdateTimestamp(timestamp);
            }
        }
    }

    private void update(ObserverUpdate message){
        if (message.isLocal()) {
            localUpdate(message.getId(), message.getDelta(), message.getTimestep());
        } else {
            observedUpdate(message.getId(), message.getUpdates(), message.getTimestep(), message.getTimestamp());
        }

    }

    private void saveAndKill(){

        computeMaxStale();

        computeNumStale();

        save();

        context().system().terminate();
    }

    private void computeMaxStale(){
        // contains all updates
        ArrayList<Delta> tmp = new ArrayList<>();
        // contains all the updates done by the chosen process until a certain time step
        ArrayList<Delta> historyProcessUpdates = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            for (int j = 0; j < history.get(i).size(); j++) {
                if (j != this.historyProcess){
                    tmp.addAll(this.history.get(i).get(j));
                } else {
                    historyProcessUpdates.addAll(this.history.get(i).get(j));
                }
            }

            for (Delta d: this.history.get(i).get(this.historyProcess)) {
                // if d is the last update done by d.getP at time step i
                // TODO: tmp.contains(d) > non dovrebbe essere ovvio?
                if (tmp.contains(d) && isTheLastOne(d, tmp)) {
                    computeStale(i, d, historyProcessUpdates);
                    ArrayList<Delta> toBeRemoved = new ArrayList<>();
                    for(Delta oldDeltas : historyProcessUpdates) {
                        if(oldDeltas.getP() == d.getP() && oldDeltas.getK() == d.getK()){
                            // I don't need all the older updates anymore, so I delete them
                            toBeRemoved.add(oldDeltas);
                        }
                    }
                    historyProcessUpdates.removeAll(toBeRemoved);
                    // I will remove also d, so I re-add it in order to compute the stale next time
                    historyProcessUpdates.add(d);
                }
            }
        }
    }

    // seeks if delta is equal to the last one updated
    private boolean isTheLastOne(Delta d, ArrayList<Delta> tmp) {
        for(int i = (tmp.size() - 1); i > -1; i--) {
            // if the first element at time step i, for process d.getP and key d.getK that I
            // encounter starting from last ts han not the same timestamp, it means that d is not the last one
            if (tmp.get(i).getP() == d.getP() && tmp.get(i).getK() == d.getK()) {
                if (tmp.get(i).getN() != d.getN()) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        // if there is not such delta, it means that is the only one, so also the last one
        // N.B. not sure if this statement is ever reached
        return true;
    }

    // takes the new non-stale Delta, and search for the last non-stale Delta to compute the staleness between them
    private void computeStale(int ts, Delta lastDelta, ArrayList<Delta> updates) {
        // oldest will be the last non-stale update among all the updates for the same key
        Delta oldest = new Delta(lastDelta.getP(), lastDelta.getK(), lastDelta.getV(), lastDelta.getN());  // TODO: possiamo rimuovere oldest ed usare direttamente lastDelta?

        for(Delta oldDeltas : updates) {
            if(oldDeltas.getP() == lastDelta.getP() && oldDeltas.getK() == lastDelta.getK() && oldDeltas.getN() < oldest.getN())
                oldest = oldDeltas;
        }

        long staleness = lastDelta.getUpdateTimestamp() - oldest.getN();

        if (staleness > maxStale[ts])
            maxStale[ts] = staleness;
    }

    private void computeNumStale() {
        ArrayList<Delta> tmp = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            for (int j = 0; j < history.get(i).size(); j++) {
                if (j != this.historyProcess){
                    tmp.addAll(this.history.get(i).get(j));
                }
            }

            // we remove from all the local updates of timestep ts the
            // reconciled updates happened at historyProcess participant
            // In this way we leave inside tmp just the local updates that were
            // not propagated to historyProcess participant.
            tmp.removeAll(this.history.get(i).get(this.historyProcess));
            numStale[i] = tmp.size();
        }
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
        for (int i = 0; i < updateRates.length; i++) {
            sb.append(updateRates[i]).append(" ");
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
//        logger.info("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "SetupMessage": // initialization message
                initializeObserved((SetupMessage) message);
                break;
            case "ObserverUpdate": // initialization message
                update((ObserverUpdate) message);
                break;
            case "ObserverUpdateRate":
                saveUpdateRate((ObserverUpdateRate) message);
        }
    }
}

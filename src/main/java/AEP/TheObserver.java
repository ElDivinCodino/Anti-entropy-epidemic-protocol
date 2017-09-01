package AEP;

import AEP.messages.ObserverUpdate;
import AEP.messages.SetupMessage;
import AEP.messages.TimeoutMessage;
import AEP.nodeUtilities.Delta;
import AEP.nodeUtilities.Utilities;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    // entire distributed replicated state
    private TreeMap<Integer, TreeMap<Integer, ArrayList<Delta>>> observed;

    private ArrayList<ArrayList<ArrayList<Delta>>> history;

    // these array lists are #timesteps long
    private long[] maxStale;
    private int[] numStale;

    private void initializeObserved(SetupMessage message) {
        this.tuplesNumber = message.getTuplesNumber();
        this.participantNumber = message.getPs().size();
        this.finalTimestep = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.timesteps = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.pathname = message.getStoragePath();
        this.maxStale = new long[timesteps];
        this.numStale = new int[timesteps];
        this.historyProcess = Utilities.getRandomNum(0, participantNumber-1);

        this.observed = new TreeMap<>();
        for(int j = 0; j < participantNumber; j++) {    // add all participants
            TreeMap<Integer, ArrayList<Delta>> pLocalState = new TreeMap<>();
            for(int k = 0; k < participantNumber; k++) {   // add all the states of each participant
                ArrayList<Delta> list = new ArrayList<>();

                for (int z = 0; z < tuplesNumber; z++) {
                    list.add(new Delta(k, z, null, System.currentTimeMillis()));
                }
                pLocalState.put(k, list);
            }
            this.observed.put(j, pLocalState);
        }

        this.history = new ArrayList<>();
        for (int i = 0; i < timesteps; i++) {
            ArrayList<ArrayList<Delta>> process = new ArrayList<>();
            for (int j = 0; j < participantNumber; j++) {
                process.add(new ArrayList<>());
            }
            this.history.add(process);
        }
    }

    private void localUpdate(Integer id, Delta d, Integer ts){
        observed.get(id).get(id).set(d.getK(), d);
        // for the history we do not care about the local values of the selected process
        if (id != this.historyProcess){
            this.history.get(ts).get(d.getP()).add(d);
        }
    }

    private void observedUpdate(Integer id, ArrayList<Delta> updates, Integer ts){

        if (ts == this.finalTimestep){
            saveAndKill();
        }

        if (id == this.historyProcess){
            // history
            this.history.get(ts).get(id).addAll(updates);


            // max stale
            for(Delta d : updates) {
                if (d.getN() == observed.get(d.getP()).get(d.getP()).get(d.getK()).getN()) {
                    // it means that I reached the last update done locally by d.getP()
                    long staleness = d.getN() - observed.get(id).get(d.getP()).get(d.getK()).getN();
                    observed.get(id).get(d.getP()).set(d.getK(), d);

                    if (staleness > maxStale[ts])
                        maxStale[ts] = staleness;
                } else {
                    // it means that I received an update that is not the last one, so the staleness continues:
                    // I'm not interested in updating anything
                }
            }
        }


//        save();
    }

    private void update(ObserverUpdate message){
        if (message.isLocal()) {
            localUpdate(message.getId(), message.getDelta(), message.getTimestep());
        } else {
            observedUpdate(message.getId(), message.getUpdates(), message.getTimestep());
        }

    }

    private void computeMaxStale(){

    }

    private void saveAndKill(){
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

        save();

        context().system().terminate();
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

//    public String toString(){
//
//    }

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
        }
    }
}

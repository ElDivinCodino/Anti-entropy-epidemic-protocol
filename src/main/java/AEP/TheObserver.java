package AEP;

import AEP.messages.ObserverUpdate;
import AEP.messages.SetupMessage;
import AEP.messages.TimeoutMessage;
import AEP.nodeUtilities.Delta;
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
    private String pathname;

    // entire distributed replicated state
    private TreeMap<Integer, TreeMap<Integer, ArrayList<Delta>>> observed;

    // these array lists are #timesteps long
    private long[] maxStale;
    private int[] numStale;
    // for each process we track the last time step when the process was not stale
    private int[] lastStale;

    private void initializeObserved(SetupMessage message) {
        this.tuplesNumber = message.getTuplesNumber();
        this.participantNumber = message.getPs().size();
        this.timesteps = message.getTimesteps().get(message.getTimesteps().size()-1);
        this.pathname = message.getStoragePath();
        this.maxStale = new long[timesteps];
        this.numStale = new int[timesteps];
        this.lastStale = new int[participantNumber];

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
    }

    private void localUpdate(Integer id, Delta d){
        observed.get(id).get(id).set(d.getK(), d);
    }

    private void observedUpdate(Integer id, ArrayList<Delta> updates, Integer ts){
        for(Delta d : updates) {
            if (d.getN() == observed.get(d.getP()).get(d.getP()).get(d.getK()).getN()) {
                // it means that I reached the last update done locally by d.getP()
                long staleness = d.getN() - observed.get(id).get(d.getP()).get(d.getK()).getN();
                observed.get(id).get(d.getP()).set(d.getK(), d);

                for (int i = lastStale[id]; i < ts; i++) {
                    numStale[i]++;
                }
                lastStale[id] = ts;

                if (staleness > maxStale[ts])
                    maxStale[ts] = staleness;
            } else {
                // it means that I received an update that is not the last one, so the staleness continues:
                // I'm not interested in updating anything
            }
        }
        save();
    }

    private void update(ObserverUpdate message){
        if (message.isLocal()) {
            localUpdate(message.getId(), message.getDelta());
        } else {
            observedUpdate(message.getId(), message.getUpdates(), message.getTimestep());
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

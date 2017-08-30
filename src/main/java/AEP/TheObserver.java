package AEP;

import AEP.messages.ObserverUpdate;
import AEP.messages.SetupMessage;
import AEP.messages.TimeoutMessage;
import AEP.nodeUtilities.Delta;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

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
    private int deletingTime = 5; // how often (in seconds) prune the branches of previous timesteps

    // one tree map per time step
    TreeMap<Integer, TreeMap<Long, TreeMap<Long, ArrayList<Delta>>>> observed;

    // these array lists are #timesteps long
    ArrayList<Long> maxStale;

    private void initializeObserved(SetupMessage message) {
        this.tuplesNumber = message.getTuplesNumber();
        this.participantNumber = message.getPs().size();
        this.timesteps = message.getTimesteps().size();

        for(int i = 0; i < timesteps; i++) {    // add all timesteps
            for(int j = 0; j < participantNumber; j++) {    // add all participants
                for(long k = 0; k < participantNumber; k++) {   // add all the states of each participant
                    ArrayList<Delta> list = new ArrayList<>();

                    for (int z = 0; z < tuplesNumber; z++) {
                        list.add(new Delta(k, z, null, -1));
                    }

                    observed.get(i).get(j).put(k, list);    // add all the deltas of each state
                }
            }
        }

        scheduleTimeout(deletingTime, TimeUnit.SECONDS);
    }

    private void localUpdate(Integer id, Delta d, Integer ts){
        observed.get(ts).get(id).get(id).set((int)d.getK(), d);
    }

    private void observedUpdate(Integer id, ArrayList<Delta> updates, Integer ts){
        for(Delta d : updates) {
            if (d.getN() == observed.get(ts).get(d.getP()).get(d.getP()).get((int)d.getK()).getN()) {
                // it means that I reached the last update done locally by d.getP()
                long staleness = d.getN() - observed.get(ts).get(id).get(d.getP()).get((int)d.getK()).getN();
                observed.get(ts).get(id).get(d.getP()).set((int)d.getK(), d);

                if (staleness > maxStale.get(ts))
                    maxStale.set(ts, staleness);
            } else {
                // it means that I received an update that is not the last one, so the staleness continues:
                // I'm not interested in updating anything
            }
        }
    }

    private void update(ObserverUpdate message){
        if (message.isLocal()) {
            localUpdate(message.getId(), message.getDelta(), message.getTimestep());
        } else {
            observedUpdate(message.getId(), message.getUpdates(), message.getTimestep());
        }

    }

    private void timeoutMessage(TimeoutMessage message) {
        pruneBranches();
        scheduleTimeout(deletingTime, TimeUnit.SECONDS);
    }

    // TODO: A little bit empirical
    private void pruneBranches() {
        observed.remove(0);
        observed.remove(1);
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
            case "TimeoutMessage":
                timeoutMessage((TimeoutMessage) message);
                break;
        }
    }

    /**
     * This method implements a scheduler that triggers a message every certain time
     * @param time quantity of time chosen
     * @param unit time unit measurement chosen
     */
    protected void scheduleTimeout(Integer time, TimeUnit unit) {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(time, unit),
                getSelf(), new TimeoutMessage(), getContext().system().dispatcher(), getSelf());
        //logger.info("scheduleTimeout: scheduled timeout in {} {}",
          //      time, unit.toString());
    }
}

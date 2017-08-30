package AEP;

import AEP.messages.*;
import AEP.nodeUtilities.*;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Francesco on 24/08/17.
 */
public class Participant extends UntypedActor{

    protected ActorRef observer;

    // custom logger to display useful stuff to console
    protected CustomLogger logger;

    // Where all the data items are stored.
    protected Storage storage = null;
    protected String storagePath;
    protected List<ActorRef> ps;
    protected int tuplesNumber;
    protected int id;

    protected float updateRate = 1;
    // used in FC classes
    protected float desiredUR;
    protected int gossipRate = 1;  // one gossip message per second

    protected boolean flow_control;

    // experiment parameters
    int current_timestep;
    int current_timestep_index;
    List<Integer> timesteps;
    List<Integer> updaterates;

    public Participant(int id, CustomLogger.LOG_LEVEL level) {
        this.id = id;
        this.current_timestep = -1;  // beginning of experiment
        this.current_timestep_index = 0;  // index of timestep list

        this.logger = new CustomLogger("P" + this.id);
        this.logger.setLevel(level);
    }

    protected void initValues(SetupMessage message){
        this.tuplesNumber = message.getTuplesNumber();
        this.observer = message.getPs().get(0);
        message.getPs().remove(0);
        this.ps = message.getPs();
        this. storage = new Storage(message.getStoragePath(), ps.size(), tuplesNumber, id, logger);
        this.timesteps = message.getTimesteps();
        this.updaterates = message.getUpdaterates();
        this.flow_control = message.isFlow_control();
        assert timesteps.size() == updaterates.size();

        // get the first update rate
        this.updateRate = this.updaterates.get(0);
    }

    private void setupMessage(SetupMessage message){
        initValues(message);
        increaseTimeStep();
        logger.info("Setup completed for node " + id);

        scheduleTimeout(this.gossipRate, TimeUnit.SECONDS);
        if (this.updateRate != 0)
            scheduleUpdateTimeout(Math.round(1000/updateRate), TimeUnit.MILLISECONDS);
    }

    protected void increaseTimeStep(){
        // increase time counter
        this.current_timestep++;
        System.out.println("current_timestep " + this.current_timestep);
        // if this is the last timestep, stop the experiment
        if (this.current_timestep == this.timesteps.get(this.timesteps.size()-1)){
            logger.info("End of experiment for Participant " + this.id);
            context().system().terminate();
        }
        // if there is a change in the update rate
        if (this.current_timestep == this.timesteps.get(this.current_timestep_index)){
            if (flow_control){
                this.desiredUR = this.updaterates.get(this.current_timestep_index);
                if (this.desiredUR == 0){
                    this.updateRate = 0;
                }
            }else{
                this.updateRate = this.updaterates.get(this.current_timestep_index);
            }

            logger.debug("Update rate changed to " + this.updateRate + " for p " + this.id);
            this.current_timestep_index++;
        }
    }

    protected void timeoutMessage(TimeoutMessage message){
        this.increaseTimeStep();

        int rndId;
        // choose a random peer excluding self
        do {
            rndId = Utilities.getRandomNum(0, ps.size() - 1);
        } while (rndId == this.id);
        ActorRef q = ps.get(rndId);

        logger.debug("P " + this.id + " starts gossip with P " + q);

        q.tell(new StartGossip(storage.createDigest()), self());
        logger.info("Timeout: sending StartGossip to " + q);
        scheduleTimeout(this.gossipRate, TimeUnit.SECONDS);
    }

    /**
     * First phase, here q receives the Digest from p
     * @param message
     */
    protected void startGossip(StartGossip message){
        logger.info("First phase: Digest from " + getSender());
        ArrayList<Delta> digest = ((StartGossip) message).getParticipantStates();
        // sender set to null because we do not need to answer to this message
        getSender().tell(new GossipMessage(false, storage.computeDifferences(digest)), null);
        // send to p the second message containing the digest (NOTE: in the paper it should be just the outdated entries that q requests to p)
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());
        logger.info("Second phase: sending differences + digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());
            logger.info("Gossip exchange with node " + sender() + " completed");
        } else {
            // second phase, receiving message(s) from q.
            if (getSender() == getContext().system().deadLetters()){ // this is the message with deltas
                storage.reconciliation(message.getParticipantStates());
            }else{ // digest message to respond to
                // send to q last message of exchange with deltas.
                getSender().tell(new GossipMessage(true, storage.computeDifferences(message.getParticipantStates())), self());
            }
            logger.info("Third phase: sending differences to " + getSender());
        }
    }

    private void update() {
        String newValue = Utilities.getRandomNum(0, 1000).toString();
        int keyToBeUpdated = Utilities.getRandomNum(0, tuplesNumber - 1);
        Delta update = storage.update(keyToBeUpdated, newValue);

        // send update to Observer
        observer.tell(new ObserverUpdate(this.id, this.current_timestep, update, true), getSelf());

        if (this.updateRate != 0)
            scheduleUpdateTimeout(Math.round(1000/this.updateRate), TimeUnit.MILLISECONDS);
    }

    protected void test(Object message){
        logger.error("Method not implemented in class " + this.getClass().getName());
    }

    public void onReceive(Object message) throws Exception {
        logger.info("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "SetupMessage": // initialization message
                setupMessage((SetupMessage) message);
                break;
            case "TimeoutMessage":
                timeoutMessage((TimeoutMessage) message);
                break;

            case "StartGossip":
               startGossip((StartGossip) message);
                break;
            case "GossipMessage":
                gossipMessage((GossipMessage) message);
                break;
            case "UpdateTimeout":
                update();
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
        logger.info("scheduleTimeout: scheduled timeout in {} {}",
                time, unit.toString());
    }

    /**
     * This method implements a scheduler that triggers an update every certain time
     * @param time quantity of time chosen
     * @param unit time unit measurement chosen
     */
    private void scheduleUpdateTimeout(Integer time, TimeUnit unit) {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(time, unit),
                getSelf(), new UpdateTimeout(), getContext().system().dispatcher(), getSelf());
        logger.info("scheduleUpdateTimeout: scheduled timeout for an update in {} {}",
                time, unit.toString());
    }
}

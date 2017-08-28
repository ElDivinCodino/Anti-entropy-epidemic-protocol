package AEP;

import AEP.messages.*;
import AEP.nodeUtilities.*;
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

    // custom logger to display useful stuff to console
    protected CustomLogger logger = new CustomLogger();

    // Where all the data items are stored.
    protected Storage storage = null;
    protected String storagePath;
    protected List<ActorRef> ps;
    protected int tuplesNumber;
    protected int id;

    protected int updateRate = 1000;

    public Participant(String destinationPath, int id) {
        this.storagePath = destinationPath;
        this.id = id;
        this.logger.setLevel(CustomLogger.LOG_LEVEL.DEBUG);
    }

    protected void setupMessage(SetupMessage message){
        tuplesNumber = message.getCouplesNumber();
        ps = message.getParticipants();

        storage = new Storage(storagePath, ps.size(), tuplesNumber, id);

        logger.debug("Setup completed for node " + id);

        scheduleTimeout(1, TimeUnit.SECONDS);
        scheduleUpdateTimeout(updateRate, TimeUnit.MILLISECONDS);
    }

    protected void timeoutMessage(TimeoutMessage message){
        int rndId;
        // choose a random peer excluding self
        do {
            rndId = Utilities.getRandomNum(0, ps.size() - 1);
        } while (rndId == this.id);
        ActorRef q = ps.get(rndId);

        q.tell(new StartGossip(storage.createDigest()), self());
        logger.debug("Timeout: sending StartGossip to " + q);
        scheduleTimeout(1, TimeUnit.SECONDS);
    }

    /**
     * First phase, here q receives the Digest from p
     * @param message
     */
    protected void startGossip(StartGossip message){
        logger.debug("First phase: Digest from " + getSender());
        ArrayList<Delta> digest = ((StartGossip) message).getParticipantStates();
        // sender set to null because we do not need to answer to this message
        getSender().tell(new GossipMessage(false, storage.computeDifferences(digest)), null);
        // send to p the second message containing the digest (NOTE: in the paper it should be just the outdated entries that q requests to p)
        getSender().tell(new GossipMessage(false, storage.createDigest()), self());
        logger.debug("Second phase: sending differences + digest to " + getSender());
    }

    protected void gossipMessage(GossipMessage message){
        if (message.isSender()) {
            storage.reconciliation(message.getParticipantStates());
            logger.debug("Gossip exchange with node " + sender() + " completed");
        } else {
            // second phase, receiving message(s) from q.
            if (getSender() == null){ // this is the message with deltas
                storage.reconciliation(message.getParticipantStates());
            }else{ // digest message to respond to
                // send to q last message of exchange with deltas.
                getSender().tell(new GossipMessage(true, storage.computeDifferences(message.getParticipantStates())), self());
            }
            logger.debug("Third phase: sending differences to " + getSender());
        }
    }

    protected void update() {
        String newValue = Utilities.getRandomNum(0, 1000).toString();
        int keyToBeUpdated = Utilities.getRandomNum(0, tuplesNumber - 1);
        storage.update(keyToBeUpdated, newValue);

        scheduleUpdateTimeout(updateRate, TimeUnit.MILLISECONDS);
    }

    protected void test(Object message){
        logger.error("Method not implemented in class " + this.getClass().getName());
    }

    public void onReceive(Object message) throws Exception {
        logger.debug("Received Message {}", message.toString());

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
        logger.debug("scheduleTimeout: scheduled timeout in {} {}",
                time, unit.toString());
    }

    /**
     * This method implements a scheduler that triggers an update every certain time
     * @param time quantity of time chosen
     * @param unit time unit measurement chosen
     */
    protected void scheduleUpdateTimeout(Integer time, TimeUnit unit) {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(time, unit),
                getSelf(), new UpdateTimeout(), getContext().system().dispatcher(), getSelf());
        logger.debug("scheduleUpdateTimeout: scheduled timeout for an update in {} {}",
                time, unit.toString());
    }
}

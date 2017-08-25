package AEP.nodeUtilities;

import AEP.messages.GossipMessage;
import AEP.messages.SetupNetMessage;
import AEP.messages.StartGossip;
import AEP.messages.TimeoutMessage;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Francesco on 24/08/17.
 */
public class Participant extends UntypedActor{

    // custom logger to display useful stuff to console
    private CustomLogger logger = new CustomLogger();

    // Where all the data items are stored.
    private Storage storage = null;
    private String storagePath;
    private List<ActorRef> ps;
    private int id;

    public Participant(String destinationPath, int id) {
        this.storagePath = destinationPath;
        this.id = id;
        this.logger.setLevel(CustomLogger.LOG_LEVEL.DEBUG);
    }

    public void onReceive(Object message) throws Exception {
        logger.debug("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "SetupNetMessage": // initialization message
                SetupNetMessage msg = ((SetupNetMessage) message);

                int couplesNumber = msg.getCouplesNumber();
                ps = msg.getParticipants();

                /*
                if (couplesNumber == 0 || ps.size() == 0) {
                    throw new RuntimeException("No participant states set found");
                } else {
                    storage = new Storage(storagePath, ps.size(), couplesNumber);
                }*/

                storage = new Storage(storagePath, ps.size(), couplesNumber, id);

                logger.debug("Setup completed for node " + id);

                scheduleTimeout(1, TimeUnit.SECONDS);
                break;

            case "TimeoutMessage":
                ActorRef q;

                do {
                    q = ps.get(Utilities.getRandomNum(0, ps.size() - 1));
                } while (q == self());

                q.tell(new StartGossip(storage.createDigest()), self());
                logger.debug("Timeout: sending StartGossip to " + q);
                scheduleTimeout(1, TimeUnit.SECONDS);
                break;

            case "StartGossip":
                TreeMap<Integer, TreeMap<Integer, Couple>> digest = ((StartGossip) message).getParticipantStates();
                getSender().tell(new GossipMessage(false, storage.computeDifferences(digest)), self());
                logger.debug("First phase: sending differences + digest to " + getSender());
                break;

            case "GossipMessage":
                GossipMessage gossipMsg = ((GossipMessage) message);
                if (gossipMsg.isSender()) {
                    storage.reconciliation(gossipMsg.getParticipantStates());
                    logger.debug("Third phase: sending differences to " + getSender());
                } else {
                    storage.reconciliation(gossipMsg.getParticipantStates());
                    getSender().tell(new GossipMessage(true, storage.computeDifferences(gossipMsg.getParticipantStates())), self());
                    logger.debug("Second phase: sending differences + digest to " + getSender());
                }
                break;
        }
    }

    private void scheduleTimeout(Integer time, TimeUnit unit) {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(time, unit),
                getSelf(), new TimeoutMessage(), getContext().system().dispatcher(), getSelf());
        logger.debug("scheduleTimeout: scheduled timeout in {} {}",
                time, unit.toString());
    }
}

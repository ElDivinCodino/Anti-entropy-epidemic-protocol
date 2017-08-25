package AEP.nodeUtilities;

import AEP.CiaoMessage;
import AEP.messages.SetupNetMessage;
import akka.actor.UntypedActor;

/**
 * Created by Francesco on 24/08/17.
 */
public class Participant extends UntypedActor{

    // custom logger to display useful stuff to console
    private DynamoLogger nodeActorLogger = new DynamoLogger();

    // Where all the data items are stored.
    private Storage storage = null;
    private String storagePath;

    public Participant(String destinationPath) {
        this.storagePath = destinationPath;
        context().actorSelection(self().path()).tell(new CiaoMessage(), null);
    }

    public void onReceive(Object message) throws Exception {
        nodeActorLogger.debug("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "SetupNetMessage": // initialization message
                SetupNetMessage msg = ((SetupNetMessage) message);

                int couplesNumber = msg.getCouplesNumber();
                int participantsNumber = msg.getParticipantsNumber();

                if (couplesNumber == 0 || participantsNumber == 0) {
                    throw new RuntimeException("No participant states set found");
                } else {
                    storage = new Storage(storagePath, participantsNumber, couplesNumber);
                }
                break;
            //case...
        }
    }
}

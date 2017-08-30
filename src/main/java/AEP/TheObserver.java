package AEP;

import AEP.messages.ObserverUpdate;
import AEP.nodeUtilities.Delta;
import akka.actor.UntypedActor;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by StefanoFiora on 30/08/2017.
 */
public class TheObserver extends UntypedActor {

    // one tree map per time step
    TreeMap<Integer, TreeMap<Long, TreeMap<Long, ArrayList<Delta>>>> observed;
    // these array lists are #timesteps long
    ArrayList<Integer> staleCount;
    ArrayList<Integer> maxStale;

    private void localUpdate(Integer id, Delta d, Integer ts){


    }

    private void update(ObserverUpdate message){
        if (message.isLocal()) {
            localUpdate(message.getId(), message.getDelta(), message.getTimestep());
        }

    }



    @Override
    public void onReceive(Object message) throws Throwable {
//        logger.info("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "ObserverUpdate": // initialization message
                update((ObserverUpdate) message);
        }
    }
}

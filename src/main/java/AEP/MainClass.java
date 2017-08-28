package AEP;

import AEP.messages.SetupMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import AEP.nodeUtilities.Utilities;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is useful to both setup the network and design the operations performed by the participants
 */
public class MainClass {

    private int n = 4; // number of nodes/participants belonging to the network
    private int p = 5; // number of (key,value) pairs for each participant

    private List<ActorRef> ps = new ArrayList<ActorRef>();

    public static void main(String[] args) {

        MainClass main = new MainClass(); // initialize the system

    }

    public MainClass() {

        String localIP = null;
        String participantName;
        Config myConfig = ConfigFactory.load("application");
        String destinationPath = myConfig.getString("aep.storage.location");

        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up participant states and the actor system

        Integer randomPort = Utilities.getRandomNum(10000, 10100);
        Config custom = ConfigFactory.parseString("akka.remote.netty.tcp.hostname =" + localIP + ", akka.remote.netty.tcp.port = " + randomPort);

        ActorSystem system = ActorSystem.create("AEP", custom.withFallback(myConfig));

        // Set up all the participants
        for (int i = 0; i < n; i++) {
            participantName = "Participant_" + i;
            ActorRef node = system.actorOf(Props.create(Participant.class, destinationPath + "/" + participantName, i), participantName);
            ps.add(node);
        }

        for (ActorRef participant : ps) {
            participant.tell(new SetupMessage(p, ps), null);
        }
    }
}

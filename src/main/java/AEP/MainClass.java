package AEP;

import AEP.messages.SetupNetMessage;
import AEP.nodeUtilities.Participant;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import AEP.nodeUtilities.Utilities;

import java.net.InetAddress;

/**
 * This class is useful to both setup the network and design the operations performed by the participants
 */
public class MainClass {

    private int n = 2; // number of nodes/participants belonging to the network
    private int p = 2; // number of (key,value) pairs for each participant

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

        Integer randomPort = Utilities.getAvailablePort(10000, 10100);
        Config custom = ConfigFactory.parseString("akka.remote.netty.tcp.hostname =" + localIP + ", akka.remote.netty.tcp.port = " + randomPort);

        ActorSystem system = ActorSystem.create("AEP", custom.withFallback(myConfig));

        // Set up all the participants
        for (int i = 0; i < n; i++) {
            participantName = "Participant_" + i;
            ActorRef node = system.actorOf(Props.create(Participant.class, destinationPath), participantName);
            node.tell(new SetupNetMessage(n, p), null);
        }
    }
}

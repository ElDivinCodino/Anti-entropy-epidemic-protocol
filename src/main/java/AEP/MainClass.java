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

    private List<ActorRef> ps = new ArrayList<ActorRef>();

    public static void main(String[] args) {

        MainClass main = new MainClass(); // initialize the system

    }

    public MainClass() {

        String localIP = null;
        String participantName;
        Config myConfig = ConfigFactory.load("application");
        String destinationPath = myConfig.getString("aep.storage.location");
        Integer participants = myConfig.getInt("aep.participants.p");
        Integer deltas = myConfig.getInt("aep.participants.keys");
        Integer mtu = myConfig.getInt("aep.participants.mtu");
        Float alpha = (float)myConfig.getDouble("aep.flowcontrol.alpha");
        Float beta = (float)myConfig.getDouble("aep.flowcontrol.beta");

        List<Integer> timesteps = myConfig.getIntList("aep.execution.timesteps");
        List<Integer> updaterates = myConfig.getIntList("aep.execution.updaterates");

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
        for (int i = 0; i < participants; i++) {
            participantName = "Participant_" + i;
            ActorRef node = system.actorOf(Props.create(PreciseParticipant.class, i), participantName);
            ps.add(node);
        }

        for (int i = 0; i < ps.size(); i++) {
            participantName = "Participant_" + i;
            ps.get(i).tell(new SetupMessage(deltas, ps, mtu, destinationPath + "/" + participantName, timesteps, updaterates, alpha, beta), null);
        }

    }
}

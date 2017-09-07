package AEP;

import AEP.messages.SetupMessage;
import AEP.nodeUtilities.CustomLogger;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import AEP.nodeUtilities.Utilities;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import AEP.PreciseParticipant.Ordering;

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
        String mainClass = myConfig.getString("aep.experiment.class");
        String orderingMethod = myConfig.getString("aep.experiment.ordering");
        String destinationPath = myConfig.getString("aep.storage.location");
        Integer participants = myConfig.getInt("aep.participants.p");
        Integer deltas = myConfig.getInt("aep.participants.keys");
        Float alpha = (float)myConfig.getDouble("aep.flowcontrol.alpha");
        Float beta = (float)myConfig.getDouble("aep.flowcontrol.beta");
        Integer phi1 = myConfig.getInt("aep.flowcontrol.phi1");
        Integer phi2 = myConfig.getInt("aep.flowcontrol.phi2");
        Boolean flow_control = myConfig.getBoolean("aep.flowcontrol.flowcontrol");
        String loglevel = myConfig.getString("aep.logger.level");
        CustomLogger.LOG_LEVEL level = null;

        switch (loglevel) {
            case "INFO":
                level = CustomLogger.LOG_LEVEL.INFO;
                break;
            case "DEBUG":
                level = CustomLogger.LOG_LEVEL.DEBUG;
                break;
            case "OFF":
                level = CustomLogger.LOG_LEVEL.OFF;
                break;
        }

        List<Integer> timesteps = myConfig.getIntList("aep.execution.timesteps");
        List<Integer> updaterates = myConfig.getIntList("aep.execution.updaterates");
        List<Integer> mtu = myConfig.getIntList("aep.execution.mtu");

        // choose a random Participant from which to log operations
        int historyProcess = Utilities.getRandomNum(0, participants - 1);

        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up participant states and the actor system

//        Integer randomPort = Utilities.getRandomNum(10000, 10100);
//        Config custom = ConfigFactory.parseString("akka.remote.netty.tcp.hostname =" + localIP + ", akka.remote.netty.tcp.port = " + randomPort);

        ActorSystem system = ActorSystem.create("AEP"/*, custom.withFallback(myConfig)*/);

        Class myClass = null;
        switch (mainClass) {
            case "Participant":
                myClass = Participant.class;
                break;
            case "PreciseParticipant":
                myClass = PreciseParticipant.class;
                break;
            case "ScuttlebuttParticipant":
                myClass = ScuttlebuttParticipant.class;
                break;
        }

        ActorRef observer = system.actorOf(Props.create(TheObserver.class), "Observer");

        // Set up all the participants
        for (int i = 0; i < participants; i++) {
            participantName = "Participant_" + i;

            ActorRef node = system.actorOf(Props.create(myClass, i, level), participantName);
            ps.add(node);
        }

        Ordering method = null;

        switch (orderingMethod) {
            case "Oldest":
                method = Ordering.OLDEST;
                break;
            case "Newest":
                method = Ordering.NEWEST;
                break;
            case "ScuttleBreadth":
                method = Ordering.SCUTTLEBREADTH;
                break;
            case "ScuttleDepth":
                method = Ordering.SCUTTLEDEPTH;
                break;
        }

        // send first setup message to observer
        observer.tell(new SetupMessage(deltas, ps, mtu, destinationPath + "/" + "observer.txt", timesteps, updaterates, alpha, beta, method, phi1, phi2, flow_control, null, historyProcess), null);

        for (int i = 0; i < ps.size(); i++) {
            participantName = "Participant_" + i;
            ps.get(i).tell(new SetupMessage(deltas, ps, mtu, destinationPath + "/" + participantName + ".txt", timesteps, updaterates, alpha, beta, method, phi1, phi2, flow_control, observer, historyProcess), null);
        }

    }
}

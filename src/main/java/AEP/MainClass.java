package AEP;

import AEP.messages.SetupMessage;
import AEP.nodeUtilities.CustomLogger;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import AEP.nodeUtilities.Utilities;

import java.io.File;
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

        String error_msg = "Exactly 5 parameters are needed!\n" +
                "\t- <Pclass> <Ordering> <nP> <nK> <FC>\n" +
                "\t Pclass: [Participant|Precise|Scuttlebutt]\n" +
                "\t Ordering: [None|Newest|Oldest|Breadth|Depth]\n" +
                "\t nP: <int number>\n" +
                "\t nK: <int number>\n" +
                "\t nK: [true|false]\n";
        if (args.length != 5){
            throw new IllegalArgumentException(error_msg);
        }

        new MainClass(
                args[0],
                args[1],
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                Boolean.parseBoolean(args[4]));
    }

    public MainClass(String mainClass, String orderingMethod, Integer participants, Integer deltas, boolean flow_control) {

        System.out.println(mainClass + " " + orderingMethod + " " + participants + " " + deltas + " " + flow_control);
        String localIP = null;
        String participantName;
        Config myConfig = ConfigFactory.load("application");
        Float alpha = (float)myConfig.getDouble("aep.flowcontrol.alpha");
        Float beta = (float)myConfig.getDouble("aep.flowcontrol.beta");
        Integer phi1 = myConfig.getInt("aep.flowcontrol.phi1");
        Integer phi2 = myConfig.getInt("aep.flowcontrol.phi2");
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
            case "Precise":
                myClass = PreciseParticipant.class;
                break;
            case "Scuttlebutt":
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
        String destinationPath = "";
        switch (orderingMethod) {
            case "None":
                destinationPath = "/tmp/AEP/logs/participant";
                break;
            case "Oldest":
                method = Ordering.OLDEST;
                destinationPath = "/tmp/AEP/logs/precise_oldest";
                break;
            case "Newest":
                method = Ordering.NEWEST;
                destinationPath = "/tmp/AEP/logs/precise_newest";
                break;
            case "Breadth":
                method = Ordering.SCUTTLEBREADTH;
                destinationPath = "/tmp/AEP/logs/scuttle_breadth";
                break;
            case "Depth":
                method = Ordering.SCUTTLEDEPTH;
                destinationPath = "/tmp/AEP/logs/scuttle_depth";
                break;
        }
        if (flow_control) {
            destinationPath = destinationPath + "_fc";
        }

        // Create log directory if not exists
        File directory = new File(destinationPath);
        if (! directory.exists()){
            // make the entire directory path including parents
            directory.mkdirs();
        }
        for(File file: directory.listFiles())
            if (!file.isDirectory())
                file.delete();

        // send first setup message to observer
        observer.tell(new SetupMessage(deltas, ps, mtu, destinationPath + "/" + "observer.txt", timesteps, updaterates, alpha, beta, method, phi1, phi2, flow_control, null, historyProcess), null);

        for (int i = 0; i < ps.size(); i++) {
            participantName = "Participant_" + i;
            ps.get(i).tell(new SetupMessage(deltas, ps, mtu, destinationPath + "/" + participantName + ".txt", timesteps, updaterates, alpha, beta, method, phi1, phi2, flow_control, observer, historyProcess), null);
        }

    }
}

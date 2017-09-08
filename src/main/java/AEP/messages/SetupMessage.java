package AEP.messages;

import AEP.PreciseParticipant.Ordering;
import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Francesco on 24/08/17.
 */
public final class SetupMessage implements Serializable {

    private int tuplesNumber;
    private List<ActorRef> ps;
    private List<Integer> mtu;
    private String storagePath;
    private List<Integer> timesteps;
    private List<Integer> updaterates;
    private float alpha;
    private float beta;
    private Ordering ordering;
    private int phi1;
    private int phi2;
    boolean flow_control;
    ActorRef observer;
    private int chosenProcess;

    public SetupMessage(int tuplesNumber, List<ActorRef> ps, List<Integer> mtu, String storagePath, List<Integer> timesteps, List<Integer> updaterates, float alpha, float beta, Ordering ordering, int phi1, int phi2, boolean flow_control, ActorRef observer, int chosenProcess) {
        this.tuplesNumber = tuplesNumber;
        this.ps = ps;
        this.mtu = mtu;
        this.storagePath = storagePath;
        this.timesteps = timesteps;
        this.updaterates = updaterates;
        this.alpha = alpha;
        this.beta = beta;
        this.ordering = ordering;
        this.phi1 = phi1;
        this.phi2 = phi2;
        this.flow_control = flow_control;
        this.observer = observer;
        this.chosenProcess = chosenProcess;
    }

    public int getTuplesNumber() {
        return tuplesNumber;
    }

    public List<ActorRef> getPs() {
        return ps;
    }

    public List<Integer> getMtu() {
        return mtu;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public List<Integer> getTimesteps() {
        return timesteps;
    }

    public List<Integer> getUpdaterates() {
        return updaterates;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getBeta() {
        return beta;
    }

    public Ordering getOrdering() {
        return ordering;
    }

    public int getPhi1() {
        return phi1;
    }

    public int getPhi2() {
        return phi2;
    }

    public boolean isFlow_control() {
        return flow_control;
    }

    public ActorRef getObserver() {
        return observer;
    }

    public int getChosenProcess() {
        return chosenProcess;
    }
}

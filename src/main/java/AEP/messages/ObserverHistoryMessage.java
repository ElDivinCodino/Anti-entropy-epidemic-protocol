package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

public final class ObserverHistoryMessage implements Serializable {

    Integer id;
    ArrayList<ArrayList<Delta>> history;
    ArrayList<ArrayList<Integer>> numberOfDeltas;
    boolean[] ifGreaterThanMtu;

    public ObserverHistoryMessage(Integer id, ArrayList<ArrayList<Delta>> history, ArrayList<ArrayList<Integer>> numberOfDeltas, boolean[] ifGreaterThanMtu) {
        this.id = id;
        this.history = history;
        this.numberOfDeltas = numberOfDeltas;
        this.ifGreaterThanMtu = ifGreaterThanMtu;
    }

    public ArrayList<ArrayList<Delta>> getHistory() {
        return history;
    }

    public Integer getId() {
        return id;
    }

    public ArrayList<ArrayList<Integer>> getNumberOfDeltas() {
        return numberOfDeltas;
    }

    public boolean[] getIfGreaterThanMtu() {
        return ifGreaterThanMtu;
    }
}

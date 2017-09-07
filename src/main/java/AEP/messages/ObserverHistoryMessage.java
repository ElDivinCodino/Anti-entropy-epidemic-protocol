package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by StefanoFiora on 30/08/2017.
 */
public final class ObserverHistoryMessage implements Serializable {

    Integer id;
    ArrayList<ArrayList<Delta>> history;

    public ObserverHistoryMessage(Integer id, ArrayList<ArrayList<Delta>> history) {
        this.id = id;
        this.history = history;
    }

    public ArrayList<ArrayList<Delta>> getHistory() {
        return history;
    }

    public Integer getId() {
        return id;
    }

}

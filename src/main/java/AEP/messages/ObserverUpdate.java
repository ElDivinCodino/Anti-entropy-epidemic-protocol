package AEP.messages;

import AEP.nodeUtilities.Delta;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by StefanoFiora on 30/08/2017.
 */
public class ObserverUpdate implements Serializable {

    Integer id;
    Integer timestep;
    ArrayList<Delta> updates;
    boolean local;
    long timestamp;

    public ObserverUpdate(Integer id, Integer timestep, ArrayList<Delta> updates, boolean local, long timestamp) {
        this.id = id;
        this.timestep = timestep;
        this.updates = updates;
        this.local = local;
        this.timestamp = timestamp;
    }

    public Integer getTimestep() {
        return timestep;
    }

    public ArrayList<Delta> getUpdates() {
        return updates;
    }

    public Integer getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

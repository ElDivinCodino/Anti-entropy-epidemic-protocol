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

    public ObserverUpdate(Integer id, Integer timestep, ArrayList<Delta> updates) {
        this.id = id;
        this.timestep = timestep;
        this.updates = updates;
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

}

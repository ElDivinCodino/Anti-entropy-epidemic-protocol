package AEP.messages;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 03/09/2017.
 */
public final class ObserverUpdateRate implements Serializable {

    private Integer id;
    private Integer timestep;
    private float updateRate;

    public ObserverUpdateRate(Integer id, Integer timestep, float updateRate) {
        this.id = id;
        this.timestep = timestep;
        this.updateRate = updateRate;
    }

    public Integer getId() {
        return id;
    }

    public Integer getTimestep() {
        return timestep;
    }

    public float getUpdateRate() {
        return updateRate;
    }
}

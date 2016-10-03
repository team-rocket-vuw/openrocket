package data;

import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.FlightData;
import net.sf.openrocket.util.ArrayList;

/**
 * Utility class to contain all the data for a single simulation run
 *
 * Created 3rd October 2016
 * By Marcel van Workum
 */
public class SimulationData {
    private Simulation simulation;
    private ArrayList<FlightData> flightDataList;

    public SimulationData(Simulation simulation, ArrayList<FlightData> flightDataList) {
        this.simulation = simulation;
        this.flightDataList = flightDataList;
    }

    /**
     * Gets simulation.
     *
     * @return the simulation
     */
    public Simulation getSimulation() {
        return simulation;
    }

    /**
     * Gets flight data list.
     *
     * @return the flight data list
     */
    public ArrayList<FlightData> getFlightDataList() {
        return flightDataList;
    }
}

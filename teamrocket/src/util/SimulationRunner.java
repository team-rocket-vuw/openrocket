package util;

import data.SimulationData;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.FlightData;
import net.sf.openrocket.simulation.SimulationOptions;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.AbstractSimulationListener;
import net.sf.openrocket.simulation.listeners.SimulationListener;
import net.sf.openrocket.util.ArrayList;

/**
 * Utility class to perform a variety of openrocket simulations
 *
 * Created 3rd October 2016
 * By Marcel van Workum
 */
public class SimulationRunner {
    private OpenRocketDocument openRocketDocument;
    private final SimulationListener simulationListener = new AbstractSimulationListener();

    public SimulationRunner(OpenRocketDocument openRocketDocument) {
        this.openRocketDocument = openRocketDocument;
    }

    /**
     * Run the simulations
     *
     * TODO pass in config options  to allow for more variety
     *
     * @return List of simulation data objects which are a (Simulation, FlightData List) tuple
     */
    public ArrayList<SimulationData> run() {
        ArrayList<SimulationData> simulationDataList = new ArrayList<>();

        // TODO Hard coded angle values until the GUI can pass in values
        double startAngle = 0;
        double endAngle = 45;
        double angleStep = 1.0;

        for (Simulation simulation : openRocketDocument.getSimulations()) {
            ArrayList<FlightData> flightData = simulateMultipleLaunchAngles(simulation, startAngle, endAngle, angleStep);

            simulationDataList.add(new SimulationData(simulation, flightData));
        }

        return simulationDataList;
    }

    private ArrayList<FlightData> simulateMultipleLaunchAngles(Simulation simulation, double startAngle, double endAngle, double angleStep){
        ArrayList<FlightData> flightDataList = new ArrayList<>();

        try {
            SimulationOptions simulationOptions = simulation.getOptions();

            // Perform the simulation for the specified range of launch angles
            for(double launchAngle = startAngle; launchAngle <= endAngle; launchAngle += angleStep) {
                // Update simulation options
                simulationOptions.setLaunchRodAngle(Math.toRadians(launchAngle));
                simulation.setSimulationOptions(simulationOptions);

                // Perform the simulation
                simulation.simulate(simulationListener);

                // Get the result of the simulation and set launch angle to be stored with flight data
                FlightData f = simulation.getSimulatedData();
                f.setLaunchAngle(launchAngle);

                flightDataList.add(f);
            }
        } catch (SimulationException e) {
            System.err.println(e.getMessage());
        }

        return flightDataList;
    }
}

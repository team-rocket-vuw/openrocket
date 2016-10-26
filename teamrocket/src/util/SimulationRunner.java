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
import org.json.simple.JSONObject;

/**
 * Utility class to perform a variety of openrocket simulations
 *
 * Created 3rd October 2016
 * By Marcel van Workum
 */
public class SimulationRunner {
    private OpenRocketDocument openRocketDocument;
    private final SimulationListener simulationListener = new AbstractSimulationListener();
    private JSONObject weatherDataJson;

    public SimulationRunner(OpenRocketDocument openRocketDocument) {
        this.openRocketDocument = openRocketDocument;
    }

    public SimulationRunner(OpenRocketDocument openRocketDocument, JSONObject weatherDataJson) {
        this.openRocketDocument = openRocketDocument;
        this.weatherDataJson = weatherDataJson;
    }

    public void setWeatherData(JSONObject weatherDataJson){
        this.weatherDataJson = weatherDataJson;
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
            setWeatherConditions(this.weatherDataJson, simulation.getOptions());
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

    public void setWeatherConditions(JSONObject conditions, SimulationOptions simulationOptions){

        JSONObject main = (JSONObject)conditions.get("main");
        JSONObject wind = (JSONObject)conditions.get("wind");
        JSONObject coord = (JSONObject)conditions.get("coord");

        simulationOptions.setLaunchLongitude((double) coord.get("lon"));
        simulationOptions.setLaunchLatitude((double) coord.get("lat"));
        simulationOptions.setWindSpeedAverage((double) wind.get("speed"));
        simulationOptions.setWindDirection((double) wind.get("deg"));
        simulationOptions.setLaunchPressure((double) main.get("pressure"));
        simulationOptions.setLaunchTemperature((double) main.get("temp"));
        //simulationOptions.setLaunchIntoWind(true); //Do we wanna do this??
    }

}

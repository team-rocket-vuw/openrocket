package util;

import data.SimulationData;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.models.wind.PinkNoiseWindModel;
import net.sf.openrocket.models.wind.WindModel;
import net.sf.openrocket.motor.MotorInstanceConfiguration;
import net.sf.openrocket.simulation.*;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.AbstractSimulationListener;
import net.sf.openrocket.simulation.listeners.SimulationListener;
import net.sf.openrocket.util.ArrayList;
import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.GeodeticComputationStrategy;
import net.sf.openrocket.util.WorldCoordinate;
import org.json.simple.JSONObject;

import static net.sf.openrocket.simulation.FlightDataType.TYPE_LATITUDE;
import static net.sf.openrocket.simulation.FlightDataType.TYPE_LONGITUDE;


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

                simulationOptions = setWeatherConditions(this.weatherDataJson, simulation.getOptions());
                simulation.setSimulationOptions(simulationOptions);

                // Perform the simulation
                simulation.simulate(simulationListener);

                // Get the result of the simulation and set launch angle to be stored with flight data
                FlightData f = simulation.getSimulatedData();
                f.setLaunchAngle(launchAngle);
                f.setLandingCoord(getLandingCoord2(simulation));
                flightDataList.add(f);

                System.out.println();
                System.out.println(getLandingCoord(simulation));
                System.out.println(getLandingCoord2(simulation));
                System.out.println(getLandingCoord3(simulation));
                System.out.println();

            }
        } catch (SimulationException e) {
            System.err.println(e.getMessage());
        }

        return flightDataList;
    }

    public SimulationOptions setWeatherConditions(JSONObject conditions, SimulationOptions simulationOptions){

        JSONObject main = (JSONObject)conditions.get("main");
        JSONObject wind = (JSONObject)conditions.get("wind");
        JSONObject coord = (JSONObject)conditions.get("coord");

        simulationOptions.setLaunchLongitude((double) coord.get("lon"));
        simulationOptions.setLaunchLatitude((double) coord.get("lat"));
        simulationOptions.setWindSpeedAverage((double) wind.get("speed"));
        simulationOptions.setWindDirection((double) wind.get("deg"));
        simulationOptions.setLaunchPressure((double) main.get("pressure"));
        simulationOptions.setLaunchTemperature((double) main.get("temp"));

        return simulationOptions;
        //simulationOptions.setLaunchIntoWind(true); //Do we wanna do this??
    }

    public String getLandingCoord(Simulation simulation){
        //TODO: check these values are legit
        double initialLatitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lat");
        double initialLongitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lon");

        SimulationStatus status = new SimulationStatus(simulation.getConfiguration(), new MotorInstanceConfiguration(), (simulation.getSimulatedConditions().toSimulationConditions()));
        WorldCoordinate coord = status.getRocketWorldPosition();
        Coordinate launchCoord = new Coordinate(initialLongitude, initialLatitude);
        GeodeticComputationStrategy geodeticComputation = (simulation.getSimulatedConditions().toSimulationConditions()).getGeodeticComputation();

        WorldCoordinate landingCoord = geodeticComputation.addCoordinate(coord, launchCoord);
        WorldCoordinate diffCoord = differenceCoord(coord, landingCoord);

        System.out.println();
        System.out.println("angle: " + Math.toDegrees(simulation.getOptions().getLaunchRodAngle()));
        System.out.println("wind speed: " + simulation.getOptions().getWindSpeedAverage());
        System.out.println(coord);
        System.out.println(landingCoord);
        System.out.println(diffCoord);
        System.out.println(coordToMeters(diffCoord));
        System.out.println();

        return coordToMeters(diffCoord);
    }

    public String getLandingCoord2(Simulation simulation){
        double initialLatitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lat");
        double initialLongitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lon");

        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);

        Coordinate launchCoord = new Coordinate(initialLongitude, initialLatitude);
        WorldCoordinate coord = new WorldCoordinate(flightDataBranch.getLast(TYPE_LONGITUDE), flightDataBranch.getLast(TYPE_LATITUDE), 0);

        GeodeticComputationStrategy geodeticComputation = (simulation.getSimulatedConditions().toSimulationConditions()).getGeodeticComputation();
        WorldCoordinate landingCoord = geodeticComputation.addCoordinate(coord, launchCoord);
        WorldCoordinate diffCoord = differenceCoord(coord, landingCoord);

        return coordToMeters(diffCoord);
    }

    public String getLandingCoord3(Simulation simulation){
        //TODO: check these values are legit
        double initialLatitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lat");
        double initialLongitude = (double)((JSONObject)weatherDataJson.get("coord")).get("lon");

        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);

        return String.format("(%f,%f)", (flightDataBranch.getLast(TYPE_LONGITUDE) - initialLongitude),
                (flightDataBranch.getLast(TYPE_LATITUDE) - initialLatitude));
    }

    public WorldCoordinate differenceCoord(WorldCoordinate c1, WorldCoordinate c2){
        double dlat = c2.getLatitudeDeg() - c1.getLatitudeDeg();
        double dlon = c2.getLongitudeDeg() - c1.getLongitudeDeg();
        return new WorldCoordinate(dlat, dlon, 0);
    }

    public String coordToMeters(WorldCoordinate c){
        double METERS_PER_DEGREE_LATITUDE = 111325; // "standard figure"
        double METERS_PER_DEGREE_LONGITUDE_EQUATOR = 111050;
        double earthRadius = 6371000; //meters
        return String.format("(%f,%f)", c.getLongitudeDeg() * METERS_PER_DEGREE_LONGITUDE_EQUATOR, c.getLatitudeDeg() * METERS_PER_DEGREE_LATITUDE);
        //return String.format("(%f,%f)", c.getLongitudeDeg()*1000, c.getLatitudeDeg()*1000);
    }
}

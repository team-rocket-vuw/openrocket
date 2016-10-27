package util;

import data.SimulationData;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
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

import java.lang.reflect.Array;
import java.util.List;

import static net.sf.openrocket.simulation.FlightDataType.*;


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



                System.out.println(getFlightCoords(simulation).size());
                System.out.println(getFlightCoords(simulation).toString());

                // Get the result of the simulation and set launch angle to be stored with flight data
                FlightData f = simulation.getSimulatedData();
                f.setLaunchAngle(launchAngle);
                //f.setLandingCoord(getLandingCoord(simulation));
                f.setPositionUpwind(getPositionUpwind(simulation));
                f.setLateralDistance(getLateralDistance(simulation));
                f.setFlightCoords(getFlightCoords(simulation));
                flightDataList.add(f);

            }
        } catch (SimulationException e) {
            System.err.println(e.getMessage());
        }
        System.out.println(test());
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
        simulationOptions.setLaunchIntoWind(true); //Do we wanna do this??

        return simulationOptions;

    }

    public List<String> getFlightCoords(Simulation simulation){
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);
        List<Double> altitudes = flightDataBranch.get(TYPE_ALTITUDE);
        List<Double> positions = flightDataBranch.get(TYPE_POSITION_X);

        List<String> coords = new ArrayList<String>();
        for(int i = 0; i < altitudes.size(); i+=5){
            String coord = String.format("(%f,%f)", positions.get(i), altitudes.get(i));
            coords.add(coord);
        }
        return coords;
    }

    public String getLandingCoord(Simulation simulation){
        //TODO: check these values are legit - They're not....
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);


        SimulationStatus status = new SimulationStatus(simulation.getConfiguration(), new MotorInstanceConfiguration(), (simulation.getSimulatedConditions().toSimulationConditions()));
        WorldCoordinate worldPos = status.getRocketWorldPosition();
        Coordinate launchPosCoord = new Coordinate(simulation.getOptions().getLaunchLongitude(),simulation.getOptions().getLaunchLatitude());

        GeodeticComputationStrategy geodeticComputation = (simulation.getSimulatedConditions().toSimulationConditions()).getGeodeticComputation();
        WorldCoordinate launch = geodeticComputation.addCoordinate(worldPos, launchPosCoord);

        WorldCoordinate launchPos = new WorldCoordinate(flightDataBranch.get(TYPE_LONGITUDE).get(0), flightDataBranch.get(TYPE_LATITUDE).get(0), 0);
        WorldCoordinate landingPos = new WorldCoordinate(flightDataBranch.getLast(TYPE_LONGITUDE), flightDataBranch.getLast(TYPE_LATITUDE), 0);
        WorldCoordinate diffPos = getifferenceBetweenCoords(launchPos, landingPos);

        System.out.println();

        return coordToMeters(landingPos);
    }

    public double getPositionUpwind(Simulation simulation){
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);
        return flightDataBranch.getLast(TYPE_POSITION_X);
    }

    public double getPositionParallelToWind(Simulation simulation){
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);
        return flightDataBranch.getLast(TYPE_POSITION_Y);
    }

    public double getLateralDistance(Simulation simulation){
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);
        return flightDataBranch.getLast(TYPE_POSITION_XY);
    }

    public double getLateralDirection(Simulation simulation){
        FlightDataBranch flightDataBranch = simulation.getSimulatedData().getBranch(0);
        return flightDataBranch.getLast(TYPE_POSITION_DIRECTION);
    }

    public String getCoords(Double distance, Double direction){
        double x = distance;
        System.out.println("Direction: " + direction);
        //TODO: FIX
        double y = x*Math.sin(direction);
        return String.format("(%f,%f)", x, y);

    }

    public String test(){
        WorldCoordinate c1 = new WorldCoordinate(-41.288892, 174.776125, 0);
        WorldCoordinate c2 = new WorldCoordinate(-41.288891, 174.776201,0);
        return coordToMeters(getifferenceBetweenCoords(c1,c2));
    }

    public WorldCoordinate getifferenceBetweenCoords(WorldCoordinate c1, WorldCoordinate c2){
        double dlat = c2.getLatitudeDeg() - c1.getLatitudeDeg();
        double dlon = c2.getLongitudeDeg() - c1.getLongitudeDeg();
        return new WorldCoordinate(dlon, dlat, 0);
    }

    public String coordToMeters(WorldCoordinate c){
        double METERS_PER_DEGREE_LATITUDE = 111325; // "standard figure"
        double METERS_PER_DEGREE_LONGITUDE_EQUATOR = 111050;
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LONGITUDE_EQUATOR * Math.cos(c.getLatitudeRad());
        double earthRadius = 6371000; //meters
        return String.format("(%f,%f)", c.getLongitudeDeg()*metersPerDegreeLongitude, c.getLatitudeDeg()*METERS_PER_DEGREE_LATITUDE);
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (earthRadius * c);

        return dist;
    }

    public String toCartisianCoord(WorldCoordinate c){
        double earthRadius = 6371000;
        double lon = c.getLongitudeDeg();
        double lat = c.getLatitudeDeg();

        double x = earthRadius * Math.cos(lat) * Math.cos(lon);
        double y = earthRadius * Math.cos(lat) * Math.sin(lon);

        return String.format("(%f,%f)", x,y);
    }
}

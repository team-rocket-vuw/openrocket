package util;

import data.SimulationData;
import net.sf.openrocket.simulation.FlightData;
import net.sf.openrocket.util.ArrayList;

/**
 * Utility class to format the simulation data json string
 *
 * Created 3rd October 2016
 * By Marcel van Workum
 */
public class JSONFormatter {

    /**
     * Converts a lsit of Simulation Data objectins into a JSON hash
     *
     * @param simulationDataList list of Simulation Data objects
     * @return JSON Hash string
     */
    public static String stringifySimulationData(ArrayList<SimulationData> simulationDataList) {
        String stringifiedSimulationData = "{";

        for (int simulationDataIndex = 0; simulationDataIndex < simulationDataList.size(); simulationDataIndex++) {
            SimulationData simulationData = simulationDataList.get(simulationDataIndex);

            stringifiedSimulationData += String.format("\"%s\": { ", simulationData.getSimulation().getName());

            stringifiedSimulationData += ConvertFlightData(simulationData.getFlightDataList());

            // add comma to ending bracket, except for last occurrence
            if (simulationDataIndex != simulationDataList.size() - 1) {
                stringifiedSimulationData += "}, ";
            } else {
                stringifiedSimulationData += "} ";
            }
        }

        stringifiedSimulationData += "}";

        return stringifiedSimulationData;
    }

    private static String ConvertFlightData(ArrayList<FlightData> flightDataList) {
        String stringifiedSimulationData = "";

        for (int flightDataIndex = 0; flightDataIndex < flightDataList.size(); flightDataIndex++) {
            FlightData flightData = flightDataList.get(flightDataIndex);

            stringifiedSimulationData +=
            "\"launch" + (flightDataIndex + 1) + "\": { " +
                "\"launchAngle\": " + flightData.getLaunchAngle() + ", " +
                "\"launchStatistics\": { " +
                    flightData.toString() +
                "}";

            // add comma to ending bracket, except for last occurrence
            if (flightDataIndex != flightDataList.size() - 1) {
                stringifiedSimulationData += "}, ";
            } else {
                stringifiedSimulationData += "} ";
            }
        }

        return stringifiedSimulationData;
    }
}

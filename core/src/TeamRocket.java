import java.io.File;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import net.sf.openrocket.database.Databases;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.file.GeneralRocketLoader;
import net.sf.openrocket.file.RocketLoadException;
import net.sf.openrocket.plugin.PluginModule;
import net.sf.openrocket.simulation.FlightData;
import net.sf.openrocket.simulation.SimulationOptions;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.AbstractSimulationListener;
import net.sf.openrocket.simulation.listeners.SimulationListener;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.startup.ExceptionHandler;
import net.sf.openrocket.startup.GuiModule;
import net.sf.openrocket.util.ArrayList;

public class TeamRocket {
	private OpenRocketDocument document;
	private ArrayList<FlightData> listOfFlightData;

	public TeamRocket(){
		loadFile();
		runFileSimulations();
		printFlightData();
	}

	public static void main(String[] args) throws RocketLoadException {
		TeamRocket teamRocket = new TeamRocket();
	}

	public void runFileSimulations(){
		for (Simulation s : document.getSimulations()) {
			SimulationListener simulationListener = new AbstractSimulationListener();

			listOfFlightData = simulateMultipleLaunchAngles(simulationListener, s);
		}
	}

	public ArrayList<FlightData> simulateMultipleLaunchAngles(SimulationListener simulationListener, Simulation s){
		ArrayList<FlightData> fd = new ArrayList<FlightData>();
		try {
			SimulationOptions simulationOptions = s.getOptions();

			//Simulate for each degree between 0 and 45 degrees
			for(int i = 0; i <= 45; i++) {
				simulationOptions.setLaunchRodAngle(Math.toRadians(i));
				s.setSimulationOptions(simulationOptions);
				s.simulate(simulationListener);
				FlightData f = s.getSimulatedData();
				fd.add(f);
			}
		} catch (SimulationException e) {
			e.printStackTrace();
		} finally {
			return fd;
		}
	}

	private void loadFile(){
		File file = new File("Boosted Dart.ork");

		GuiModule guiModule = new GuiModule();
		Module pluginModule = new PluginModule();

		Injector injector = Guice.createInjector(guiModule, pluginModule);
		Application.setInjector(injector);

		guiModule.startLoader();

		Databases.fakeMethod();

		GeneralRocketLoader loader = new GeneralRocketLoader(file);
		try {
			this.document = loader.load();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void printFlightData(){
		for(int i = 0; i < listOfFlightData.size(); i++){
			//TODO: print angle, for now it's simulating at every degree so list index works but not if not at every angle
			System.out.printf("Launch angle: %d degrees, Max Altitude: %f, Flight time: %f\n",
					i, listOfFlightData.get(i).getMaxAltitude(), listOfFlightData.get(i).getFlightTime());
		}
	}
}

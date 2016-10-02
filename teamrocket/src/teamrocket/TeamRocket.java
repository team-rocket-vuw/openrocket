package teamrocket;

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
import net.sf.openrocket.startup.GuiModule;
import net.sf.openrocket.util.ArrayList;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

public class TeamRocket {
	private OpenRocketDocument document;
	private ArrayList<FlightData> listOfFlightData;
	private final PrintStream outputStream = System.out;

	private TeamRocket(){
		disableOutput();

		loadFile();
		runFileSimulations();

		setOutputStream(outputStream);
	}

	private void setOutputStream(PrintStream outputStream) {
		System.setOut(outputStream);
	}

	private void disableOutput() {
		PrintStream dummyStream    = new PrintStream(new OutputStream(){
			public void write(int b) {}
		});

		setOutputStream(dummyStream);
	}

	public static void main(String[] args) throws RocketLoadException {
		run();
	}

	public static String run() {
		System.out.println("Started simulations");
		TeamRocket tr = new TeamRocket();

		System.out.println("Ended simulations");

		return tr.getStringifiedFlightData();
	}

	private void runFileSimulations(){
		for (Simulation s : document.getSimulations()) {
			SimulationListener simulationListener = new AbstractSimulationListener();

			listOfFlightData = simulateMultipleLaunchAngles(simulationListener, s);
		}
	}

	public ArrayList<FlightData> simulateMultipleLaunchAngles(SimulationListener simulationListener, Simulation s){
		ArrayList<FlightData> fd = new ArrayList<>();
		try {
			SimulationOptions simulationOptions = s.getOptions();

			//Simulate for each degree between 0 and 45 degrees
			for(double launchAngle = 0; launchAngle <= 45; launchAngle++) {
				simulationOptions.setLaunchRodAngle(Math.toRadians(launchAngle));
				s.setSimulationOptions(simulationOptions);
				s.simulate(simulationListener);
				FlightData f = s.getSimulatedData();
				f.setLaunchAngle(launchAngle);
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

	public String getStringifiedFlightData() {
		String stringifiedFlightData = "{";
		for (FlightData fd: listOfFlightData) {
			stringifiedFlightData +=
				"\n\tlaunch: {\n\t\t" +
					"launchAngle: " + fd.getLaunchAngle() + ",\n\t\t" +
					"launchStatistics: {\n" +
						fd.toString() +
					"\n\t\t}" +
				"\n\t}";
		}

		stringifiedFlightData += "\n}";

		return stringifiedFlightData;
	}

	private class DataTuple {

	}
}

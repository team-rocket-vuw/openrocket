import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import data.SimulationData;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.file.GeneralRocketLoader;
import net.sf.openrocket.file.RocketLoadException;
import net.sf.openrocket.plugin.PluginModule;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.startup.GuiModule;
import net.sf.openrocket.util.ArrayList;
import util.JSONFormatter;
import util.SimulationRunner;

import java.io.File;


/**
 * Main class:
 *
 * This class acts as the entry point for the team rocket application.
 *
 * When the jar is read from by the Python code, this class should be loaded. Once that is done,
 * simply instantiate a Team Rocket object and call #runSimulations
 *
 * Created September 21st 2016
 * Modified 3rd October 2016
 * By Marcel van Workum & Lauren Hucker
 */
public class TeamRocket {
	private String fileName;

	/**
	 * Instantiate a new team rocket instance.
	 *
	 * @param fileName name of ork file to be loaded
	 */
	public TeamRocket(String fileName){
		this.fileName = fileName;
	}

	/**
	 * Executes the simulations
	 *
	 * TODO this should take in a variable of config params so that we can run different types of simulations/pass in weather/gps location
	 *
	 * @return JSON string containing simulation results
	 */
	public String runSimulations() {
		System.out.println("Started simulations");

		ArrayList<SimulationData> simulationData = performSimulations();

		System.out.println("Ended simulations");

		return JSONFormatter.stringifySimulationData(simulationData);
	}

	private ArrayList<SimulationData> performSimulations() {
		// First we need to instantiate all of the open rocket dependencies
		dangerouslyInstantiateOpenRocketDependencies();

		// And then create a simulation runner, which will handle all of the simulations
		// We pass in the loaded .ork file into this
		SimulationRunner simulationRunner = new SimulationRunner(loadOpenRocketDocument());

		return simulationRunner.run();
	}

	private OpenRocketDocument loadOpenRocketDocument(){
		// Read the .ork file
		File file = new File(fileName);

		GeneralRocketLoader loader = new GeneralRocketLoader(file);
		OpenRocketDocument document = null;

		try {
			document = loader.load();
		} catch(Exception e){
			System.err.println(e.getMessage());
		}

		return document;
	}

	/*
	    This is very hacky, but essentially is instantiates all of the dependencies needed to make OpenRocket function
	 */
	private void dangerouslyInstantiateOpenRocketDependencies() {
		// Note that the GuiModule is not the GUI itself, but just a container for the GUI's dependencies.
		// This is required because it loads the component and motor databases
		GuiModule guiModule = new GuiModule();

		// Again, this is simply required for the application to load.
		Module pluginModule = new PluginModule();

		// Set up the injectors
		Injector injector = Guice.createInjector(guiModule, pluginModule);
		Application.setInjector(injector);

		// Asynchronously loads the component presets and motor database
		// However the motor database has been made to load synchronously to ensure it works with Python
		guiModule.startLoader();
	}

	// Entry point required to compile JAR artifact
	public static void main(String[] args) throws RocketLoadException { }
}

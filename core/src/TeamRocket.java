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
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.AbstractSimulationListener;
import net.sf.openrocket.simulation.listeners.SimulationListener;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.startup.GuiModule;

import java.io.File;

public class TeamRocket {
	public static void main(String[] args) throws RocketLoadException {
        File file = new File("Boosted Dart.ork");

        GuiModule guiModule = new GuiModule();
        Module pluginModule = new PluginModule();

        Injector injector = Guice.createInjector(guiModule, pluginModule);
        Application.setInjector(injector);

        guiModule.startLoader();

        Databases.fakeMethod();

        GeneralRocketLoader loader = new GeneralRocketLoader(file);
        OpenRocketDocument document = loader.load();

        for (Simulation s : document.getSimulations()) {
            SimulationListener simulationListener = new AbstractSimulationListener();
            try {
                s.simulate(simulationListener);
            } catch (SimulationException e) {
                e.printStackTrace();
            }

            FlightData f = s.getSimulatedData();
            System.out.println(f.getMaxAltitude());
            System.out.println(f.getFlightTime());
        }
    }
}

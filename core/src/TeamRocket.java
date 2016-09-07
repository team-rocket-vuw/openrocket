import java.io.File;
import java.util.List;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.file.GeneralRocketLoader;
import net.sf.openrocket.gui.util.OpenFileWorker;
import net.sf.openrocket.rocketcomponent.Rocket;

public class TeamRocket {
	File file;
	OpenRocketDocument orDoc;
	
	public TeamRocket(File file) {
		this.file = file;
		orDoc = loadDoc();
		//System.out.println(getSimulations(orDoc).get(0));
	}
	
	public OpenRocketDocument loadDoc() {
		OpenFileWorker openFileWorker = new OpenFileWorker(file);
		GeneralRocketLoader loader = openFileWorker.getRocketLoader();
		
		try {
			return loader.load();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public Rocket getRocket(OpenRocketDocument doc) {
		return doc.getRocket();
	}
	
	public List<Simulation> getSimulations(OpenRocketDocument doc) {
		return doc.getSimulations();
	}
	
	public static void main(String[] args) {
		File f = new File(args[0]);
		TeamRocket teamRocket = new TeamRocket(f);
		
	}
	
	
}

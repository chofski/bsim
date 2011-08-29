package BSimParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import javax.vecmath.Vector3d;

public class BSimParser {
	
	private static Random rng = new Random();
	
	public static void main (String[] args) {
		
		// Open the file from args
        File paramFile = new File(args[0]);        
        
        // Parse the file to generate a simulation
        BSimFromFile sim = BSimParser.parseFile(paramFile);
	
        // Run the simulation
        sim.run();
	}

	/**
     * Parses a parameter file and returns a new simulation.
     */
    public static BSimFromFile parseFile (File paramFile){

    	// Parameters object to hold the parsed file contents
    	BSimFromFile sim = new BSimFromFile();
    	Scanner scanner;
    	int lineCounter = 1;
    	
    	// Attempt to scan each line and process it
    	try { scanner = new Scanner(paramFile);
    	try {
    		while(scanner.hasNextLine()) {
    			processLine(scanner.nextLine().split(":"), sim, lineCounter);
    			lineCounter++;
    		}
    	} finally { scanner.close(); }
    	} catch(FileNotFoundException e) {System.err.println("Input file not found"); }

    	// Now that valid objects have been created, try to assign chemical fields to bacteria
    	sim.assignBacteriaChemicalFieldsFromNames();
    	
    	// Return the output parameters object
    	return sim;
    }
    
    /**
     * Use data following line header to set parameters in BSimFromFile object
     */
    private static void processLine (String[] line, BSimFromFile sim, int lineNo) {
    	
    	// --------------------------------------------------------------
    	// Simulation Parameters (inc. meshes, chemical fields, etc)
    	// --------------------------------------------------------------
    	
    	// Time Step
    	if (line[0].equals("DT")) 
    		sim.getSim().setDt(parseToDouble(line[1]));
    	
    	// Create a new chemical field, options of the form CHEM_FIELD:name:param1=value1,param2=value2...
    	// See BSimChemicalFieldFactory for more details.
    	else if(line[0].equals("MESH"))
    		sim.setMesh(line[1]);
    	
    	// Create a new bacterial population, options of the form BACTERIA:name:param1=value1,param2=value2...
    	// See BSimChemicalFieldFactory for more details.
    	else if(line[0].equals("BACTERIA"))
    		sim.addBacteria(line[1], BSimBacteriumFactory.parse(line[2], sim.getSim()));
    	
    	// Create a new particle population, options of the form PARTICLES:name:param1=value1,param2=value2...
    	// See BSimChemicalFieldFactory for more details.
    	else if(line[0].equals("PARTICLES"))
    		sim.addParticles(line[1], BSimParticleFactory.parse(line[2], sim.getSim()));
    	
    	// Create a new chemical field, options of the form CHEM_FIELD:name:param1=value1,param2=value2...
    	// See BSimChemicalFieldFactory for more details.
    	else if(line[0].equals("CHEM_FIELD"))
    		sim.addChemicalField(line[1], BSimChemicalFieldFactory.parse(line[2], sim.getSim()));
    	
    	// --------------------------------------------------------------
    	// Output Related Parameters
    	// --------------------------------------------------------------
    	
    	else if (line[0].equals("OUTPUT_PATH")) 
    		sim.setOutputPath(line[1]);
    	
    	else if (line[0].equals("OUTPUT_DATA")) {
    		if (parseToInt(line[1]) == 1) { sim.setOutputData(true); }
    		else { sim.setOutputData(false); }
    	}
    	
    	else if (line[0].equals("OUTPUT_MOVIE")) {
    		if (parseToInt(line[1]) == 1) { sim.setOutputMovie(true); }
    		else { sim.setOutputMovie(false); }
    	}
    	
    	else if (line[0].equals("DATA_FILENAME")) 
    		sim.setDataFileName(line[1]);
    	
    	else if (line[0].equals("MOVIE_FILENAME")) 
    		sim.setMovieFileName(line[1]);
    	
    	else if (line[0].equals("MOVIE_SPEED")) 
    		sim.setMovieSpeed(parseToInt(line[1]));
    	
    	else if (line[0].equals("MOVIE_DT")) 
    		sim.setMovieDt(parseToDouble(line[1]));
    	
    	else if (line[0].equals("MOVIE_WIDTH")) 
    		sim.setMovieWidth(parseToInt(line[1]));
    	
    	else if (line[0].equals("MOVIE_HEIGHT")) 
    		sim.setMovieHeight(parseToInt(line[1]));
    	
    	// --------------------------------------------------------------
    	// Unknown Parameters
    	// --------------------------------------------------------------
    	
    	// Generic comments of lines that are not use
    	else if(line[0].equals("***")) { } // Do nothing - comment
    	else System.err.println("Line " + lineNo + " not Read in Parameter File");
    }
    
    /**
     * Convert String into a double handling possible errors (returning 0.0 if problem found)
     */
    public static double parseToDouble (String str) {
    	double result;
    	try{ result = Double.parseDouble(str); }
    	catch(Exception e){ 
    		System.err.println("Problem converting to double");
    		result = 0.0;
    	}
    	return result;
    }
    
    /**
     * Convert String into an int handling possible errors (returning 0 if problem found)
     */
    public static int parseToInt (String str) {
    	int result;
    	try{ result = Integer.parseInt(str); }
    	catch(Exception e){ 
    		System.err.println("Problem converting to int");
    		result = 0;
    	}
    	return result;
    }
    
    /** Create a map of attribute names to their values (held as the original Strings) */
    public static HashMap<String,String> parseAttributeValuePairs (String str) {
    	HashMap<String,String> pairList = new HashMap<String,String>();
    	
    	// Split the string on ',' character to get pairs and the split 
    	// again on '=' to get an individual pair
    	
    	String [] strPairList = str.split(",");
    	for(String strPair : strPairList ){		
    		String [] strAttVal = strPair.split("=");
    		if (strAttVal.length != 2) {
    			System.err.println("Encountered invalid attribute value pair");
    			break;
    		}
    		else {
    			// Create the pair in the map
    			pairList.put(strAttVal[0], strAttVal[1]);
    		}
    	}
    	return pairList;
    }
    
    /** Generate a random position vector within specified bounds */
    public static Vector3d randomVector3d(Vector3d boundStart, Vector3d boundEnd) { 
    	double newX = 0.0;
    	double newY = 0.0;
    	double newZ = 0.0;

    	// Generate new random positions in the range
    	newX = boundStart.x + ((boundEnd.x-boundStart.x) * rng.nextDouble());
    	newY = boundStart.x + ((boundEnd.x-boundStart.x) * rng.nextDouble());
    	newZ = boundStart.x + ((boundEnd.x-boundStart.x) * rng.nextDouble());

    	return new Vector3d(newX, newY, newZ);
    }
}
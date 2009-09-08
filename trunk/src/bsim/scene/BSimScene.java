/**
 * BSimScene.java
 *
 * Class that represents the scene of a simulation. It extends a JPanel which allows
 * for it to be easily embedded into any swing based GUI. Additionally, this class 
 * maintains the vectors of all bacteria and beads, as well as the physcis engine 
 * used to update the position and properties of all objects in the simulation. The class
 * is threaded to ensure that the main GUI event thread continues to function and
 * control of animaton is via a semaphre implemented as a notifiable object to ensure that
 * minimal processing is carried out when paused.
 *
 * Authors: Thomas Gorochowski
 *          Ian Miles
 *          Charlie Harrison
 *          Mattia Fazzini
 * Created: 12/07/2008
 * Updated: 26/08/2009
 */
package bsim.scene;

import java.util.Vector;

import javax.vecmath.Vector3d;

import bsim.app.BSimApp;
import bsim.app.BSimSemaphore;
import bsim.field.BSimChemicalField;
import bsim.particle.BSimBacterium;
import bsim.particle.BSimBead;
import bsim.particle.BSimParticle;
import bsim.particle.BSimVesicle;


public class BSimScene implements Runnable{
	
	public static double dt = 0.01; // seconds	
	public static double xBound 	= 100;
	public static double yBound 	= 100;
	public static double zBound		= 100;
	
	// Variables and constants for the animation state
	public static final int PLAYING = 1;
	public static final int PAUSED = 2;
	private int playState = PAUSED;
			
	// Vectors holding all bacteria and beads in the simulation
	private Vector<BSimBacterium> bacteria;
	private Vector<BSimBead> beads;
	private Vector<BSimVesicle> vesicles;
	private Vector<BSimVesicle> vesiclesToRemove;
	
	// Chemical fields required for the simulation
	private BSimChemicalField fGoal;
	private BSimChemicalField fQuorum;
	
	// Number of time steps that have occured in current simulation
	private int timeStep = 0;
	
	// Thread to run the simulation in
	private Thread simThread;
	// Semaphore to control the play/pause commands
	private BSimSemaphore simSem;
	private BSimSemaphore renderSem;
	
	// The applications that the simulation is embedded
	// (required for changes to time to be sent back to the GUI)
	private BSimApp app = null;
		
	// Parameters for the scene	
	public boolean guiExists = false;	
	
	public boolean startVideo = false;
	public boolean endVideo = false;	
	public String imageFileName = null;		

	//BSimBatch parameter for waiting the rigth closing of the file
	public boolean waitingForVideoClosing = true;
	public boolean waitingForVideoOpening = true;
	
	
	/**
	 * General constructor for use when GUI is present
	 */
	public BSimScene(BSimSemaphore newSimSem, BSimApp newApp)
	{
		super();
		
		//this.addComponentListener(this);
		
		guiExists = true;
				
		// Update the internal variables
		simSem = newSimSem;
		renderSem = new BSimSemaphore();
		app = newApp;
				
	    
		// Create initial bacteria and beads
		resetScene(1);
		
		// Create new thread to run the simulation in an associate with this object
		simThread = new Thread(this);
		// Start the simulation thread
		simThread.start();
	}
	
	
	/**
	 * General constructor for use with no GUI
	 */
	public BSimScene()
	{
		super();
				
		// Update the internal variables
		simSem = null;
		app = null;
				
		// Create initial bacteria and beads
		resetScene(1);
	}
	
	
	/**
	 * Reset the scene creating bactera and beads.
	 */
	private void resetScene(int firstTime) {
					
		// Move back to first time-step 
		timeStep = 0;				
		bacteria = new Vector<BSimBacterium>();
		beads = new Vector<BSimBead>();
		vesicles = new Vector<BSimVesicle>();		
		
		// Create the bacteria and beads
		Vector3d startPos = new Vector3d();
		double width = BSimScene.xBound;
		double height = BSimScene.yBound;
		double depth = BSimScene.zBound;
		while(bacteria.size() < 100) {
			Vector3d position = new Vector3d();
			Vector3d offset = new Vector3d(Math.random()*width, Math.random()*height, Math.random()*depth);			
			position.add(startPos, offset);
			BSimBacterium b = new BSimBacterium(position, 1, new Vector3d(Math.random(),Math.random(),Math.random()), this);
			if(!intersection(b)) bacteria.add(b);			
		}
	
		
		// Reset the renderer with new scene data
		if(app != null && app.getRenderer() != null){
			app.resetDisplay(firstTime);
		}

	}
	
	
	
	private boolean intersection(BSimParticle p) {
		for(BSimBacterium b : bacteria)
			if (p.outerDistance(b) < 0) return true;
		for(BSimBead b : beads)
			if (p.outerDistance(b) < 0) return true;	
		return false;
	}	

	
	
	
	
	/**
	 * Update the parameters that are used
	 */
	public void updateParams () {
				
		// Reset the scene to recreate all objects and ensure local variables are consistent
		this.reset();
	}
	
	
	/**
	 * The main thread loop. This handles the animation of the simulation using 
	 * notifiable objects to ensure that when paused no additional processing
	 * resources are used.
	 */
	public void run(){
		// Loop forever (until application closes)
		do{
			// Need to catch exceptions because sleep method is called
			try{
				
				// Check to see if playback state has changed
				if(playState == PAUSED) {
					// If paused wait on notifiable object (semaphore)
					simSem.waitOn();
				}
				
				// Wait the for the time-step
				Thread.sleep((int)(1000*BSimScene.dt));
				
				// Update all the elements in the scene
				runAllUpdates();
				
				// Redraw the scene for this frame
				app.getRenderer().redraw();
				// All threads wait for the redraw (or we may get concurrent modification)
				renderSem.waitOn();
				
				// Update the time-step
				timeStep++;
				
			}
			catch(InterruptedException error){};
		}while(true);
	}
	
	
	private void runAllUpdates(){
						
//		for(int i = 0; i < bacteria.size(); i++) {
//			for(int j = i+1; j < bacteria.size(); j++) {
//				bacteria.get(i).interaction( bacteria.get(j));
//			}		
//		}
//		
		
		for(BSimBacterium bacterium : bacteria) {
			vesiclesToRemove = new Vector<BSimVesicle>();
			for(BSimVesicle vesicle : vesicles) {
				bacterium.interaction(vesicle);				
			}
			vesicles.removeAll(vesiclesToRemove);
		}	
		
		for(BSimBacterium p : bacteria) {
			p.action();
			p.updatePosition();
		}		
		for(BSimBead p : beads) {
			p.action();
			p.updatePosition();
		}		
		for(BSimVesicle p : vesicles) {
			p.action();
			p.updatePosition();
		}
								
		// Update the fields
		if(fGoal != null) fGoal.updateField();
		if(fQuorum != null) fQuorum.updateField();
		
	}
	
	
	/**
	 * Signal renderer semaphore (e.g. that redraw has completed)
	 */
	public void signalRenderSem() {
		renderSem.signal();
	}
		
	
	/**
	 * Plays the current simulation.
	 */
	public void play() {
		// Update the playback state (if required)
		if(playState == PAUSED) {
			playState = PLAYING;
		}
	}
	
	
	/**
	 * Pauses the current simulation.
	 */
	public void pause() {
		// Update the playback state (if required)
		if(playState == PLAYING) {
			playState = PAUSED;
		}
	}
	
	
	/**
	 * Resets the simulation.
	 */
	public void reset() {
		// Update state variables
		playState = PAUSED;
		
		// Recreate all simulation objects
		resetScene(0);
	}
	

	/**
	 * Skips the simulation forward a given number of frames. Intermediate frames still
	 * have to be computed.
	 */
	public void skipFrames(int numOfFrames) {
		// Loop through the necessary frames and update positions of objects
		for(int i=0; i<numOfFrames; i++){
			// Update all the elements in the scene
			runAllUpdates();
			
			// Update the time-step
			timeStep++;
			
		}
	}
	
	
	/**
	 * Standard get methods for the class.
	 */	
	public BSimApp getApp() { return app; }
	
	public Vector getBacteria (){ return bacteria; }	
	public Vector getBeads (){ return beads; }	
	public Vector getVesicles (){ return vesicles; }
	public void addVesicle(BSimVesicle b){ vesicles.add(b); }
	public void removeVesicle(BSimVesicle b){ vesiclesToRemove.add(b); }
		
	public int getTimeStep (){ return timeStep; }
	public BSimChemicalField getGoalField (){ return fGoal; }
	public BSimChemicalField getQuorumField() { return fQuorum; }	
		
	public void setStartVideo (boolean b){ startVideo=b; }
	public void setEndVideo (boolean b){ endVideo=b; }
	public void setImageFileName (String s){ imageFileName=s; }
	
	public int getPlayState () { return playState; }
	
	public boolean getWaitingForVideoClosing(){return waitingForVideoClosing;}	
	public boolean getWaitingForVideoOpening(){return waitingForVideoOpening;}
	public void setWaitingForVideoOpening(boolean b){waitingForVideoOpening=b;}	

}


package bsim.scene;

import java.util.Vector;

import javax.vecmath.Vector3d;

import bsim.app.BSimApp;
import bsim.app.BSimSemaphore;
import bsim.app.gui.BSimGUI;
import bsim.field.BSimChemicalField;
import bsim.particle.BSimBacterium;
import bsim.particle.BSimBead;
import bsim.particle.BSimParticle;
import bsim.particle.BSimVesicle;

public abstract class BSimScene {
	
	// If these are going to be changed in the setup, might have to eventually do something clever
	// to make them more private as they are static so changing them by accident will make a huge mess
	
	// Global simulation time step (seconds)
	public static double dt = 0.01;
	
	// Simulation boundary dimensions
	public static double xBound 	= 100;
	public static double yBound 	= 100;
	public static double zBound		= 100;
	
	// Variables and constants for the animation state
	public static final int PLAYING = 1;
	public static final int PAUSED = 2;
	private int playState = PAUSED;
			
	// Vectors holding all bacteria and beads in the simulation
	private Vector<BSimBacterium> bacteria = new Vector<BSimBacterium>();
	private Vector<BSimBead> beads = new Vector<BSimBead>();
	private Vector<BSimVesicle> vesicles = new Vector<BSimVesicle>();
	private Vector<BSimVesicle> vesiclesToRemove = new Vector<BSimVesicle>();
	
	// Chemical fields required for the simulation
	private BSimChemicalField fGoal		= null;
	private BSimChemicalField fQuorum	= null;
	
	// Number of time steps that have occurred in current simulation
	private int timeStep = 0;
	
	// Thread to run the simulation in
	private Thread simThread;
	
	// Semaphore to control the play/pause commands
	private BSimSemaphore simSem;
	private BSimSemaphore renderSem;
	
	// The BSimApp that runs the simulation
	// (required for changes to time to be sent back to the GUI)
	private static BSimApp app;
	//private static BSimScene scene;
	
	public boolean startVideo = false;
	public boolean endVideo = false;	
	public String imageFileName = null;		

	//BSimBatch parameter for waiting the rigth closing of the file
	public boolean waitingForVideoClosing = true;
	public boolean waitingForVideoOpening = true;
	
	
	/**
	 * General constructor
	 */
	public BSimScene(BSimApp newApp)
	{
		super();
		app = newApp;				
		// Update the internal variables
		simSem = new BSimSemaphore();;
		renderSem = new BSimSemaphore();
	    
		// Create initial bacteria and beads
		//resetScene(1);
		//******************************************************** Threads ********************************************************
		// Create new thread to run the simulation in an associate with this object
		//simThread = new Thread(this);
		// Start the simulation thread
		//simThread.start();
		
	}
	
	
	/**
	 * Reset the scene 
	 */
	private void resetScene(int firstTime) {
		
		// Move back to first time-step 
		timeStep = 0;
		//if(firstTime == 1){
		//	app = new BSimApp(this);
		//}
		app.reset(firstTime);
	}
	
	
	/**
	 * Check for intersection between some particle p and all bacteria and beads in the scene
	 */
	protected boolean intersection(BSimParticle p) {
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
	 * Update all of the scene elements for one time step
	 * TODO: multithreading?
	 */
	public void update(){
								
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
	public void advance(int frameSkip) {
		
		// Need to catch exceptions because sleep method is called
		for(int i=0; i<frameSkip; i++){
			//try{
				// Check to see if playback state has changed
//				if(playState == BSimScene.PAUSED) {
//					// If paused wait on notifiable object (semaphore)
//					simSem.waitOn();
//				}
				
				// Wait the for the time-step
				//Thread.sleep((int)(1000*dt));
				
				// Update all the elements in the scene for one time step
				update();
				
				// Redraw the scene for this frame
				if(BSimGUI.guiState()){
					app.getGUI().getRenderer().redraw();
					// All threads wait for the redraw (or we may get concurrent modification)
					app.getRenderSem().waitOn();
				}
				
				// Update the time-step
				timeStep++;
			
			//} catch(InterruptedException error){};
		}		
	}
	
	
	/**
	 * The main thread loop. This handles the animation of the simulation using 
	 * notifiable objects to ensure that when paused no additional processing
	 * resources are used.
	 */
	//public void run(){
		// Loop forever (until application closes)
		//do{
			
		//}while(true);
	//}
	
	/**
	 * Standard get methods for the class.
	 */	
	public static BSimApp getApp() { return app; }
	public static void setApp(BSimApp newApp) { app = newApp; }
	public static void runApp() { app.runApp(); }
	
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

}

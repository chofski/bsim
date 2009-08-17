/**
 * BSimRecruitBacterium.java
 *
 * Class that represents a bacterium that by default will move randomly, until contact 
 * with a bead is made at which time it will follow the goal chemoattractant. A 
 * co-ordination signal will also be released that will cause any bacteria in a high 
 * enough concentration to also follow the chemoattractant. Also, a recruitment signal is
 * produced on contact with a bead that all bacteria will follow by default.
 *
 * Authors: Thomas Gorochowski
 * 			Mattia Fazzini(Update)
 * Created: 01/09/2008
 * Updated: 12/08/2009
 */
package bsim.drawable.bacteria;

import java.awt.Color;
import java.awt.Graphics;

import bsim.BSimParameters;
import bsim.BSimScene;
import bsim.drawable.BSimDrawable;
import bsim.logic.BSimLogic;


public class BSimRecruitBacterium extends BSimCoordBacterium implements BSimLogic, BSimDrawable {

	protected boolean foundRecruit = false;
	
	/**
	 * General constructor for a trilinear elongation bacteria.
	 */
	public BSimRecruitBacterium(double newSpeed, double newMass,
			double newL0, double newR, double newTC, double newT2, double newTG, double newa1, double newa2, double newa3, int newElongationType,
			double[] newDirection, double[] newPosition, double newForceMagnitudeDown,
			double newForceMagnitudeUp,
			int newState, double newTumbleSpeed, int newRemDt, BSimScene newScene, 
		    BSimParameters newParams, double newSwitchSpeed, double newCoordThreshold) {
			
		super(newSpeed, newMass,
				newL0, newR, newTC, newT2, newTG, newa1, newa2, newa3, newElongationType,
				newDirection, newPosition, newForceMagnitudeDown,
				newForceMagnitudeUp,
				newState, newTumbleSpeed, newRemDt, newScene, 
				newParams, newSwitchSpeed, newCoordThreshold);
	}
	
	/**
	 * General constructor for bilinear elongation .
	 */
	public BSimRecruitBacterium(double newSpeed, double newMass,
			double newL0, double newLTC, double newLTG, double newR, double newTC, double newTG, int newElongationType,
			double[] newDirection, double[] newPosition, double newForceMagnitudeDown,
			double newForceMagnitudeUp,
			int newState, double newTumbleSpeed, int newRemDt, BSimScene newScene, 
		    BSimParameters newParams, double newSwitchSpeed, double newCoordThreshold) {
		
		super(newSpeed, newMass,
				newL0, newLTC, newLTG, newR, newTC, newTG, newElongationType,
				newDirection, newPosition, newForceMagnitudeDown,
				newForceMagnitudeUp,
				newState, newTumbleSpeed, newRemDt, newScene, 
			    newParams, newSwitchSpeed, newCoordThreshold);
	}


	/**
	 * Implements the BSimLogic interface. In this case it merely carries out
	 * the standard chemotaxis toward fGoal gradient. The internal force of the bacterium
	 * at a timestep is returned.
	 */
	public double[] runLogic ( boolean contactBac, 
	                           boolean contactBead,
	                           boolean contactBoundary ) {
		
		int newChemo = 0;
		
		// Need to check if a switch to chemo has been made.
		if(beadContactTimer > 0 || 
		   scene.getCoordinationField().getConcentration(this.getCentrePos()) > coordThreshold) {
			newChemo = BAC_CHEMO_GOAL;
		}
		else{
			newChemo = BAC_CHEMO_RECRUIT;
		}
		
		if(newChemo != chemo){
			// Reset the memory
			memToReset = true;
		}
		
		// Update the gradient to use
		chemo = newChemo;
		
		if(beadContactTimer > 0){
				// Generate some recruitment chemical at current location
				scene.getRecruitmentField().addChemical (1.0, this.getCentrePos());
		}
		
		return  super.runLogic(contactBac, contactBead, contactBoundary);
	}

	
	/**
	 * This is an updated version of the BSimBacterium method to only allow for the
	 * sensed goal concentration to be used if in contact with a bead.
	 */
	protected double senseRunContinueProb() {
		double shortTermMean;
		double longTermMean;
		double shortTermCounter = 0.0;
		double longTermCounter = 0.0;
		double shortTermMemoryLength = 1.0; // seconds
		double longTermMemoryLength = 3.0; // seconds
		double sensitivity = 0.000001;
		
		// Perform the normal attraction to the goal chemoattractant
		for(int i=0; i<concMemory.size();i++) {
			if(i <= (longTermMemoryLength/params.getDtSecs())) {
				longTermCounter = longTermCounter + (Double)concMemory.elementAt(i);
			} else shortTermCounter = shortTermCounter + (Double)concMemory.elementAt(i);
		}
		shortTermMean = shortTermCounter / (1 + (shortTermMemoryLength/params.getDtSecs()));
		longTermMean = longTermCounter / (longTermMemoryLength/params.getDtSecs());
	
		if(shortTermMean - longTermMean > sensitivity) {
			foundRecruit = true;
			runUp = true;
			return upRunProb;
		}
		else if(longTermMean - shortTermMean > sensitivity){
			foundRecruit = false;
			runUp = false;
			return downRunProb;
		}
		else {
			foundRecruit = false;
			runUp = false;
			return isoRunProb;
		}
	}
	
	
	/*
	 * Replication function
	 */
	
	public BSimRecruitBacterium replicate(BSimScene scene, BSimParameters params){
		double[] newCentrePosBact2 = new double[3];
		double[] newPosition = new double[3];
		double beta = Math.atan2(direction[1],direction[0]);
		double alpha = Math.acos((direction[2])/Math.sqrt(Math.pow(direction[0], 2.0)+Math.pow(direction[1], 2.0)+Math.pow(direction[2], 2.0)));
		this.size=this.size/2;
		newCentrePosBact2[0] = centrePos[0]+((this.size/2)*Math.sin(alpha)*Math.cos(beta));
		newCentrePosBact2[1] = centrePos[1]+((this.size/2)*Math.sin(alpha)*Math.sin(beta));
		newCentrePosBact2[2] = centrePos[2]+((this.size/2)*Math.cos(alpha));
		newPosition[0]=newCentrePosBact2[0]-(this.size/2);
		newPosition[1]=newCentrePosBact2[0]-(this.size/2);;
		newPosition[2]=newCentrePosBact2[0]-(this.size/2);;
		centrePos[0] = centrePos[0]-((this.size/2)*Math.sin(alpha)*Math.cos(beta));
		centrePos[1] = centrePos[1]-((this.size/2)*Math.sin(alpha)*Math.sin(beta));
		centrePos[2] = centrePos[2]-((this.size/2)*Math.cos(alpha));
		setCentrePos(centrePos);
		BSimRecruitBacterium newBact = null;
		if(elongationType==BSimBacterium.TRILINEAR_ELONGATION){
			//inherit the trilinear elongation
			newBact = new BSimRecruitBacterium(this.speed, this.mass, this.size, this.width, this.timeC, this.time2, this.timeG, this.a1minute, this.a2minute, this.a3minute, this.elongationType, this.direction, newPosition, this.forceMagnitudeDown, this.forceMagnitudeUp, this.state, this.speed, this.remDt, scene, params, this.switchSpeed, this.coordThreshold);
		}
		else{
			//inherit the bilinear elongation
			newBact = new BSimRecruitBacterium(this.speed, this.speed, this.size, this.ltc, this.ltg, this.width, this.timeC, this.timeG, this.elongationType, this.direction, newPosition, this.forceMagnitudeDown, this.forceMagnitudeUp, this.state, this.speed, this.remDt, scene, params, this.switchSpeed, this.coordThreshold);
		}
		newBact.startNewPhase();
		scene.setReallocateNewForceMat(true);
		return newBact;
	}
	
	/**
	 * Redraws the bacterium. A small red circle is also drawn to represent the direction
	 * of the bacteria.
	 */
	public void redraw(Graphics g) {

		// Draw the main shape of bacterium
		if(beadContactTimer > 0){
			g.setColor(Color.BLUE);
		}
		else if(scene.getCoordinationField().getConcentration(this.getCentrePos()) > coordThreshold){
			g.setColor(Color.YELLOW);
		}
		else if(foundRecruit == true){
			g.setColor(Color.RED);
		}
		else{
			g.setColor(Color.GREEN);
		}
		
		g.fillOval((int)position[0],(int)position[1],(int)(size),(int)(size));

		// Draw an indicator of bacterium's direction
		//int x1,x2;
		//double littleR = size/5.0;
		//x1 = (int)(position[0] + (size/2.0)*(1+direction[0]) - (littleR/Math.sqrt(2.0)));
		//x2 = (int)(position[1] + (size/2.0)*(1+direction[1]) - (littleR/Math.sqrt(2.0)));
		//g.setColor(Color.RED);
		//g.fillOval(x1,x2,(int)(littleR*2.0),(int)(littleR*2.0));
	}
}
/**
 * BSimBoundary.java
 *
 * Class that represents the borders of the area that is wrapped around on itself.
 * 
 * N.B. When creating wrapping boundaries, care must be taken to avoid offsets that place 
 * objects outside of the desired area.  It is also advisable to make the area 
 * large enough so that beads are very unlikely to reach the edges.
 *
 * Authors: Sophie Woods
 *          Charlie Harrison
 *          Thomas Gorochowski (Updates)
 * Created: 01/08/2008
 * Updated: 24/08/2008
 */
package bsim.drawable.boundary;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.Vector;

import bsim.drawable.BSimDrawable;
import bsim.drawable.bacteria.BSimBacterium;
import bsim.drawable.bead.BSimBead;
import bsim.physics.BSimParticle;

public class BSimWrapBoundary extends BSimBoundary implements BSimDrawable {
	// The offset given to an object that intersects with a boundary of type WRAP
	private double[] wrapOffset = {0.0,0.0};
	
	// The vector that is normal to the boundary and has magnitude equal to the length of the boundary
	private double[] normVect;
		
	private double length;
	
	boolean resetBacMem = false;
	
	
	/**
	 * General constructor.
	 */
	public BSimWrapBoundary(double[] newp1, double[] newp2, double[] newWrapOffset, double newResetBacMem) {

		super(newp1, newp2);   

		// Call the parent constructor with the basic properties	
		//super(newSpeed, newMass, newSize, newDirection, newPosition, BSimObject.OBTYPE_BACT);
		normVect = new double[2];
		
		normVect[0]   = p1[1] - p2[1];
		normVect[1]   = p2[0] - p1[0];
		
		length = Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
		
		wrapOffset[0] = newWrapOffset[0];
		wrapOffset[1] = newWrapOffset[1];
		
		if(newResetBacMem == 1){
			resetBacMem = true;
		}
		else{
			resetBacMem = false;
		}
	}

	/**
	 * Method to deal with boundary collisions
	 */
	public void boundaryCollisions(Vector bacteria, Vector beads) {
		
		int n = bacteria.size();
		int m = beads.size();
		BSimBacterium bact;
		BSimBead bead;
		
		// Loop through each bacterium
		for (int i=0; i < n; i++) {
			// Get next bacterium
			bact = (BSimBacterium)bacteria.elementAt(i);
			
			// Check for collision with boundary; displace object if necessary 
			if (isColliding(bact)) {
				displace((BSimParticle)bact);
				// Reset their memory
				bact.setMemToReset(true);
			}
		}
		
		// Loop through all beads
		for (int i=0; i < m; i++) {
			// Get next bead
			bead  = (BSimBead)beads.elementAt(i);
			
			// Check for collision with boundary; displace object if necessary 
			if (isColliding(bead)) displace((BSimParticle)bead);
		}
	}
	
	/**
	 * Return a boolean to indicate whether or not an object is colliding with the boundary.  This is based on
	 * the minimum distance to the boundary, which is either the perpendicular distance or the distance to the 
	 * nearest end 
	 */
	public boolean isColliding(BSimParticle x) {
		double[] direction =x.getDirection();
		double[] point = x.getCentrePos();
		double radius = x.getSize()/2;
		double dist;
		
		double perpDist = distPointToLine(point) * length;
		double t = intersectPos(point);
		
		// If the point has no perpendicular intersection with the line, take the distance to the nearest end
		if (t>1 || t<0) {
			return false;
			//dist = Math.min(BSimUtils.get2Ddist(point, p1), BSimUtils.get2Ddist(point, p2));
		} else {
			dist = perpDist;
		}
				
		if (dist - radius < 0) {
			return true;
			
		} else if (dist - 10 < 0) {
			double[] vector = {point[0] + direction[0],point[1] + direction[1]};
			double perpDistVector = distPointToLine(vector) * length;
			
			if (perpDistVector > perpDist) {
				return true;
			}
			else return false;	
		}
		else return false;
	}

	
	/**
	 * Return the absolute value of the perpendicular distance between a point and the boundary as a 
	 * proportion of the length of the boundary
	 */
	public double distPointToLine(double[] point) {
		double dist;
		
		double xp = point[0];
		double yp = point[1];
		double x1 = p1[0];
		// double x2 = p2[0];
		double y1 = p1[1];
		// double y2 = p2[1];
		double c = normVect[0];
		double d = normVect[1];
		
		/*
		 * Calculate the distance to the line as a proportion of the normal vector; this
		 * expression was found by solving a system of linear vector equations that give
		 * the point of intersection
		 */
		dist = (c*(x1 - xp) + d*(y1 - yp))/(Math.pow(c,2) + Math.pow(d,2));
		
		return Math.abs(dist);
	}
	
	/**
	 * Return the position of the perpendicular intersection of a point along the boundary as a proportion 
	 * of the length of the boundary
	 */
	public double intersectPos(double[] point) {
		double t;
		
		double xp = point[0];
		double yp = point[1];
		double x1 = p1[0];
		// double x2 = p2[0];
		double y1 = p1[1];
		// double y2 = p2[1];
		double c = normVect[0];
		double d = normVect[1];
		
		/*
		 * The expression for t gives the position along the boundary of the intersection
		 * point; if t is not in the range [0,1] then the point is "off the end" of the 
		 * line
		 */
		t = (c*(y1 - yp) + d*(xp - x1))/(Math.pow(c,2) + Math.pow(d,2));
		
		return t;
	}
	
	/**
	 * Move an object according to the wrapOffset vector
	 */
	public void displace(BSimParticle x) {
		double[] offset = new double[2];
		double[] newCentrePos = new double[2];
		
		offset[0] = wrapOffset[0] - x.getSize()*wrapOffset[0]/Math.abs(wrapOffset[0]);
		offset[1] = wrapOffset[1] - x.getSize()*wrapOffset[1]/Math.abs(wrapOffset[1]);
		
		for (int i = 0; i<2; i++) if (Double.isNaN(offset[i])) offset[i] = 0;
		
		newCentrePos[0] = x.getCentrePos()[0] + offset[0];
		newCentrePos[1] = x.getCentrePos()[1] + offset[1];
		
		x.setCentrePos(newCentrePos);
	}
	
	
	/**
	 * Draw the boundary.
	 */
	public void redraw(Graphics g) {
		g.setColor(Color.GRAY);
		Graphics2D g2d = (Graphics2D)g;
		// dash length, space length
		float[] dashValues = { 8.0f, 5.0f } ;
		Stroke stroke = new BasicStroke( 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashValues, 0 ) ;
 		g2d.setStroke( stroke ) ;
 		Line2D line = new Line2D.Double( (int)p1[0], (int)p1[1], (int)p2[0], (int)p2[1] ) ;
 		g2d.draw( line ) ;
		g2d.setStroke( new BasicStroke() ) ;

	}
}

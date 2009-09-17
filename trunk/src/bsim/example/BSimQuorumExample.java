package bsim.example;

import java.awt.Color;
import java.util.Vector;

import javax.vecmath.Vector3d;

import processing.core.PGraphics3D;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.BSimTicker;
import bsim.draw.BSimP3DDrawer;
import bsim.export.BSimPngExporter;
import bsim.export.BSimLogger;
import bsim.export.BSimMovExporter;
import bsim.export.BSimPdfExporter;
import bsim.particle.BSimBacterium;

public class BSimQuorumExample {

	public static void main(String[] args) {

		BSim sim = new BSim();		
		sim.setDt(0.01);
		sim.setSimulationTime(10);
		sim.setTimeFormat("0.00");
		sim.setBound(100,100,100);
		
		final double diffusivity = 1000; // (microns)^2/sec
		final double decayRate = 0.05;
		final double productionRate = 1e9; // molecules/sec
		final double threshold = 1e4;  // molecules/(micron)^3
		final double activationDelay = 1;
		final BSimChemicalField field = new BSimChemicalField(sim, new int[]{10,10,10}, diffusivity, decayRate);
	
		class BSimQuorumBacterium extends BSimBacterium {
			private boolean activated = false;
			private double lastActivated = -1;

			public BSimQuorumBacterium(BSim sim, Vector3d position) {
				super(sim, position); // default radius is 1 micron			
			}

			@Override
			public void action() {
				super.action();				
				if(field.getConc(position) > threshold) {
					activated = true; 
					lastActivated = sim.getTime();
				}
				else {
					activated = false;
					if(lastActivated == -1 || (sim.getTime() - lastActivated) > activationDelay) {				
						field.addQuantity(position, productionRate*sim.getDt());
					}
				}
			}
		}		
		final Vector<BSimQuorumBacterium> bacteria = new Vector<BSimQuorumBacterium>();		
		while(bacteria.size() < 100) {		
			BSimQuorumBacterium p = new BSimQuorumBacterium(sim, new Vector3d(Math.random()*sim.getBound().x, Math.random()*sim.getBound().y, Math.random()*sim.getBound().z));
			if(!p.intersection(bacteria)) bacteria.add(p);		
		}


		sim.setTicker(new BSimTicker() {
			@Override
			public void tick() {
				for(BSimQuorumBacterium b : bacteria) {
					b.action();		
					b.updatePosition();
				}
				field.update();
			}		
		});


		BSimP3DDrawer drawer = new BSimP3DDrawer(sim, 800,600) {
			@Override
			public void scene(PGraphics3D p3d) {		
				draw(field, Color.BLUE, (float)(255/(2*threshold)));
				for(BSimQuorumBacterium p : bacteria) {
					draw(p, p.activated ? Color.RED : Color.GREEN);
				}			
			}
		};
		sim.setDrawer(drawer);				

		
		sim.preview();

	}




}
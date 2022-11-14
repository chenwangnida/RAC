package main;

import java.util.ArrayList;

import org.apache.commons.math3.util.MathUtils;

import ec.Evolve;

public class Main {
	
	public static ArrayList cpuUtilization = new ArrayList();
	public static ArrayList wastedMen = new ArrayList();

	public static int newPMNo = 0;
	public static int newVMNo = 0;

	public static void main(String[] args) {
		String param = args[0];
		String seed = args[1];
		int numberOfRuns = Integer.valueOf(args[2]);
		String method = args[3];
		String test = args[4]; // training or testing
		String pathToFiles = args[5]; //path to save outputs, e.g., "./outputs/Bitbrains_CCGP_3OS"


		System.out.println(param);
		System.out.println(seed);

		// Set up the parameters. Include the parameter settings, output file names,
		// and output form
		String[] runConfig = new String[] { Evolve.A_FILE, param, "-p",
				("stat.file=$" + pathToFiles + "/" + seed + "/out.stat"), "-p", ("jobs=" + numberOfRuns), "-p",
				("seed.0=" + seed), method, test , pathToFiles};
		long startTime = System.currentTimeMillis();
		Evolve.main(runConfig);
		long endTime = System.currentTimeMillis();
		System.out.println("============================================================");
		System.out.println("Total Execution Time : " + (endTime - startTime) / 1000);
		System.out.println("************************************************************");

	}
	
	public static double avgCPUUtilPerContainer (ArrayList<Double[]> l) {
	    double sum = 0;
	    for (Double[] i: l) {
	        sum += (1-i[0]/i[5]);
	    }
	    return sum/l.size();
	}
	
	public static double avgWastedMemPerContainer (ArrayList<Double[]> l) {
	    double sum = 0;
	    for (Double[] i: l) {
	        sum += i[1];
	    }
	    return sum/l.size();
	}
	
	public static double sum(ArrayList<Double> l) {
	    double sum = 0;
	    for (double i: l) {
	        sum += i;
	    }
	    return sum;
	}
}

package main;

import com.opencsv.CSVReader;
import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.coevolve.GroupedProblemForm;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.util.Parameter;
import expressionParser.ReadExpression;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

public class ContainerAllocationProblem extends GPProblem implements GroupedProblemForm {
	public static final String P_DATA = "data";
	boolean shouldSetContext;
	MainAllocationProcessContainer mainAllocationProcessContainer;
	MainAllocationProcessContainer2 mainAllocationProcessContainer2;
	MainAllocationProcessContainerTest mainAllocationProcessContainerTest;
	MainAllocationProcessContainer_Ind mainAllocationProcessContainer_Ind;
	MainAllocationProcessContainer_Ind2 mainAllocationProcessContainer_Ind2;
	MainAllocationProcessContainer_IndTest mainAllocationProcessContainerIndTest;
	ArrayList<Double> benchmarkResult;
	SubJustFit_FF benchmark;

	// Initialization data
	ArrayList<ArrayList> initPm;
	ArrayList<ArrayList> initPmType;
	ArrayList<ArrayList> initVm;
	ArrayList<ArrayList> initVmType;
	ArrayList<ArrayList> initOs;
	ArrayList<ArrayList> initContainer;

	// A list of containers, inputX[0] is for cpu, [1] is for mem [2] for os [3] for
	// hours
	private ArrayList<ArrayList<Double[]>> inputX = new ArrayList<>();

	// A list of candidate VMs, each vm has an array which includes its CPU and Mem
	// capacity
	private ArrayList<Double[]> vmTypeList = new ArrayList<>();
	private ArrayList<Double[]> pmTypeList = new ArrayList<>();

	// An array of OS probability
	private ArrayList<Double> OSPro = new ArrayList<>();

	@Override
	public void setup(final EvolutionState state, final Parameter base) {

		// very important
		super.setup(state, base);
		if (!(input instanceof DoubleData)) {
			state.output.fatal("GPData class must subclasses from " + DoubleData.class, base.push(P_DATA), null);
		}

		int method = Integer.valueOf(state.runtimeArguments[8]);
		String isTesting = state.runtimeArguments[9];

		Parameter vmCPUOverheadRateP = new Parameter("VMCPUOverheadRate");
		Parameter vmMemOverheadP = new Parameter("VMMemOverhead");
		Parameter readFileStartFromP = new Parameter("readFileStartFrom");
		Parameter readFileEndP = new Parameter("readFileEnd");

		Parameter vmConfigPathP = new Parameter("vmConfigPath");
		Parameter pmConfigPathP = new Parameter("pmConfigPath");
		Parameter osProP = new Parameter("osProPath");
		Parameter envPath = new Parameter("initEnvPath");

		double vmCpuOverheadRate = state.parameters.getDouble(vmCPUOverheadRateP, null);
		double vmMemOverhead = state.parameters.getDouble(vmMemOverheadP, null);
		int start = state.parameters.getInt(readFileStartFromP, null);
		int end = state.parameters.getInt(readFileEndP, null);

		String vmConfigPath = state.parameters.getString(vmConfigPathP, null);
		String pmConfigPath = state.parameters.getString(pmConfigPathP, null);
		String osProPath = state.parameters.getString(osProP, null);
		String initEnvPath = state.parameters.getString(envPath, null);

		readVMConfig(vmConfigPath);
		readPMConfig(pmConfigPath);
		readOSPro(osProPath);

		if (isTesting.equals("testing")) {
			int seed = Integer.valueOf(state.runtimeArguments[7].split("=")[1]);// test on one specific seed

//			for (int seed = 0; seed < 30; seed++) {// test on 30 runs
			String treeParentPath = state.runtimeArguments[10];
			String evolvedVMTree = treeParentPath + "/VM_Tree" + "/bestGPTree_";
			String evolvedPMTree = treeParentPath + "/PM_Tree" + "/bestGPTree_";
			Parameter testCasePathP = new Parameter("testCasePath");
			Parameter testosPathP = new Parameter("testosPath");

			String testCasePath = state.parameters.getString(testCasePathP, null);
			String testosPath = state.parameters.getString(testosPathP, null);
			// we only first warm up for the testing
			start = 0;
			end = 1;
			readEnvData(initEnvPath, start, end);
			readFromFiles(testCasePath, testosPath, start, end - 1);

			ReadExpression expReaderForVMRule = new ReadExpression(evolvedVMTree);
			ReadExpression expReaderForPMRule = new ReadExpression(evolvedPMTree);
			String expressionVM = null;
			String expressionPM = null;

			ArrayList<Double> resultList = new ArrayList<Double>();
			switch (method) {
			case 0:
				// CCGP
				expressionVM = expReaderForVMRule.readExpFrom(seed);
				expressionPM = expReaderForPMRule.readExpFrom(seed);

				mainAllocationProcessContainerTest = new MainAllocationProcessContainerTest(this, state,
						vmCpuOverheadRate, vmMemOverhead);

				resultList = mainAllocationProcessContainerTest.test(null, state, inputX, initVm, initContainer, initOs,
						initPm, initPmType, initVmType, pmTypeList, vmTypeList, expressionVM, expressionPM, seed,
						stack);
				break;

			case 1:
				// GPGP
				expressionVM = expReaderForVMRule.readExpFrom(seed);
				expressionPM = expReaderForPMRule.readExpFrom(seed);

				mainAllocationProcessContainerIndTest = new MainAllocationProcessContainer_IndTest(this, state,
						vmCpuOverheadRate, vmMemOverhead);

				resultList = mainAllocationProcessContainerIndTest.test(null, state, inputX, initVm, initContainer,
						initOs, initPm, initPmType, initVmType, pmTypeList, vmTypeList, expressionVM, expressionPM,
						seed, stack);
				break;

			case 2:
				// First Fit
				benchmark = new SubJustFit_FF(vmCpuOverheadRate, vmMemOverhead, inputX, initVm, initContainer, initOs,
						initPm, initPmType, initVmType, pmTypeList, vmTypeList);

				resultList = benchmark.allocate(0);
				break;

			}
			double aveFit = 0;
			for (int i = 0; i < resultList.size(); ++i) {
				aveFit += resultList.get(i);
			}

			aveFit /= resultList.size();

			try {
				outputTestingResults(seed + " " + aveFit, seed, treeParentPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Testing performance of seed " + seed + " on testing dataset is: " + aveFit);
//			}
			System.out.println("Mean CPU Utilization : " + Main.sum(Main.cpuUtilization) / Main.cpuUtilization.size());
			System.out.println("Mean wasted Mem Utilization : " + Main.sum(Main.wastedMen) / Main.wastedMen.size());
			System.out.println("Total new PMs : " + Main.newPMNo);
			System.out.println("Total new VMs : " + Main.newVMNo);

			System.exit(0);// exit testing mode

		} else {

			Parameter trainCasePathP = new Parameter("trainCasePath");
			Parameter osPathP = new Parameter("osPath");

			String trainCasePath = state.parameters.getString(trainCasePathP, null);
			String osPath = state.parameters.getString(osPathP, null);

			readEnvData(initEnvPath, start, end);
			readFromFiles(trainCasePath, osPath, start, end - 1);

		}

		switch (method) {
		case 0:
			// CCGP
			mainAllocationProcessContainer = new MainAllocationProcessContainer(this, state, vmCpuOverheadRate,
					vmMemOverhead);
			break;

		case 1:
			// GPGP
			mainAllocationProcessContainer_Ind = new MainAllocationProcessContainer_Ind(this, state, vmCpuOverheadRate,
					vmMemOverhead);
			break;

//		case 2:
//			// First Fit
//			ArrayList<Double> resultList = new ArrayList<Double>();
//
//			benchmark = new SubJustFit_FF(vmCpuOverheadRate, vmMemOverhead, inputX, initVm, initContainer, initOs,
//					initPm, initPmType, initVmType, pmTypeList, vmTypeList);
//			for (int i = 0; i < inputX.size(); i++) {
//				resultList.addAll(benchmark.allocate(i));
//			}
//			System.out.print(resultList);
//			System.exit(0);// exit

		}

		// another instances are used for serving specific evaluation in co-evolutionary
		// evaluator

		switch (method) {
		case 0:
			// CCGP
			mainAllocationProcessContainer2 = new MainAllocationProcessContainer2(this, state, vmCpuOverheadRate,
					vmMemOverhead);
			break;

		case 1:
			// GPGP
			mainAllocationProcessContainer_Ind2 = new MainAllocationProcessContainer_Ind2(this, state,
					vmCpuOverheadRate, vmMemOverhead);
			break;

		}

//		benchmark = new SubJustFit_FF(vmCpuOverheadRate, vmMemOverhead, inputX, initVm, initContainer, initOs, initPm,
//				initPmType, initVmType, pmTypeList, vmTypeList);

//		benchmarkResult = new ArrayList<>();
//        for(int i = 0; i < inputX.size(); i++){
//		benchmarkResult.add(benchmark.allocate(i));
//        }

	}

	private void readEnvData(String initEnvPath, int start, int end) {
		ReadConfigures readEnvConfig = new ReadConfigures();
		initPm = readEnvConfig.testCases(initEnvPath, "pm", start, end);
		initOs = readEnvConfig.testCases(initEnvPath, "os", start, end);
//        ArrayList<ArrayList> temp= readEnvConfig.testCases(initEnvPath, "container", start, end);
		initContainer = readEnvConfig.testCases(initEnvPath, "container", start, end);
		initVm = readEnvConfig.testCases(initEnvPath, "vm", start, end);
		initPmType = readEnvConfig.testCases(initEnvPath, "pmType", start, end);
		initVmType = readEnvConfig.testCases(initEnvPath, "vmType", start, end);

	}

	// Read two column from the testCase file
	private ArrayList<Double[]> readFromFile(String path, String osPath) {
		ArrayList<Double[]> data = new ArrayList<>();
		try {
			Reader reader = Files.newBufferedReader(Paths.get(path));
			Reader readerOS = Files.newBufferedReader(Paths.get(osPath));

			CSVReader csvReader = new CSVReader(reader);
			CSVReader csvReaderOS = new CSVReader(readerOS);
			String[] nextRecord;
			String[] nextRecordOS;
			while ((nextRecord = csvReader.readNext()) != null && (nextRecordOS = csvReaderOS.readNext()) != null) {
				// [0] is for cpu, [1] is for mem [2] for os [3] for hours
				Double[] container = new Double[4];
				container[0] = Double.parseDouble(nextRecord[0]);
				container[1] = Double.parseDouble(nextRecord[1]);
				container[2] = Double.parseDouble(nextRecordOS[0]);
				container[3] = Double.parseDouble(nextRecord[2]);
				data.add(container);
			}
			reader.close();
			readerOS.close();
			csvReader.close();
			csvReaderOS.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return data;
	}

	private void readOSPro(String osProPath) {
		try {
			Reader reader = Files.newBufferedReader(Paths.get(osProPath));
			CSVReader csvReader = new CSVReader(reader);
			String[] nextRecord;
			while ((nextRecord = csvReader.readNext()) != null) {
				Double pro;
				pro = Double.parseDouble(nextRecord[0]);
				OSPro.add(pro);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void readVMConfig(String vmConfigPath) {
		try {
			Reader reader = Files.newBufferedReader(Paths.get(vmConfigPath));
			CSVReader csvReader = new CSVReader(reader);
			String[] nextRecord;
			while ((nextRecord = csvReader.readNext()) != null) {
				Double[] vm = new Double[3];
				vm[0] = Double.parseDouble(nextRecord[0]); // cpu Mhz
				vm[1] = Double.parseDouble(nextRecord[1]); // mems
				vm[2] = Double.parseDouble(nextRecord[2]); // num of core
				vmTypeList.add(vm);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void readPMConfig(String pmConfigPath) {
		try {
			Reader reader = Files.newBufferedReader(Paths.get(pmConfigPath));
			CSVReader csvReader = new CSVReader(reader);
			String[] nextRecord;
			while ((nextRecord = csvReader.readNext()) != null) {
				Double[] pm = new Double[5];
				pm[0] = Double.parseDouble(nextRecord[0]);// cpu
				pm[1] = Double.parseDouble(nextRecord[1]);// mem
				pm[2] = Double.parseDouble(nextRecord[2]);// idle
				pm[3] = Double.parseDouble(nextRecord[3]);// max
				pm[4] = Double.parseDouble(nextRecord[4]);// core num
				pmTypeList.add(pm);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	// we read containers from file
	private void readFromFiles(String testCasePath, String osPath, int start, int end) {
//        end = 1;
//
		for (int i = start; i <= end; ++i) {
			String path = testCasePath + i + ".csv";
			String pathOS = osPath + i + ".csv";
			inputX.add(readFromFile(path, pathOS));
		}
	}

	@Override
	public void preprocessPopulation(EvolutionState state, Population pop, boolean[] prepareForFitnessAssessment,
			boolean countVictoriesOnly) {
		for (int i = 0; i < pop.subpops.size(); i++) {
			if (prepareForFitnessAssessment[i]) {
				for (int j = 0; j < pop.subpops.get(i).individuals.size(); j++) {
					KozaFitness fit = (KozaFitness) (pop.subpops.get(i).individuals.get(j).fitness);
					fit.trials = new ArrayList();
				}
			}
		}

	}

	@Override
	public int postprocessPopulation(EvolutionState state, Population pop, boolean[] assessFitness,
			boolean countVictoriesOnly) {
		for (int i = 0; i < pop.subpops.size(); i++) {
			if (assessFitness[i]) {
				for (int j = 1; j < pop.subpops.get(i).individuals.size(); j++) {
					KozaFitness fit = (KozaFitness) (pop.subpops.get(i).individuals.get(j).fitness);
					Double fitnessValue = ((Double) fit.trials.get(0)).doubleValue();
					fit.setStandardizedFitness(state, fitnessValue);
					pop.subpops.get(i).individuals.get(j).evaluated = true;
				}
			}
		}
		return 0;
	}

	@Override
	public void evaluate(EvolutionState state, Individual[] ind, boolean[] updateFitness, boolean countVictoriesOnly,
			int[] subpops, int threadnum) {
		if (ind.length == 0) {
			state.output.fatal("Number of individuals provided to ContainerAllocationProblem is 0");
		}
		if (ind.length == 1) {
			state.output.warnOnce(
					"Coevolution used" + " but number of individuals provided to RuleCoevolutionProblem is 1.");
		}

		//
		Random random = new Random(state.generation);
//		System.out.println("random testcase based on seed" + state.generation + ";");
		// Step 1: setup VM selection and creation rule
		GPIndividual containerAllocationRule = (GPIndividual) ind[0];

		// Setup VM allocation rule
		GPIndividual vmAllocationRule = (GPIndividual) ind[1];

		KozaFitness fit1 = (KozaFitness) ind[0].fitness;
		KozaFitness fit2 = (KozaFitness) ind[1].fitness;

		int method = Integer.valueOf(state.runtimeArguments[8]);
		int testCase = -1;
		// get 5 training instances based on current generation.

		if (updateFitness[0]) {
			ArrayList<Double> resultList = new ArrayList<Double>();

			switch (method) {
			case 0:
				// CCGP
				for (int ep_num = 0; ep_num < 5; ep_num++) {
					testCase = random.nextInt(200);
//					System.out.print("testcase" + testCase + ";");
					resultList.addAll(mainAllocationProcessContainer.evaluate((DoubleData) this.input, state, inputX,
							initVm, initContainer, initOs, initPm, initPmType, initVmType, pmTypeList, vmTypeList,
							containerAllocationRule, vmAllocationRule, threadnum, this.stack, testCase));
				}
				break;
			case 1:
				// GPGP
				for (int ep_num = 0; ep_num < 5; ep_num++) {
					testCase = random.nextInt(200);
//					System.out.print("testcase" + testCase + ";");
					resultList.addAll(mainAllocationProcessContainer_Ind.evaluate((DoubleData) this.input, state,
							inputX, initVm, initContainer, initOs, initPm, initPmType, initVmType, pmTypeList,
							vmTypeList, containerAllocationRule, vmAllocationRule, threadnum, this.stack, testCase));
				}
				break;
			}

			double aveFit = 0;
			for (int i = 0; i < resultList.size(); ++i) {
				aveFit += resultList.get(i);
			}

//			 Now we normalize the fitness value based on benchmark
//            aveFit /= benchmarkResult.get(state.generation);
			aveFit /= resultList.size();

			fit1.trials.add(aveFit);
			fit1.setStandardizedFitness(state, aveFit);
		}

		if (updateFitness[1]) {

			ArrayList<Double> resultList = new ArrayList<Double>();

			switch (method) {
			case 0:
				// CCGP
				for (int ep_num = 0; ep_num < 5; ep_num++) {
					testCase = random.nextInt(200);
//					System.out.print("testcase" + testCase + ";");
					resultList.addAll(mainAllocationProcessContainer2.evaluate((DoubleData) this.input, state, inputX,
							initVm, initContainer, initOs, initPm, initPmType, initVmType, pmTypeList, vmTypeList,
							containerAllocationRule, vmAllocationRule, threadnum, this.stack, testCase));
				}
				break;
			case 1:
				// GPGP
				for (int ep_num = 0; ep_num < 5; ep_num++) {
					testCase = random.nextInt(200);
//					System.out.print("testcase" + testCase + ";");
					resultList.addAll(mainAllocationProcessContainer_Ind2.evaluate((DoubleData) this.input, state,
							inputX, initVm, initContainer, initOs, initPm, initPmType, initVmType, pmTypeList,
							vmTypeList, containerAllocationRule, vmAllocationRule, threadnum, this.stack, testCase));
				}
				break;
			}

			double aveFit = 0;
			for (int i = 0; i < resultList.size(); ++i) {
				aveFit += resultList.get(i);
			}

//            aveFit /= benchmarkResult.get(state.generation);
			aveFit /= resultList.size();

			fit2.trials.add(aveFit);
			fit2.setStandardizedFitness(state, aveFit);
		}
	}

	@Override
	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
			final int threadnum) {
	}

	public void outputTestingResults(String str, int seed, String path) throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(path + "/" + seed + ".txt"));
		writer.write(str);

		writer.close();
	}

	// The min parameter (the origin) is inclusive, whereas the upper bound max is
	// exclusive.
	public int getRandomNumberUsingNextInt(int min, int max) {
		Random random = new Random();
		return random.nextInt(max - min) + min;
	}

}

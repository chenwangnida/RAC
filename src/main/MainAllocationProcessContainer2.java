package main;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.ADFStack;
import ec.gp.GPIndividual;

import java.util.ArrayList;
import java.util.HashMap;

public class MainAllocationProcessContainer2 {
	public double maxCPU = 41600;
	public double maxMem = 256000;

	private final double vmCpuOverheadRate;
	private final double vmMemOverhead;

	public double containerCpu;
	public double containerMem;
	public int containerOs;

	public double normalizedContainerCpu;
	public double normalizedContainerMem;
	public double normalizedVmCpuCapacity;
	public double normalizedVmMemCapacity;

	public double currentPmCpuRemain;
	public double currentPmMemRemain;

	public double normalizedVmCpuOverhead;
	public double normalizedVmMemOverhead;

	public double normalizedPmCpuRemain;
	public double normalizedPmMemRemain;

	public double normalizedPmActualCpuUsed;
	public double normalizedPmActualMemUsed;

	public double normalizedVmActualCpuUsed;
	public double normalizedVmActualMemUsed;

	public boolean newVmFlag = false;
	public boolean hourChanged = false;

	private ContainerAllocationProblem containerAllocationProblem;

	private double energyConsumption;
	public double currentEnergyUnitTime = 0.0;
	public double currentTimestamp = 0.0;
	public double previousTimestamp = 0.0;
	private boolean newPMFlag;
	private Double[] newPMHolder;

	// Constructor
	public MainAllocationProcessContainer2(ContainerAllocationProblem containerAllocationProblem, EvolutionState state,
			double vmCpuOverheadRate, double vmMemOverhead) {

		this.containerAllocationProblem = containerAllocationProblem;
		this.vmCpuOverheadRate = vmCpuOverheadRate;
		this.vmMemOverhead = vmMemOverhead;
		this.energyConsumption = 0;
		this.previousTimestamp = 0;
		this.currentEnergyUnitTime = 0;
		this.currentTimestamp = 0;
	}

	private boolean energyUpdate(double curerntTime, double previousTime) {
		if (currentEnergyUnitTime < 0) {
			System.err.println("currentEnergyUnitTime cannot be negative, something must be wrong!");
		}
		this.energyConsumption += (this.currentEnergyUnitTime) * Math.abs(curerntTime - previousTime) / 1000 / 3600;
		return true;
	}

	public ArrayList<Double> evaluate(DoubleData input, EvolutionState state, ArrayList<ArrayList<Double[]>> inputX,
			ArrayList<ArrayList> initVm, ArrayList<ArrayList> initContainer, ArrayList<ArrayList> initOs,
			ArrayList<ArrayList> initPm, ArrayList<ArrayList> initPmType, ArrayList<ArrayList> initVmType,
			ArrayList<Double[]> pmTypeList, ArrayList<Double[]> vmTypeList, GPIndividual vmSelectionCreationRule,
			GPIndividual vmAllocationRule, int threadnum, ADFStack stack, int testCase) {

		MyEvolutionState myEvolutionState = (MyEvolutionState) state;
		// testCaseNum equals the current generation
//		int testCase = state.generation;
		// initialize the resource lists
		ArrayList<Double> resultList = new ArrayList<>();

		ArrayList<Double[]> pmResourceList = new ArrayList<>();
		ArrayList<Double[]> pmActualUsageList = new ArrayList<>();
		ArrayList<Double[]> vmResourceList = new ArrayList<>();
		HashMap<Integer, Integer> VMPMMapping = new HashMap<>();
		HashMap<Integer, Integer> vmIndexTypeMapping = new HashMap<>();
		this.energyConsumption = 0.0;

		// using a universal initializer
		initializationDataCenterForAll initializing = new initializationDataCenterForAll(testCase, pmResourceList,
				pmActualUsageList, vmResourceList, vmTypeList, pmTypeList, initPm, initVm, initContainer, initOs,
				initPmType, initVmType, VMPMMapping, vmIndexTypeMapping);
		this.currentEnergyUnitTime = initializing.getUnitPower();
		vmResourceList = (ArrayList<Double[]>) initializing.getVmResourceList().clone();
		pmResourceList = (ArrayList<Double[]>) initializing.getPmResourceLsit().clone();
		pmActualUsageList = (ArrayList<Double[]>) initializing.getPmActualUsageList().clone();
		vmIndexTypeMapping = (HashMap<Integer, Integer>) initializing.getVmIndexTypeMapping().clone();
		VMPMMapping = (HashMap<Integer, Integer>) initializing.getVMPMMapping().clone();

//		System.out.println("Warm up power is " + this.currentEnergyUnitTime);
		ArrayList<Double[]> containers = inputX.get(testCase);
		this.currentTimestamp = containers.get(0)[3];

		// Start simulation
		for (Double[] container : containers) {
			containerCpu = container[0];
			containerMem = container[1];
			containerOs = container[2].intValue();

			// update myEvolutionState
			myEvolutionState.containerCpu = containerCpu;
			myEvolutionState.containerMem = containerMem;
			myEvolutionState.containerOs = containerOs;

			if (this.currentTimestamp - container[3] != 0) {
				energyUpdate(container[3], currentTimestamp);
				this.currentTimestamp = container[3];
			}

			Integer chosenVM;
			Integer currentVmNum = vmResourceList.size();

			// select or create a VM
			chosenVM = VMSelectionCreation(input, state, vmSelectionCreationRule, threadnum, stack, vmResourceList,
					pmResourceList, pmActualUsageList, vmTypeList, vmIndexTypeMapping, containerCpu, containerMem,
					containerOs);

			// check if the VM exists, if chosenVM < currentVmNum is true, it means
			// the chosenVM exists, we just need to update its resources

			if (chosenVM < currentVmNum) {
				// update the VM resources, allocating this container into this VM
				vmResourceList.set(chosenVM,
						new Double[] { vmResourceList.get(chosenVM)[0] - containerCpu,
								vmResourceList.get(chosenVM)[1] - containerMem, new Double(containerOs),
								vmResourceList.get(chosenVM)[3] });

				// Find the pmIndex in the mapping
				int pmIndex = VMPMMapping.get(chosenVM);
				if (containerCpu != 0) {
					double newRemain = pmActualUsageList.get(pmIndex)[0] - containerCpu;
					updateCurrentPow(pmIndex, pmActualUsageList, newRemain);
				}
				// update the PM actual resources
				pmActualUsageList.set(pmIndex,
						new Double[] { pmActualUsageList.get(pmIndex)[0] - containerCpu,
								pmActualUsageList.get(pmIndex)[1] - containerMem, pmActualUsageList.get(pmIndex)[2],
								pmActualUsageList.get(pmIndex)[3], pmActualUsageList.get(pmIndex)[4],
								pmActualUsageList.get(pmIndex)[5] });

			} else {// Else, we need to create this new VM

				// Retrieve the type of select VM
				int vmType = chosenVM - currentVmNum;

				// create this new VM
				vmResourceList.add(new Double[] {
						vmTypeList.get(vmType)[0] - containerCpu - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
						vmTypeList.get(vmType)[1] - containerMem - vmMemOverhead, new Double(containerOs),
						vmTypeList.get(vmType)[2] });

				// Whenever we create a new VM, map its index in the VMResourceList to its type
				// for future purpose
				vmIndexTypeMapping.put(vmResourceList.size() - 1, vmType);
				// After creating a VM, we will choose a PM to allocate
				Integer chosenPM = VMAllocation(input, myEvolutionState, vmAllocationRule, threadnum, stack,
						pmResourceList, pmActualUsageList, pmTypeList, vmTypeList.get(vmType)[0],
						vmTypeList.get(vmType)[1], containerCpu + vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
						containerMem + vmMemOverhead, vmTypeList.get(vmType)[2]);

				// If we cannot choose a running PM, then create a new PM
				if (chosenPM == null) {
					
					int chosedType = pmCreation(pmTypeList, vmTypeList.get(vmType)[0], vmTypeList.get(vmType)[1],
							vmTypeList.get(vmType)[2]);

					pmResourceList.add(new Double[] { pmTypeList.get(chosedType)[0] - vmTypeList.get(vmType)[0],
							pmTypeList.get(chosedType)[1] - vmTypeList.get(vmType)[1], pmTypeList.get(chosedType)[2],
							pmTypeList.get(chosedType)[3], pmTypeList.get(chosedType)[4],
							pmTypeList.get(chosedType)[0] });

					// Add the Actual usage to the PM
					pmActualUsageList.add(new Double[] {
							pmTypeList.get(chosedType)[0] - containerCpu
									- vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
							pmTypeList.get(chosedType)[1] - containerMem - vmMemOverhead, pmTypeList.get(chosedType)[2],
							pmTypeList.get(chosedType)[3], pmTypeList.get(chosedType)[4],
							pmTypeList.get(chosedType)[0] });

					double util = 1 - (pmActualUsageList.get(pmActualUsageList.size() - 1)[0]
							/ pmResourceList.get(pmActualUsageList.size() - 1)[5]);

					this.currentEnergyUnitTime += pmActualUsageList.get(pmActualUsageList.size() - 1)[2]; // idle

					this.currentEnergyUnitTime += (pmTypeList.get(chosedType)[3] - pmTypeList.get(chosedType)[2])
							* (2 * util - Math.pow(util, 1.4));

					// Map the VM to the PM
					VMPMMapping.put(vmResourceList.size() - 1, pmResourceList.size() - 1);
				
				} else {// If there is an existing PM, we allocate it to an existing PM
					
					currentPmCpuRemain = pmResourceList.get(chosenPM)[0] - vmTypeList.get(vmType)[0];
					currentPmMemRemain = pmResourceList.get(chosenPM)[1] - vmTypeList.get(vmType)[1];
					
					if (containerCpu != 0) {
						double newRemainingCPU = pmActualUsageList.get(chosenPM)[0] - containerCpu
								- vmTypeList.get(vmType)[0] * vmCpuOverheadRate;
						updateCurrentPow(chosenPM, pmActualUsageList, newRemainingCPU);
					}
					
					// update the PM resources: pm resources - vm size
					pmResourceList.set(chosenPM,
							new Double[] { currentPmCpuRemain, currentPmMemRemain, pmResourceList.get(chosenPM)[2],
									pmResourceList.get(chosenPM)[3], pmResourceList.get(chosenPM)[4],
									pmResourceList.get(chosenPM)[5],

							});

					// update the actual resources: actual usage - container required - vm overhead
					pmActualUsageList.set(chosenPM,
							new Double[] {
									pmActualUsageList.get(chosenPM)[0] - containerCpu
											- vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
									pmActualUsageList.get(chosenPM)[1] - containerMem - vmMemOverhead,
									pmActualUsageList.get(chosenPM)[2], pmActualUsageList.get(chosenPM)[3],
									pmActualUsageList.get(chosenPM)[4], pmActualUsageList.get(chosenPM)[5] });

					// Map the VM to the PM
					VMPMMapping.put(vmResourceList.size() - 1, chosenPM);

				} // End of allocating a VM to an existing PM

			} // End of creating a new VM

		} // End of all test cases

		resultList.add(this.energyConsumption);
		this.energyConsumption = 0.0;
		this.currentEnergyUnitTime = 0.0;
		return resultList;
	}

	private int pmCreation(ArrayList<Double[]> pmTypeList, double vmCpu, double vmMem, double vmCore) {
		int chosedType = -1;
		double bestcurrentUtil_CPU = 0;
		double requireCPU = vmCpu;
		double requireMem = vmMem;

		for (int i = 0; i < pmTypeList.size(); i++) {
			if (requireCPU <= pmTypeList.get(i)[0] && requireMem <= pmTypeList.get(i)[1]
					&& pmTypeList.get(i)[4] >= vmCore) {
				double currentUtil_CPU = (pmTypeList.get(i)[0] - requireCPU) / pmTypeList.get(i)[0];
				if (bestcurrentUtil_CPU < currentUtil_CPU) {
					chosedType = i;
					bestcurrentUtil_CPU = currentUtil_CPU;
				} else {// the current type is worse
					continue;
				}
			}

		}
		if (chosedType < 0 || chosedType > pmTypeList.size()) {
			System.err.println("Choose PM cannot be negative, or large than pmTypeList");
		}
		return chosedType;
	}

	private Integer VMAllocation(DoubleData input, final EvolutionState state, final Individual ind,
			final int threadnum, final ADFStack stack, ArrayList<Double[]> pmResourceList,
			ArrayList<Double[]> pmActualResourceList, ArrayList<Double[]> pmTypeList, double vmCpuCapcacity,
			double vmMemCapacity, double vmUsedCpu, double vmUsedMem, double vmCore) {

		Integer chosenPM = null;
		Double BestScore = Double.MAX_VALUE * (-1);
		int pmCount = 0;
		// Loop through the tempResourceList
		for (Double[] pm : pmResourceList) {
			// Get the remaining PM resources
			double pmCpuRemain = pm[0];
			double pmMemRemain = pm[1];
			double pmCore = pm[4];
			double pmActualCpuUsed = pmActualResourceList.get(pmCount)[0];
			double pmActualMemUsed = pmActualResourceList.get(pmCount)[1];

			// If the remaining resource is enough for the container
			// And the OS is compatible
			if (pmCpuRemain >= vmCpuCapcacity && pmMemRemain >= vmMemCapacity && pmCore>=vmCore) {

				Double pmScore = EvolveVmAllocationMethod(input, state, ind, threadnum, stack, pmCpuRemain, pmMemRemain,
						vmCpuCapcacity, vmMemCapacity, pmActualCpuUsed, pmActualMemUsed, vmUsedCpu, vmUsedMem, pmCore);

				// Core of BestFit, score the bigger the better
//				if (chosenPM == null || pmScore > BestScore) {
				if (pmScore > BestScore) {
					chosenPM = pmCount;
					BestScore = pmScore;
				}
			} // End if
				// If there is no suitable PM (no PM has enough resources), then we just return
				// null.
			pmCount++;
		}
		return chosenPM;
	}

	private Double EvolveVmAllocationMethod(final DoubleData input, final EvolutionState state, final Individual ind,
			final int threadnum, final ADFStack stack, double pmCpuRemain, double pmMemRemain, double vmCpuCapacity,
			double vmMemCapacity, double pmActualCpuUsed, double pmActualMemUsed, double vmActualCpuUsed,
			double vmActualMemUsed, double pmCore) {

		currentPmCpuRemain = pmCpuRemain;
		currentPmMemRemain = pmMemRemain;
		normalizedPmCpuRemain = currentPmCpuRemain / maxCPU;
		normalizedPmMemRemain = currentPmMemRemain / maxMem;
		normalizedVmCpuCapacity = vmCpuCapacity / maxCPU;
		normalizedVmMemCapacity = vmMemCapacity / maxMem;
		normalizedPmActualCpuUsed = pmActualCpuUsed / maxCPU;
		normalizedPmActualMemUsed = pmActualMemUsed / maxMem;
		normalizedVmActualCpuUsed = vmActualCpuUsed / maxCPU;
		normalizedVmActualMemUsed = vmActualMemUsed / maxMem;

		// update state in myEvolutionState
		MyEvolutionState myEvolutionState = (MyEvolutionState) state;
		myEvolutionState.currentPmCpuRemain = currentPmCpuRemain;
		myEvolutionState.currentPmMemRemain = currentPmMemRemain;
		myEvolutionState.normalizedPmCpuRemain = normalizedPmCpuRemain;
		myEvolutionState.normalizedPmMemRemain = normalizedPmMemRemain;
		myEvolutionState.normalizedVmCpuCapacity = normalizedVmCpuCapacity;
		myEvolutionState.normalizedVmMemCapacity = normalizedVmMemCapacity;
		myEvolutionState.normalizedPmActualCpuUsed = normalizedPmActualCpuUsed;
		myEvolutionState.normalizedPmActualMemUsed = normalizedPmActualMemUsed;
		myEvolutionState.normalizedVmActualCpuUsed = normalizedVmActualCpuUsed;
		myEvolutionState.normalizedVmActualMemUsed = normalizedVmActualMemUsed;
		myEvolutionState.coreNumber = pmCore;

		// Evaluate the GP rule
		((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual) ind,
				containerAllocationProblem);
		return input.x;

	}

	private Double EvolveSelectionCreationMethod(DoubleData input, final EvolutionState state, final Individual ind,
			final int threadnum, final ADFStack stack, double vmCpuRemain, double vmMemRemain, double vmCpuCapacity,
			double vmMemCapacity, ArrayList<Double[]> pmResourceList, ArrayList<Double[]> actualPmResourceList) {
		MyEvolutionState myEvolutionState = (MyEvolutionState) state;

		// The resource is normalized by the PM's capacity.
		normalizedContainerCpu = containerCpu / maxCPU;
		normalizedContainerMem = containerMem / maxMem;

		// update the data in myState

		myEvolutionState.normalizedVmCpuRemain = vmCpuRemain / maxCPU;
		myEvolutionState.normalizedVmMemRemain = vmMemRemain / maxMem;
		myEvolutionState.normalizedContainerCpu = normalizedContainerCpu;
		myEvolutionState.normalizedContainerMem = normalizedContainerMem;

		// we only consider the overhead of new VM
		if (newVmFlag) {
			normalizedVmCpuOverhead = vmCpuCapacity * vmCpuOverheadRate / maxCPU;
			normalizedVmMemOverhead = vmMemOverhead / maxMem;
		} else {
			normalizedVmCpuOverhead = 0;
			normalizedVmMemOverhead = 0;
		}

		// update the data in myState
		myEvolutionState.normalizedVmCpuOverhead = normalizedVmCpuOverhead;
		myEvolutionState.normalizedVmMemOverhead = normalizedVmMemOverhead;

		// Evaluate the GP rule
		((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual) ind,
				containerAllocationProblem);

		return input.x;
	}
	
	private Integer VMSelectionCreation(final DoubleData input, final EvolutionState state, final Individual ind,
			final int threadnum, final ADFStack stack, ArrayList<Double[]> vmResourceList,
			ArrayList<Double[]> pmResourceList, ArrayList<Double[]> actualPmResourceList,
			ArrayList<Double[]> vmTypeList, HashMap<Integer, Integer> vmIndexTypeMapping, Double containerCpu,
			Double containerMem, int containerOS) {
		Integer chosenVM = null;
		Double BestScore = Double.MAX_VALUE * (-1);
		int vmNum = vmResourceList.size();
		int vmCount = 0;
		ArrayList<Double[]> tempVMResourceList = new ArrayList<>();
		// make a copy of vmResourceList
		for (Double[] vmR : vmResourceList) {
			tempVMResourceList.add(vmR.clone());
		}
		for (Double[] vm : vmTypeList) {
			// add this new VM into the tempList
			tempVMResourceList.add(
					new Double[] { vm[0] - vm[0] * vmCpuOverheadRate, vm[1] - vmMemOverhead, new Double(containerOS) });
		}

		// Loop through the tempResourceList
		for (Double[] vm : tempVMResourceList) {

			// Check if the vm exists
			newVmFlag = (vmCount >= vmNum);

			// Get the remaining VM resources and OS
			double vmCpuRemain = vm[0];
			double vmMemRemain = vm[1];
			int vmOS = vm[2].intValue();
			int vmType;
			if (vmCount < vmNum) {
				vmType = vmIndexTypeMapping.get(vmCount);
			} else
				vmType = vmCount - vmNum;
			vmOS = containerOS;
			// If the remaining resource is enough for the container
			// And the OS is compatible
			if (vmCpuRemain >= containerCpu && vmMemRemain >= containerMem && vmOS == containerOS) {

				Double vmScore = EvolveSelectionCreationMethod(input, state, ind, threadnum, stack, vmCpuRemain,
						vmMemRemain, vmTypeList.get(vmType)[0], vmTypeList.get(vmType)[1],
						pmResourceList, actualPmResourceList);

				// Core of BestFit, score the bigger the better
				if (vmScore > BestScore) {
					chosenVM = vmCount;
					BestScore = vmScore;
				}

			} // End if

			// Increment the VM counter
			vmCount += 1;
		}

		newVmFlag = false;
		if (chosenVM == null) {
			System.err.println("No suitable VM for the container!Something must be wrong in filtering containers");
		}
		return chosenVM;
	}

	public boolean updateCurrentPow(Integer chosenPM, ArrayList<Double[]> actualUsageList, double newCpuRemain) {
		double privousUtil = (actualUsageList.get(chosenPM)[5] - actualUsageList.get(chosenPM)[0])
				/ actualUsageList.get(chosenPM)[5];
		double maxPow = actualUsageList.get(chosenPM)[3];
		double minPow = actualUsageList.get(chosenPM)[2];
		double newUtil = (actualUsageList.get(chosenPM)[5] - newCpuRemain) / actualUsageList.get(chosenPM)[5];
		double newPowerConsumption = (maxPow - minPow)
				* (2 * newUtil - Math.pow(newUtil, 1.4) - 2 * privousUtil + Math.pow(privousUtil, 1.4));
		if (privousUtil > newUtil) {
			System.out.println(" privous CPU Util SHOULD NOT greater than new CPU Util");
		}
		this.currentEnergyUnitTime += newPowerConsumption;
		return true;
	}

}

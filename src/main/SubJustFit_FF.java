package main;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * SubJustFit_FF represents that the algorithm contains three rules, 1. Best-Fit
 * with sub rule for allocating containers to VMs 2. JustFit for VM creation,
 * justFit finds the smallest VM for the container to create 3. First-Fit for VM
 * allocation
 */
public class SubJustFit_FF {
	public double maxCPU = 41600;
	public double maxMem = 256000;

	private double vmCpuOverheadRate;
	private double vmMemOverhead;

	private final ArrayList<ArrayList> initPmType;
	private final ArrayList<ArrayList> initVmType;

	private ArrayList<ArrayList> initVm;
	private ArrayList<ArrayList> initPm;
	private ArrayList<ArrayList> initOs;
	private ArrayList<ArrayList> initContainer;
	private ArrayList<Double[]> vmTypeList;
	private ArrayList<Double[]> pmTypeList;

	private ArrayList<Double> maxCPUs = new ArrayList<>();

	public double currentHour = 0.0;
	public double currentTimestamp;
	public double currentEnergyUnitTime;
	public double energyConsumption;
	public boolean firstContainer = false;
	private ArrayList<ArrayList<Double[]>> inputX;

	public SubJustFit_FF(double vmCpuOverheadRate, double vmMemOverhead, ArrayList<ArrayList<Double[]>> inputX,
			ArrayList<ArrayList> initVm, ArrayList<ArrayList> initContainer, ArrayList<ArrayList> initOs,
			ArrayList<ArrayList> initPm, ArrayList<ArrayList> initPmType, ArrayList<ArrayList> initVmType,
			ArrayList<Double[]> pmTypeList, ArrayList<Double[]> vmTypeList) {

		this.vmCpuOverheadRate = vmCpuOverheadRate;
		this.vmMemOverhead = vmMemOverhead;
		this.inputX = inputX;
		this.initVm = initVm;
		this.initContainer = initContainer;
		this.initOs = initOs;
		this.initPm = initPm;
		this.pmTypeList = pmTypeList;
		this.vmTypeList = vmTypeList;
		this.initPmType = initPmType;
		this.initVmType = initVmType;
//    =====================================
		this.energyConsumption = 0.0;
		this.currentEnergyUnitTime = 0.0;
		this.firstContainer = true;
	}

	public ArrayList<Double> allocate(int testCase) {
//		this.maxCPUs = new ArrayList<>();//Todo:  fix me

		ArrayList<Double> resultList = new ArrayList<>();

		ArrayList<Double[]> vmResourceList = new ArrayList<>();
		ArrayList<Double[]> pmActualUsageList = new ArrayList<>();
		this.energyConsumption = 0.0;

		// pmStatusList, the VM boundary of PM
		ArrayList<Double[]> pmResourceList = new ArrayList<>();
		HashMap<Integer, Integer> VMPMMapping = new HashMap<>();
		HashMap<Integer, Integer> vmIndexTypeMapping = new HashMap<>();

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
		
		ArrayList<Double[]> containers = inputX.get(testCase);
		this.currentTimestamp = containers.get(0)[3];

		for (Double[] container : containers) {
			double containerCpu = container[0];
			double containerMem = container[1];
			int containerOs = container[2].intValue();

			if (this.currentTimestamp - container[3] != 0) {
				energyUpdate(container[3], currentTimestamp);
				this.currentTimestamp = container[3];
			}

			if (!bestFit(container, pmActualUsageList, vmResourceList, VMPMMapping)) {// VM selection (sub rule using
																						// best fit)
				int vmType = justFit(container);// VM creation

				// create this new VM
				vmResourceList.add(new Double[] {
						vmTypeList.get(vmType)[0] - containerCpu - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
						vmTypeList.get(vmType)[1] - containerMem - vmMemOverhead, new Double(containerOs),
						vmTypeList.get(vmType)[2] });

				vmIndexTypeMapping.put(vmResourceList.size() - 1, vmType);
				Main.newVMNo ++;
				// PM selection using first Fit
				if (!firstFit(vmType, container, pmActualUsageList, pmResourceList, vmResourceList, VMPMMapping)) {

					// PM creation
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
					Main.newPMNo ++;
				}
			}
			Main.cpuUtilization.add(Main.avgCPUUtilPerContainer(pmActualUsageList));
			Main.wastedMen.add(Main.avgWastedMemPerContainer(pmActualUsageList));

		}
		System.out.println("The final power unit time of heuristic is : " + this.currentEnergyUnitTime);
		System.out.println("The power consumption is : " + this.energyConsumption);
		resultList.add(this.energyConsumption);
		return resultList;
	}

	private boolean firstFit(int vmType, Double[] container, ArrayList<Double[]> pmActualUsageList,
			ArrayList<Double[]> pmResourceList, ArrayList<Double[]> vmResourceList,
			HashMap<Integer, Integer> VMPMMapping) {
		boolean allocated = false;

		double containerCpu = container[0];
		double containerMem = container[1];

		double vmCpuCapcacity = vmTypeList.get(vmType)[0];
		double vmMemCapacity = vmTypeList.get(vmType)[1];
		double vmCore = vmTypeList.get(vmType)[2];

		int pmCount = 0;
		Integer chosenPM = null;
		for (Double[] pm : pmResourceList) {
			// Get the remaining PM resources
			double pmCpuRemain = pm[0];
			double pmMemRemain = pm[1];
			double pmCore = pm[4];

			if (pmCpuRemain >= vmCpuCapcacity && pmMemRemain >= vmMemCapacity && pmCore >= vmCore) {
				allocated = true;

				chosenPM = pmCount;
				double currentPmCpuRemain = pmResourceList.get(chosenPM)[0] - vmTypeList.get(vmType)[0];
				double currentPmMemRemain = pmResourceList.get(chosenPM)[1] - vmTypeList.get(vmType)[1];

				if (containerCpu != 0) {
					double newRemainingCPU = pmActualUsageList.get(chosenPM)[0] - containerCpu
							- vmTypeList.get(vmType)[0] * vmCpuOverheadRate;
					if (newRemainingCPU < 0) {
						System.err.println("newCPURemain" + newRemainingCPU);
					}
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

				VMPMMapping.put(vmResourceList.size() - 1, chosenPM);
				break;// Break when the first fit found
			}
			pmCount++;
		}
		return allocated;
	}

	// select the smallest (memory) VM type
	private int justFit(Double[] container) {
		int chosenVM = 0;
		int vmCounter = 0;
		double bestValue = Double.MAX_VALUE;
		for (Double[] vmType : vmTypeList) {
			if ((vmType[0] * (1 - vmCpuOverheadRate)) >= container[0]
					&& (vmType[1] >= (container[1] + vmMemOverhead))) {
				double leftMem = vmType[1] - container[1] - vmMemOverhead;
				if (leftMem < bestValue) {
					chosenVM = vmCounter;
					bestValue = leftMem;
				}
			}
			vmCounter++;
		}
		return chosenVM;
	}

	private boolean bestFit(Double[] container, ArrayList<Double[]> pmActualUsageList,
			ArrayList<Double[]> vmResourceList, HashMap<Integer, Integer> VMPMMapping) {

		boolean allocated = false;

		double containerCpu = container[0];
		double containerMem = container[1];
		int containerOs = container[2].intValue();

		int vmCounter = 0;
		int chosenVM = 0;
		double bestValue = Double.MAX_VALUE;
		for (Double[] vm : vmResourceList) {
			double vmCpuRemain = vm[0];
			double vmMemRemain = vm[1];
			int vmOs = vm[2].intValue();

			if (vmCpuRemain >= containerCpu && vmMemRemain >= containerMem && vmOs == containerOs) {
				allocated = true;
				double cpuLeft = vm[0] - container[0];
				double memLeft = vm[1] - container[1];
				double subValue = Math.abs(cpuLeft - memLeft);// This is SUB rule
				if (subValue < bestValue) {
					bestValue = subValue;
					chosenVM = vmCounter;
				}
			}
			vmCounter++;
		}

		if (allocated) {
			// update the VM actual resources
			vmResourceList.set(chosenVM,
					new Double[] { vmResourceList.get(chosenVM)[0] - containerCpu,
							vmResourceList.get(chosenVM)[1] - containerMem, new Double(containerOs),
							vmResourceList.get(chosenVM)[3] });

			int pmIndex = VMPMMapping.get(chosenVM);

			if (containerCpu != 0) {
				double newRemain = pmActualUsageList.get(pmIndex)[0] - containerCpu;
				if (newRemain < 0) {
					System.err.println("newCPURemain" + newRemain);
				}
				updateCurrentPow(pmIndex, pmActualUsageList, newRemain);
			}
			// update the PM actual resources
			pmActualUsageList.set(pmIndex,
					new Double[] { pmActualUsageList.get(pmIndex)[0] - containerCpu,
							pmActualUsageList.get(pmIndex)[1] - containerMem, pmActualUsageList.get(pmIndex)[2],
							pmActualUsageList.get(pmIndex)[3], pmActualUsageList.get(pmIndex)[4],
							pmActualUsageList.get(pmIndex)[5] });

		}
		return allocated;
	}

	// For PM creation create pm
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

	private boolean energyUpdate(double currentTime, double previousTime) {
		if (currentEnergyUnitTime < 0) {
			System.err.println("currentEnergyUnitTime cannot be negative, something must be wrong!");
		}
		this.energyConsumption += (this.currentEnergyUnitTime) * Math.abs(currentTime - previousTime) / 1000 / 3600;
//		System.out.println(this.energyConsumption+" "+ currentTime);
		return true;
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
			System.err.println(" privous CPU Util SHOULD NOT greater than new CPU Util");
		}
//		System.out.println("privousUtil"+ " " + privousUtil + "; newUtil" + " "+ newUtil+  "; newCPURemain "+ newCpuRemain);
		this.currentEnergyUnitTime += newPowerConsumption;
		return true;
	}
}

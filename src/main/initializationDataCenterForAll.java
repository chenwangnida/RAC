package main;

import java.util.ArrayList;
import java.util.HashMap;

public class initializationDataCenterForAll {
	private double currentEnergyUnitTime = 0.0;
	private double energyConsumption = 0.0;
	private double vmCpuOverheadRate = 0.1;

	private double vmMemOverhead = 200;
	private ArrayList<Double> maxCPUs = new ArrayList<>();
	private ArrayList<Double[]> pmResourceLsit = new ArrayList<>();
	private ArrayList<Double[]> pmActualUsageList = new ArrayList<>();
	private ArrayList<Double[]> vmResourceList = new ArrayList<>();
	private HashMap<Integer, Integer> VMPMMapping = new HashMap<>();
	private HashMap<Integer, Integer> vmIndexTypeMapping = new HashMap<>();

	public initializationDataCenterForAll(int testCase, ArrayList<Double[]> pmResourceList,
			ArrayList<Double[]> pmActualUsageList, ArrayList<Double[]> vmResourceList, ArrayList<Double[]> vmTypeList,
			ArrayList<Double[]> pmTypeList, ArrayList<ArrayList> initPm, ArrayList<ArrayList> initVm,
			ArrayList<ArrayList> initContainer, ArrayList<ArrayList> initOs, ArrayList<ArrayList> initPmType,
			ArrayList<ArrayList> initVmType, HashMap<Integer, Integer> VMPMMapping,
			HashMap<Integer, Integer> vmIndexTypeMapping) {
		this.energyConsumption = 0;
		this.currentEnergyUnitTime = 0.0;
		ArrayList<Double[]> initPmList = initPm.get(testCase);
		ArrayList<Double[]> initVmList = initVm.get(testCase);
		ArrayList<Double[]> containerList = initContainer.get(testCase);
		ArrayList<Double[]> osList = initOs.get(testCase);
		ArrayList<Double[]> initPmTypeList = initPmType.get(testCase);
		ArrayList<Double[]> initVmTypeList = initVmType.get(testCase);
//        System.out.println("Initialization for allocation process container on ");
		
		//Initial vmResourceList, a tuple of required cpu, mem, os, cores
		for (int i = 0; i < initVmTypeList.size(); ++i) {
			int vmType = (initVmTypeList.get(i)[0]).intValue();
			vmResourceList.add(new Double[] { (vmTypeList.get(vmType)[0] * (1 - vmCpuOverheadRate)),
					vmTypeList.get(vmType)[1] - vmMemOverhead, null, vmTypeList.get(vmType)[2] });
		}
		
		// for each pm (in each pm it contains the type of vm holding by this pm)
		for (int i = 0; i < initPmTypeList.size(); ++i) {
			int typePM = (initPmTypeList.get(i)[0]).intValue();
			Double[] vms = initPmList.get(i);// pm used to store vm types, but now vm instances id
			double pmCPU = pmTypeList.get(typePM)[0];
			double pmMem = pmTypeList.get(typePM)[1];
			double pmIdlePow = pmTypeList.get(typePM)[2];
			double pmMaxPow = pmTypeList.get(typePM)[3];
			double pmCoreNum = pmTypeList.get(typePM)[4];
			this.maxCPUs.add(new Double(pmCPU));
			// Add the PM to resource List at the beginning stage
			// The order of elements in the list is CPU, Memory and the type of this PM
			pmResourceList.add(new Double[] { pmCPU, pmMem, pmIdlePow, pmMaxPow, pmCoreNum, pmCPU });
			pmActualUsageList.add(new Double[] { pmCPU, pmMem, pmIdlePow, pmMaxPow, pmCoreNum, pmCPU });
			// for this vm, i.e., each vm in pm.csv row
			for (int vmCounter = 0; vmCounter < vms.length; ++vmCounter) {

				// Get the instance id of this VM
				int vm_instance_index = vms[vmCounter].intValue();

				// Get the type of this vm based on instance index
				int vmType = initVmTypeList.get(vm_instance_index)[0].intValue();


				// get the containers allocated on this VM			
				Double[] containers = initVmList.get(vm_instance_index);

				int pmIndex = i;

				// update the pm left resources(bounds), i.e, pmCPU, pmMem, pmIdlePow, pmMaxPow,
				// pmCoreNum, pmCPU
				pmResourceList.set(pmIndex, new Double[] { pmResourceList.get(pmIndex)[0] - vmTypeList.get(vmType)[0],
						pmResourceList.get(pmIndex)[1] - vmTypeList.get(vmType)[1], pmResourceList.get(pmIndex)[2],
						pmResourceList.get(pmIndex)[3], pmResourceList.get(pmIndex)[4], pmResourceList.get(pmIndex)[5]

				});

				// The second part of allocation,
				// We update the actual usage of PM's resources
				pmActualUsageList.set(pmIndex,
						new Double[] {
								pmActualUsageList.get(pmIndex)[0] - (vmTypeList.get(vmType)[0] * vmCpuOverheadRate),
								pmActualUsageList.get(pmIndex)[1] - vmMemOverhead, pmActualUsageList.get(pmIndex)[2],
								pmActualUsageList.get(pmIndex)[3], pmActualUsageList.get(pmIndex)[4],
								pmActualUsageList.get(pmIndex)[5]

						});

				// update the actual usage

				// update two maps
				VMPMMapping.put(vm_instance_index, pmIndex);
				vmIndexTypeMapping.put(vm_instance_index, vmType);

				// for each container
				for (int conIndex = 0; conIndex < containers.length; conIndex++) {
					int conContainer = containers[conIndex].intValue();
					// Get the container's cpu and memory
					Double[] cpuMem = containerList.get(conContainer);

					int vmIndex = vm_instance_index;
					Double[] vmCpuMem = vmResourceList.get(vmIndex);

					// update vmResource with every allocated container
					Double[] os = osList.get(vm_instance_index);
					vmResourceList.set(vmIndex, new Double[] { vmCpuMem[0] - cpuMem[0], vmCpuMem[1] - cpuMem[1],
							new Double(os[0]), vmCpuMem[3] // num of core required by this vm
					});

					// Add the Actual usage to the PM
					// Here, we must consider the overhead
					Double[] pmCpuMem = pmActualUsageList.get(pmIndex);

					// update the pm
					pmActualUsageList.set(pmIndex, new Double[] { pmCpuMem[0] - cpuMem[0], pmCpuMem[1] - cpuMem[1],
							pmCpuMem[2], pmCpuMem[3], pmCpuMem[4], pmCpuMem[5]

					});
//                    if (pmActualUsageList.get(pmIndex)[0] < 0 ){
////                        System.out.println("in initialization pm usage wrong");
//                    }
				} // Finish allocate containers to VMs
			} // End of each VM

		} // End of each PM

		// Update the unit time energy consumption
		double unitPowerConsumption = 0.0;
		for (int i = 0; i < pmResourceList.size(); i++) {
			Double idlePow = pmResourceList.get(i)[2];
			Double maxPow = pmResourceList.get(i)[3];
			unitPowerConsumption += idlePow;
			double utilization = 1 - (pmActualUsageList.get(i)[0] / pmResourceList.get(i)[5]);
			if (utilization < 0) {
				System.err.println("!!!utilization less than 0 !!!!!!!!!!!!error!!!!");
			}
			unitPowerConsumption += (maxPow - idlePow) * (2 * utilization - Math.pow(utilization, 1.4));
		}
		this.currentEnergyUnitTime = unitPowerConsumption;
		this.pmActualUsageList = pmActualUsageList;
		this.pmResourceLsit = pmResourceList;
		this.vmResourceList = vmResourceList;
		this.vmIndexTypeMapping = vmIndexTypeMapping;
		this.VMPMMapping = VMPMMapping;
	}

	public double getUnitPower() {
		return this.currentEnergyUnitTime;
	}

	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public double getVmCpuOverheadRate() {
		return vmCpuOverheadRate;
	}

	public double getVmMemOverhead() {
		return vmMemOverhead;
	}

	public ArrayList<Double[]> getPmResourceLsit() {
		return pmResourceLsit;
	}

	public ArrayList<Double[]> getPmActualUsageList() {
		return pmActualUsageList;
	}

	public ArrayList<Double[]> getVmResourceList() {
		return vmResourceList;
	}

	public HashMap<Integer, Integer> getVMPMMapping() {
		return VMPMMapping;
	}

	public HashMap<Integer, Integer> getVmIndexTypeMapping() {
		return vmIndexTypeMapping;
	}

	public ArrayList<Double> getMaxCPUs() {
		return maxCPUs;
	}

}

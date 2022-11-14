package functions;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import main.MyEvolutionState;
import main.ContainerAllocationProblem;
import main.DoubleData;


/***
 * This terminal is used to capture the feature of the number of cores for a PM
 *
 */


public class PmCoreNo extends GPNode {
    public String toString(){return "NoOfCore";}

    @Override
    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem) {
        DoubleData rd = (DoubleData) (input);
        MyEvolutionState myState = (MyEvolutionState) state;
        rd.x = myState.coreNumber;
    }

}

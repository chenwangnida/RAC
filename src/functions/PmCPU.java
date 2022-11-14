package functions;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import main.MyEvolutionState;
import main.DoubleData;


public class PmCPU extends GPNode {
    public String toString(){return "PmCPU";}

    @Override
    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem) {
        DoubleData rd = (DoubleData) (input);
        MyEvolutionState myState = (MyEvolutionState) state;
        rd.x = myState.normalizedPmCpuRemain - myState.normalizedVmCpuCapacity;
    }

}

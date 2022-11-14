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
import java.util.ArrayList;

public class cpuPredicted extends GPNode {
    public String toString() {return "cpuPredicted";}
    public int expectedChildren() { return 0; }

    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem){
        DoubleData rd = (DoubleData)(input);

        ContainerAllocationProblem p = (ContainerAllocationProblem) problem;
//        rd.x = p.normalizedContainerMem;
        rd.x = ((MyEvolutionState) state).cpuPredicted;
    }
}

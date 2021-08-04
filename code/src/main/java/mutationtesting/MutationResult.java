package mutationtesting;

import mutation.Mutation;
import qn.simulation.MeasureResult;
import qn.simulation.SimulationResults;

import java.util.List;

public class MutationResult {
    private final Mutation mutation;
    private final boolean killed;
    private final boolean timeout;
    private final SimulationResults originalResult;
    private SimulationResults mutantResult;
    private String killingTest;

    public MutationResult(Mutation mutation, SimulationResults originalResult, SimulationResults mutantResult, String killingTest) {
        this.mutation = mutation;
        this.originalResult = originalResult;
        this.mutantResult = mutantResult;
        this.killed = true;
        this.timeout = false;
        this.killingTest = killingTest;
    }

    public MutationResult(Mutation mutation, String killingTest, boolean timeout) {
        this.mutation = mutation;
        this.killed = true;
        this.timeout = timeout;
        this.killingTest = killingTest;
        this.originalResult = null;
    }

    public MutationResult(Mutation mutation) {
        this.mutation = mutation;
        this.killed = false;
        this.timeout = false;
        this.originalResult = null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[").append(mutation.getLocation()).append(",").append(mutation.getOperator()).append(",").append(mutation.getDescription()).append("]")
                .append("\t").append(this.killed);
        if (this.killed) {
            result.append("\t").append(this.killingTest);
        }
        if (this.originalResult != null) { //TODO clean that up so we don't have to set to -1 in constructors
            result.append("\t").append(this.originalResult)
                    .append("\t").append(this.mutantResult);
        }
        return result.toString();
    }

    public String printMetrics() {
        String mutantAndLocation = getKillingTest() + "\t" + mutation.getLocation() + "\t" + mutation.getOperator() + "\t";
        StringBuilder result = new StringBuilder();
        if (mutantResult != null) {
            List<MeasureResult> originalMeasures = originalResult.getMeasuresResults();
            List<MeasureResult> mutantMeasures = mutantResult.getMeasuresResults();
            assert originalMeasures.size() == mutantMeasures.size();
            for (int i = 0; i < originalMeasures.size(); i++) {
                result.append(mutantAndLocation).append(originalMeasures.get(i).getFixedPart()).append("\t").append(originalMeasures.get(i).getVariablePart()).append("\t").append(mutantMeasures.get(i).getVariablePart()).append("\t").append(isTimeout()).append("\n");
            }
        } else {
            result.append(mutantAndLocation).append(MeasureResult.getFixedPartNull()).append("\t").append(MeasureResult.getVariablePartNull()).append("\t").append(MeasureResult.getVariablePartNull()).append("\t").append(isTimeout()).append("\n");
        }
        return result.toString();
    }

    public Mutation getMutation() {
        return mutation;
    }

    public SimulationResults getOriginalResult() {
        return originalResult;
    }

    public SimulationResults getMutantResult() {
        return mutantResult;
    }

    public boolean isKilled() {
        return killed;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public String getKillingTest() {
        return killingTest;
    }
}

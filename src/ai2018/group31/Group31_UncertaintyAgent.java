package ai2018.group31;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

public class Group31_UncertaintyAgent extends AbstractNegotiationParty {


    AdditiveUtilitySpace ourUtilitySpace;

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        // Sample code that accepts offers that appear in the top 10% of offers
        // in the user model
        if (getLastReceivedAction() instanceof Offer) {
            Bid receivedBid = ((Offer) getLastReceivedAction()).getBid();
            List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();

            // If the rank of the received bid is known
            if (bidOrder.contains(receivedBid)) {
                double percentile = (bidOrder.size()
                        - bidOrder.indexOf(receivedBid))
                        / (double) bidOrder.size();
                if (percentile < 0.1)
                    return new Accept(getPartyId(), receivedBid);
            }
        }

        // Otherwise, return a random offer
        Bid rBid = generateRandomBid();
        System.out.println(" --> " + this.ourUtilitySpace.getUtility(rBid));
        return new Offer(getPartyId(), rBid);
    }

    /**
     * With this method, you can override the default estimate of the utility
     * space given uncertain preferences specified by the user model. This
     * example sets every value to zero.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace() {
        System.out.println("\n\n =====================================");
        Domain d = getDomain();
        List<Issue> issues = d.getIssues();
        int noIssues = issues.size();
        Map<Objective, Evaluator> evaluatorMap = new HashMap<Objective, Evaluator>();

        // Basic Init
        for (Issue i : issues) {
            IssueDiscrete issue = (IssueDiscrete) i;
            EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
            evaluator.setWeight(1.0 / noIssues);
            for (ValueDiscrete value : issue.getValues()) {
                evaluator.setEvaluationDouble(value, 0.0);
            }
            evaluatorMap.put(issue, evaluator);
        }
        this.ourUtilitySpace = new AdditiveUtilitySpace(d, evaluatorMap);
        updateUtilitySpace();
        printMasterPreference();
        return ourUtilitySpace;
    }

    public void updateUtilitySpace() {
        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
        double goldenValue = 0.04;
        int learnValueAddition = 1;

        // Our previous OMS Model
        for (int i = 1; i < bidOrder.size() - 1; i++) {
            Bid oppBid = bidOrder.get(i);
            Bid prevOppBid = bidOrder.get(i - 1);
            HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);

            // Increment the weights of issues that did not change their value since the last bid.
            double totalWeight = 0;
            for (Integer j : lastDiffSet.keySet()) {
                Objective issue = this.ourUtilitySpace.getDomain()
                        .getObjectivesRoot().getObjective(j);
                double weight = this.ourUtilitySpace.getWeight(j);
                double newWeight;

                if (lastDiffSet.get(j) == 0) {
                    newWeight = (weight + goldenValue);
                } else {
                    newWeight = weight;
                }
                totalWeight += newWeight;
                this.ourUtilitySpace.setWeight(issue, newWeight);
            }

            // re-weighing issues while making sure that the sum remains 1
            for (Integer j : lastDiffSet.keySet()) {
                Objective issue = this.ourUtilitySpace.getDomain()
                        .getObjectivesRoot().getObjective(j);
                double weight = this.ourUtilitySpace.getWeight(j);
                this.ourUtilitySpace.setWeight(issue, weight / totalWeight);
                System.out.println(issue.getName() + ": " + weight);
            }


            // Then for each issue value that has been offered last time, a constant
            // value is added to its corresponding ValueDiscrete.
            try {
                for (Entry<Objective, Evaluator> e : this.ourUtilitySpace
                        .getEvaluators()) {
                    EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                    IssueDiscrete issue = ((IssueDiscrete) e.getKey());
                    /*
                     * add constant learnValueAddition to the current preference of
                     * the value to make it more important
                     */
                    ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getValue(issue.getNumber());
                    Integer eval = value.getEvaluationNotNormalized(issuevalue);
                    value.setEvaluation(issuevalue, (learnValueAddition + eval));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void printMasterPreference() {
        double sum = 0;
        for (int j = 1; j < this.ourUtilitySpace.getEvaluators().size() + 1; j++) {
            System.out.println("Weight " + j + ": " + this.ourUtilitySpace.getWeight(j));
            sum += this.ourUtilitySpace.getWeight(j);
        }
        System.out.println("sum: " + sum);
        for (Entry<Objective, Evaluator> e : this.ourUtilitySpace
                .getEvaluators()) {
            EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
            IssueDiscrete issue = ((IssueDiscrete) e.getKey());
            System.out.println(issue.getDescription() + ":  " + value);
        }
    }

    @Override
    public double getUtility(Bid bid) {
        double result = 0;
        int count = 0;
        double gamma = 0.5;
        for (Entry<Objective, Evaluator> e : ourUtilitySpace.getEvaluators()) {
            Double weight = ourUtilitySpace.getWeight(count);
            try {
                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                IssueDiscrete issue = ((IssueDiscrete) e.getKey());
                ValueDiscrete issuevalue = (ValueDiscrete) bid.getValue(issue.getNumber());
                Double eval = Double.valueOf(value.getEvaluationNotNormalized(issuevalue));
                Double normalizer = Math.pow(value.getEvalMax() + 1, gamma);
                eval = Math.pow(1 + eval, gamma) / normalizer;
                result += weight * eval;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            count++;
        }
        System.out.println("Utility: " + result);
        System.out.println("=============================");
        return result;
    }

    private HashMap<Integer, Integer> determineDifference(Bid first, Bid second) {

        HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
        try {
            for (Issue i : this.ourUtilitySpace.getDomain().getIssues()) {
                Value value1 = first.getValue(i.getNumber());
                Value value2 = second.getValue(i.getNumber());
                diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return diff;
    }

    @Override
    public String getDescription() {
        return "Example agent that can deal with uncertain preferences";
    }

}
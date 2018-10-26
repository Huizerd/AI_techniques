package ai2018.group31;

// Opponent Model 
// tries to guess the opponent's preferences

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.*;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


public class Group31_OM extends OpponentModel {

    private double learnCoef;
    private int learnValueAddition;
    private int amountOfIssues;
    private double goldenValue;
    private int windowSize;
    private double gamma;

    public Group31_OM() {
    }

    public void init(NegotiationSession var1, Map<String, Double> var2) {
        this.negotiationSession = var1;
        if (var2 != null && var2.get("l") != null) {
            this.learnCoef = var2.get("l");
        } else {
            this.learnCoef = 0.2D;
        }

        this.gamma = 0.5;
        this.learnValueAddition = 1;
        this.opponentUtilitySpace = (AdditiveUtilitySpace) var1.getUtilitySpace().copy();
        this.amountOfIssues = this.opponentUtilitySpace.getDomain().getIssues().size();
        this.goldenValue = this.learnCoef / (double) this.amountOfIssues;
        this.initializeModel();

//        INITIALIZATION OF TIME WINDOW SIZE
        this.windowSize = 5;
    }

    @Override
    public void updateModel(Bid opponentBid, double time) {
        if (negotiationSession.getOpponentBidHistory().size() < 2) {
            return;
        }
        int numberOfUnchanged = 0;
        BidDetails oppBid = negotiationSession.getOpponentBidHistory()
                .getHistory()
                .get(negotiationSession.getOpponentBidHistory().size() - 1);
        BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
                .getHistory()
                .get(negotiationSession.getOpponentBidHistory().size() - 2);
        HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);

        // count the number of changes in value
        for (Integer i : lastDiffSet.keySet()) {
            if (lastDiffSet.get(i) == 0)
                numberOfUnchanged++;
        }

        // The total sum of weights before normalization.
        double totalSum = 1D + goldenValue * numberOfUnchanged;
        // The maximum possible weight
        double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

        // re-weighing issues while making sure that the sum remains 1
        for (Integer i : lastDiffSet.keySet()) {
            Objective issue = opponentUtilitySpace.getDomain()
                    .getObjectivesRoot().getObjective(i);
            double weight = opponentUtilitySpace.getWeight(i);
            double newWeight;

            if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
                newWeight = (weight + goldenValue) / totalSum;
            } else {
                newWeight = weight / totalSum;
            }
            opponentUtilitySpace.setWeight(issue, newWeight);
        }

        // Then for each issue value that has been offered last time, a constant
        // value is added to its corresponding ValueDiscrete.
        try {
            for (Entry<Objective, Evaluator> e : opponentUtilitySpace
                    .getEvaluators()) {
                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                IssueDiscrete issue = ((IssueDiscrete) e.getKey());
                /*
                 * add constant learnValueAddition to the current preference of
                 * the value to make it more important
                 */
                ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
                        .getValue(issue.getNumber());
                Integer eval = value.getEvaluationNotNormalized(issuevalue);
                Double normalizer = Math.pow(value.getEvalMax() + 1, this.gamma);
                value.setEvaluationDouble(issuevalue, Math.pow(learnValueAddition + eval, this.gamma)/normalizer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public double getBidEvaluation(Bid bid) {
        double result = 0;
        try {
            result = opponentUtilitySpace.getUtility(bid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getName() {
        return "2018 - Group 31 Model";
    }

    public Set<BOAparameter> getParameterSpec() {
        HashSet var1 = new HashSet();
        var1.add(new BOAparameter("l", 0.2D, "The learning coefficient determines how quickly the issue weights are learned"));
        return var1;
    }

    private void initializeModel() {
        double var1 = 1.0D / (double) this.amountOfIssues;
        Iterator var3 = this.opponentUtilitySpace.getEvaluators().iterator();

        while (var3.hasNext()) {
            Map.Entry var4 = (Map.Entry) var3.next();
            this.opponentUtilitySpace.unlock((Objective) var4.getKey());
            ((Evaluator) var4.getValue()).setWeight(var1);

            try {
                Iterator var5 = ((IssueDiscrete) var4.getKey()).getValues().iterator();

                while (var5.hasNext()) {
                    ValueDiscrete var6 = (ValueDiscrete) var5.next();
                    ((EvaluatorDiscrete) var4.getValue()).setEvaluation(var6, 1);
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }

    }

    /**
     * Determines the difference between bids. For each issue, it is determined
     * if the value changed. If this is the case, a 1 is stored in a hashmap for
     * that issue, else a 0.
     *
     * @param first
     *            bid of the opponent
     * @param second
     *            bid
     * @return
     */
    private HashMap<Integer, Integer> determineDifference(BidDetails first,
                                                          BidDetails second) {

        HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
        try {
            for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
                Value value1 = first.getBid().getValue(i.getNumber());
                Value value2 = second.getBid().getValue(i.getNumber());
                diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return diff;
    }
}

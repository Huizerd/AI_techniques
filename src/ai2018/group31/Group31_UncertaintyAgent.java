package ai2018.group31;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import agents.anac.y2013.MetaAgent.portfolio.thenegotiatorreloaded.BidDetails;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.NegotiationSession;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

public class Group31_UncertaintyAgent extends AbstractNegotiationParty {


    AdditiveUtilitySpace ourUtilitySpace;
//
//    public Group31_UncertaintyAgent(){
//        System.out.println("Reaching constructor");
//        ourInitUtilitySpace();
//    }

//    @Override
//    public void updateModel(Bid opponentBid, double time) {
//        if (negotiationSession.getOpponentBidHistory().size() < 2) {
//            return;
//        }
//        int numberOfUnchanged = 0;
//        BidDetails oppBid = negotiationSession.getOpponentBidHistory()
//                .getHistory()
//                .get(negotiationSession.getOpponentBidHistory().size() - 1);
//        BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
//                .getHistory()
//                .get(negotiationSession.getOpponentBidHistory().size() - 2);
//        HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);
//
//        // count the number of changes in value
//        for (Integer i : lastDiffSet.keySet()) {
//            if (lastDiffSet.get(i) == 0)
//                numberOfUnchanged++;
//        }
//
//        // The total sum of weights before normalization.
//        double totalSum = 1D + goldenValue * numberOfUnchanged;
//        // The maximum possible weight
//        double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;
//
//        // re-weighing issues while making sure that the sum remains 1
//        for (Integer i : lastDiffSet.keySet()) {
//            Objective issue = opponentUtilitySpace.getDomain()
//                    .getObjectivesRoot().getObjective(i);
//            double weight = opponentUtilitySpace.getWeight(i);
//            double newWeight;
//
//            if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
//                newWeight = (weight + goldenValue) / totalSum;
//            } else {
//                newWeight = weight / totalSum;
//            }
//            opponentUtilitySpace.setWeight(issue, newWeight);
//        }
//
//        // Then for each issue value that has been offered last time, a constant
//        // value is added to its corresponding ValueDiscrete.
//        try {
//            for (Entry<Objective, Evaluator> e : opponentUtilitySpace
//                    .getEvaluators()) {
//                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
//                IssueDiscrete issue = ((IssueDiscrete) e.getKey());
//                /*
//                 * add constant learnValueAddition to the current preference of
//                 * the value to make it more important
//                 */
//                ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
//                        .getValue(issue.getNumber());
//                Integer eval = value.getEvaluationNotNormalized(issuevalue);
//                Double normalizer = Math.pow(value.getEvalMax() + 1, this.gamma);
//                value.setEvaluationDouble(issuevalue, Math.pow(learnValueAddition + eval, this.gamma)/normalizer);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }

//    public void initUtilitySpace() {
//        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
//        for (Bid b : bidOrder) {
//             updateModel(b);
//        }
//
//    }
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {

//        // Part1 - Acceptance Strategy
//        if (getLastReceivedAction() instanceof Offer) {
//            if (AcceptanceStrategy(receivedBid) == 1)
//                return new Accept(getPartyId(), receivedBid);
//            }
//        }
//
//        // Part2 - Bidding Strategy
//        counterBid = getBiddingStrategy()
//        return new Offer(getPartyId(), counterbid);

        // Sample code that accepts offers that appear in the top 10% of offers
        // in the user model
        if (getLastReceivedAction() instanceof Offer) {
            Bid receivedBid = ((Offer) getLastReceivedAction()).getBid();
            List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
            System.out.println(" --> " + this.ourUtilitySpace.getUtility(receivedBid));

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
        return new Offer(getPartyId(), generateRandomBid());
    }

    private void log(String s) {
        System.out.println(s);
    }

//    /**
//     * With this method, you can override the default estimate of the utility
//     * space given uncertain preferences specified by the user model. This
//     * example sets every value to zero.
//     */

//    @Override
//    public AbstractUtilitySpace estimateUtilitySpace() {
//        return new AdditiveUtilitySpaceFactory(getDomain()).getUtilitySpace();
//    }

    public void updateUtilitySpace() {
        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
        double goldenValue = 0.04;
        System.out.println();
        int amountOfIssues = this.ourUtilitySpace.getDomain().getIssues().size();
        double gamma = 0.5;
        int learnValueAddition  = 1;

        // Our previous OMS Model

        int randomBidIdx = 3;
        Bid randomBid = bidOrder.get(randomBidIdx);

        for (int i = 1; i < bidOrder.size()-1; i++) {
            Bid oppBid = bidOrder.get(i);
            Bid prevOppBid = bidOrder.get(i - 1);
            System.out.println(oppBid);
            HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);

            int numberOfUnchanged = 0;
            // count the number of changes in value
            for (Integer j : lastDiffSet.keySet()) {
                if (lastDiffSet.get(j) == 0)
                    numberOfUnchanged++;
            }

            // The total sum of weights before normalization.
            double totalSum = 1D + goldenValue * numberOfUnchanged;
            // The maximum possible weight
            double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

            // re-weighing issues while making sure that the sum remains 1
            for (Integer j : lastDiffSet.keySet()) {
                Objective issue = this.ourUtilitySpace.getDomain()
                        .getObjectivesRoot().getObjective(j);
                double weight = this.ourUtilitySpace.getWeight(j);
                double newWeight;

                if (lastDiffSet.get(j) == 0 && weight < maximumWeight) {
                    newWeight = (weight + goldenValue) / totalSum;
                } else {
                    newWeight = weight / totalSum;
                }
                this.ourUtilitySpace.setWeight(issue, newWeight);
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
                    Double normalizer = Math.pow(value.getEvalMax() + 1, gamma);
                    value.setEvaluationDouble(issuevalue, Math.pow(learnValueAddition + eval, gamma) / normalizer);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println(" -> Bid of my randomBid: " + this.ourUtilitySpace.getUtility(randomBid));
        }

        for (Entry<Objective, Evaluator> e : this.ourUtilitySpace
                .getEvaluators()) {
            EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
            IssueDiscrete issue = ((IssueDiscrete) e.getKey());
            System.out.println(issue.getDescription() + ":  " + value);
        }
    }
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
        return ourUtilitySpace;
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
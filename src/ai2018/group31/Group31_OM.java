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

    public Group31_OM() {
    }

    public void init(NegotiationSession var1, Map<String, Double> var2) {
        this.negotiationSession = var1;
        if (var2 != null && var2.get("l") != null) {
            this.learnCoef = var2.get("l");
        } else {
            this.learnCoef = 0.2D;
        }

        this.learnValueAddition = 1;
        this.opponentUtilitySpace = (AdditiveUtilitySpace) var1.getUtilitySpace().copy();
        this.amountOfIssues = this.opponentUtilitySpace.getDomain().getIssues().size();
        this.goldenValue = this.learnCoef / (double) this.amountOfIssues;
        this.initializeModel();

//        INITIALIZATION OF TIME WINDOW SIZE
        this.windowSize = 5;
    }

    public void updateModel(Bid var1, double var2) {
        int h = this.negotiationSession.getOpponentBidHistory().size();
        if (h < 2) return;

        int commonOccurences = 0;
        int wz = h < this.windowSize ? h : this.windowSize;

        BidDetails currBid = this.negotiationSession.getOpponentBidHistory().getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - 1);
        Map<Integer, Integer> differenceLookup = new HashMap();

        for (int i = 2; i <= wz; i++) {
            BidDetails prevBid = this.negotiationSession.getOpponentBidHistory().getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - i);
            differenceLookup = determineDifference(prevBid, currBid, new HashMap<>(differenceLookup));
        }

        Iterator iter = differenceLookup.keySet().iterator();

        while (iter.hasNext()) {
            Integer issueNo = (Integer) iter.next();
            if (differenceLookup.get(issueNo) == 0) {
                ++commonOccurences;
            }
        }

        double var20 = 1.0D + this.goldenValue * (double) commonOccurences;
        double var10 = 1.0D - (double) this.amountOfIssues * this.goldenValue / var20;

        Iterator iter2;
        Objective objective;
        double var17;
        for (iter2 = differenceLookup.keySet().iterator(); iter2.hasNext(); this.opponentUtilitySpace.setWeight(objective, var17)) {
            Integer currentDiff = (Integer) iter2.next();
            objective = this.opponentUtilitySpace.getDomain().getObjectivesRoot().getObjective(currentDiff);
            double var15 = this.opponentUtilitySpace.getWeight(currentDiff);
            if (differenceLookup.get(currentDiff) == 0 && var15 < var10) {
                var17 = (var15 + this.goldenValue) / var20;
            } else {
                var17 = var15 / var20;
            }
        }

        try {
            iter2 = this.opponentUtilitySpace.getEvaluators().iterator();

            while (iter2.hasNext()) {
                Map.Entry var21 = (Map.Entry) iter2.next();
                EvaluatorDiscrete var22 = (EvaluatorDiscrete) var21.getValue();
                IssueDiscrete var23 = (IssueDiscrete) var21.getKey();
                ValueDiscrete var16 = (ValueDiscrete) currBid.getBid().getValue(var23.getNumber());
                Integer var24 = var22.getEvaluationNotNormalized(var16);
                var22.setEvaluation(var16, this.learnValueAddition + var24);
            }
        } catch (Exception var19) {
            var19.printStackTrace();
        }

    }

    public double getBidEvaluation(Bid var1) {
        double var2 = 0.0D;

        try {
            var2 = this.opponentUtilitySpace.getUtility(var1);
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        return var2;
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

    private HashMap<Integer, Integer> determineDifference(BidDetails prevBid, BidDetails currBid, Map<Integer, Integer> diffLookup) {
        try {
            Iterator iter = this.opponentUtilitySpace.getDomain().getIssues().iterator();

            while (iter.hasNext()) {
                Issue issue = (Issue) iter.next();
                Value prevBidVal = prevBid.getBid().getValue(issue.getNumber());
                Value currBidVal = currBid.getBid().getValue(issue.getNumber());
                diffLookup.put(issue.getNumber(), (diffLookup.get(issue.getNumber()) == 0 ||
                        prevBidVal.equals(currBidVal)) ? 0 : 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (HashMap<Integer, Integer>) diffLookup;
    }
}

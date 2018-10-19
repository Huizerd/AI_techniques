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
            this.learnCoef = (Double)var2.get("l");
        } else {
            this.learnCoef = 0.2D;
        }

        this.learnValueAddition = 1;
        this.opponentUtilitySpace = (AdditiveUtilitySpace)var1.getUtilitySpace().copy();
        this.amountOfIssues = this.opponentUtilitySpace.getDomain().getIssues().size();
        this.goldenValue = this.learnCoef / (double)this.amountOfIssues;
        this.initializeModel();
//        INITIALIZATION OF TIME WINDOW SIZE
        this.windowSize = 5;
    }

    public void updateModel(Bid var1, double var2) {
        if (this.negotiationSession.getOpponentBidHistory().size() >= 2) {
            int var4 = 0;

            LinkedList<BidDetails> prevBid = new LinkedList<>();

            for(int i=1; i<=this.windowSize; i++){
                prevBid.add((BidDetails)this.negotiationSession.getOpponentBidHistory()
                        .getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - i));
            }

            HashMap var7 = this.determineDifference(prevBid, currBid);
            Iterator var8 = var7.keySet().iterator();

            while(var8.hasNext()) {
                Integer var9 = (Integer)var8.next();
                if ((Integer)var7.get(var9) == 0) {
                    ++var4;
                }
            }

            double var20 = 1.0D + this.goldenValue * (double)var4;
            double var10 = 1.0D - (double)this.amountOfIssues * this.goldenValue / var20;

            Iterator var12;
            Objective var14;
            double var17;
            for(var12 = var7.keySet().iterator(); var12.hasNext(); this.opponentUtilitySpace.setWeight(var14, var17)) {
                Integer var13 = (Integer)var12.next();
                var14 = this.opponentUtilitySpace.getDomain().getObjectivesRoot().getObjective(var13);
                double var15 = this.opponentUtilitySpace.getWeight(var13);
                if ((Integer)var7.get(var13) == 0 && var15 < var10) {
                    var17 = (var15 + this.goldenValue) / var20;
                } else {
                    var17 = var15 / var20;
                }
            }

            try {
                var12 = this.opponentUtilitySpace.getEvaluators().iterator();

                while(var12.hasNext()) {
                    Map.Entry var21 = (Map.Entry)var12.next();
                    EvaluatorDiscrete var22 = (EvaluatorDiscrete)var21.getValue();
                    IssueDiscrete var23 = (IssueDiscrete)var21.getKey();
                    ValueDiscrete var16 = (ValueDiscrete)currBid.getBid().getValue(var23.getNumber());
                    Integer var24 = var22.getEvaluationNotNormalized(var16);
                    var22.setEvaluation(var16, this.learnValueAddition + var24);
                }
            } catch (Exception var19) {
                var19.printStackTrace();
            }

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
        return "HardHeaded Frequency Model";
    }

    public Set<BOAparameter> getParameterSpec() {
        HashSet var1 = new HashSet();
        var1.add(new BOAparameter("l", 0.2D, "The learning coefficient determines how quickly the issue weights are learned"));
        return var1;
    }

    private void initializeModel() {
        double var1 = 1.0D / (double)this.amountOfIssues;
        Iterator var3 = this.opponentUtilitySpace.getEvaluators().iterator();

        while(var3.hasNext()) {
            Map.Entry var4 = (Map.Entry)var3.next();
            this.opponentUtilitySpace.unlock((Objective)var4.getKey());
            ((Evaluator)var4.getValue()).setWeight(var1);

            try {
                Iterator var5 = ((IssueDiscrete)var4.getKey()).getValues().iterator();

                while(var5.hasNext()) {
                    ValueDiscrete var6 = (ValueDiscrete)var5.next();
                    ((EvaluatorDiscrete)var4.getValue()).setEvaluation(var6, 1);
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }

    }

    private HashMap<Integer, Integer> determineDifference(BidDetails var1, BidDetails var2) {
        HashMap var3 = new HashMap();

        try {
            Iterator var4 = this.opponentUtilitySpace.getDomain().getIssues().iterator();

            while(var4.hasNext()) {
                Issue var5 = (Issue)var4.next();
                Value var6 = var1.getBid().getValue(var5.getNumber());
                Value var7 = var2.getBid().getValue(var5.getNumber());
                var3.put(var5.getNumber(), var6.equals(var7) ? 0 : 1);
            }
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return var3;
    }
}

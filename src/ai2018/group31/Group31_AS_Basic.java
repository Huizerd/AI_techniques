package ai2018.group31;

import genius.core.Bid;
import genius.core.boaframework.*;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.AcceptanceStrategy;
import java.util.*;


public class Group31_AS_Basic extends AcceptanceStrategy {

    private double a;
    private double b;

    public Group31_AS_Basic(){

    }

    public Group31_AS_Basic(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;

        if (parameters.get("a") != null || parameters.get("b") != null) {
            a = parameters.get("a");
            b = parameters.get("b");
        } else {
            a = 1;
            b = 0;
        }
    }

    @Override
    public String printParameters() {
        String str = "[a: " + a + " b: " + b + "]";
        return str;
    }

    @Override
    public Actions determineAcceptability() {
        Bid opppLastBid       = this.negotiationSession.getOpponentBidHistory().getLastBidDetails().getBid();
        double now            = this.negotiationSession.getTime();
        double discountedBid = this.negotiationSession.getUtilitySpace().getUtilityWithDiscount(opppLastBid, now);
        double nextMyBidUtil  = offeringStrategy.getNextBid().getMyUndiscountedUtil();

        if (discountedBid >= nextMyBidUtil){
            return Actions.Accept;
        }else{
            return Actions.Reject;
        }
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {

        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("a", 1.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("b", 0.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));

        return set;
    }

    @Override
    public String getName() {
        return "2018 - Group31 AS Basic";
    }
}
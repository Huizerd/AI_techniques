package ai2018.group31;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Group31_AS extends AcceptanceStrategy {
    private int window = 11;


    public Group31_AS() {
    }

    public Group31_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        // Alpha and beta are unused.
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
    }

    @Override
    public Actions determineAcceptability() {
        double now = this.negotiationSession.getTime();
        int current_k = negotiationSession.getOpponentBidHistory().getHistory().size();
        if (current_k > this.window) {
            List<BidDetails> history = negotiationSession.getOpponentBidHistory().getHistory();
            Bid oppLastBid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails().getBid();
            double discountedOppBid = this.negotiationSession.getUtilitySpace().getUtilityWithDiscount(oppLastBid, now);

            int lookback = Math.max(history.size() - this.window, 0);
            double sum = 0;
            for (int i = lookback; i < history.size(); i++) {
                sum += history.get(i).getMyUndiscountedUtil();
            }

            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            double opponentWindowedAverage = sum / (double) window;

            if (discountedOppBid >= opponentWindowedAverage && discountedOppBid >= nextMyBidUtil) {
                return Actions.Accept;
            }
        }
        return Actions.Reject;
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
        return "Group31_AS";
    }
}

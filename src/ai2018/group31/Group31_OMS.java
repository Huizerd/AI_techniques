
// Opponent Model Strategy
// specifies how the opponent model is used to select a bid for the opponent

package ai2018.group31;


import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;

import java.util.*;

/**
 * This class uses an opponent model to determine the next bid for the opponent,
 * while taking the opponent's preferences into account.
 *
 * @author definately not Mark Hendrikx
 */
public class Group31_OMS extends OMStrategy {

    /**
     * when to stop updating the opponentmodel. Note that this value is not
     * exactly one as a match sometimes lasts slightly longer.
     */
    double updateThreshold = 1.1;

    /**
     * Initializes the opponent model strategy. If a value for the parameter t
     * is given, then it is set to this value. Otherwise, the default value is
     * used.
     *
     * @param negotiationSession
     *            state of the negotiation.
     * @param model
     *            opponent model used in conjunction with this opponent modeling
     *            strategy.
     * @param parameters
     *            set of parameters for this opponent model strategy.
     */
    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
        super.init(negotiationSession, model, new HashMap<String, Double>());
        if (parameters.get("t") != null) {
            updateThreshold = parameters.get("t").doubleValue();
        } else {
            System.out.println("OMStrategy assumed t = 1.1");
        }
    }

    /**
     * Returns the best bid for the opponent given a set of similarly preferred
     * bids.
     *
     * @param allBids
     *            of the bids considered for offering.
     * @return bid to be offered to opponent.
     */
    @Override
    public BidDetails getBid(List<BidDetails> allBids) {

        // 1. If there is only a single bid, return this bid
        if (allBids.size() == 1) {
            return allBids.get(0);
        }
        double bestUtil = -1;
        BidDetails bestBid = allBids.get(0);

        // 2. Check that not all bids are assigned at utility of 0
        // to ensure that the opponent model works. If the opponent model
        // does not work, offer a random bid.
        boolean allWereZero = true;
        // 3. Determine the best bid
        for (BidDetails bid : allBids) {
            double Uown = this.negotiationSession.getUtilitySpace().getUtility(bid.getBid());
            double Uopp = model.getBidEvaluation(bid.getBid());
            double evaluation = Math.sqrt(Uown*Uown + Uopp*Uopp);
            if (evaluation > 0.000001) {
                allWereZero = false;
            }
            if (evaluation > bestUtil) {
                bestBid = bid;
                bestUtil = evaluation;
            }
        }
        // 4. The opponent model did not work, therefore, offer a random bid.
        if (allWereZero) {
            Random r = new Random();
            return allBids.get(r.nextInt(allBids.size()));
        }
        return bestBid;

    }


    /**
     * The opponent model may be updated, unless the time is higher than a given
     * constant.
     *
     * @return true if model may be updated.
     */
    @Override
    public boolean canUpdateOM() {
        return negotiationSession.getTime() < updateThreshold;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
        return set;
    }

    @Override
    public String getName() {
        return "Group31_OMS";
    }
}
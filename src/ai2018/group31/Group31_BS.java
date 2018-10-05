package ai2018.group31;

// Bidding Strategy
// decides which set of bids could be proposed next 

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.List;

public class Group31_BS extends OfferingStrategy {

    public Group31_BS(){

    }

    public void init(NegotiationSession var1, OpponentModel var2, OMStrategy var3, Map<String, Double> var4) throws Exception {

    }

    public BidDetails determineNextBid(){
        BidDetails bidMax = this.negotiationSession.getMaxBidinDomain();
        BidDetails bidMin = this.negotiationSession.getMinBidinDomain();

        double maxUtil = this.negotiationSession.getUtilitySpace().getUtility(bidMax.getBid());
        double minUtil = this.negotiationSession.getUtilitySpace().getUtility(bidMin.getBid());
        System.out.println("maxUtil : " + Double.toString(maxUtil));
        System.out.println("minUtil : " + Double.toString(minUtil));


//        BidDetails var1 = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
//        List var3       = this.negotiationSession.getUtilitySpace().getDomain().getIssues();
//        Iterator var7   = var3.iterator();
//        while(var7.hasNext()) {
//            Issue var8 = (Issue)var7.next();
//            int var9 = var8.getNumber();
//            Value var10 = var1.getValue(var9);
//            System.out.println(var8.getType());

        // RANDOM BID
        Bid bid = this.negotiationSession.getUtilitySpace().getDomain().getRandomBid((Random)null);
        try {
            this.nextBid = new BidDetails(bid, this.negotiationSession.getUtilitySpace().getUtility(bid), this.negotiationSession.getTime());
        } catch (Exception except) {
            except.printStackTrace();
        }

        return this.nextBid;
    }

    public BidDetails determineOpeningBid() {
        return this.determineNextBid();
    }

    public String getName() {
        return "2018 - Group31 - Bidding";
    }


}
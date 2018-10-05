package ai2018.group31;

// Bidding Strategy
// decides which set of bids could be proposed next 

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.utility.UtilitySpace;

import java.util.*;

public class Group31_BS extends OfferingStrategy {

    List<Double> timeBuckets;
    List<Double> utilityThresholds;
    UtilitySpace utilSpace;
    Map<Integer, ArrayList<Bid>> bidsBuckets;
    Random random;


    public Group31_BS() {

    }

    @Override
    public void init(NegotiationSession var1, OpponentModel var2, OMStrategy var3, Map<String, Double> var4) throws Exception {
        this.timeBuckets = new ArrayList<>(Arrays.asList(0.0, 0.5, 0.8, 0.95, 1.0));
        this.utilityThresholds = new ArrayList<>(Arrays.asList(1.0, 0.9, 0.8, 0.7, 0.55));
        this.random = new Random();
        this.negotiationSession = var1;
        this.opponentModel = var2;
        this.omStrategy = var3;
        this.utilSpace = this.negotiationSession.getUtilitySpace();
        this.bidsBuckets = new HashMap<>();
        this.initBidSpace();
        for (Bid bid : this.bidsBuckets.get(0)) {
            System.out.println(bid + ": " + this.utilSpace.getUtility(bid));
        }
    }

    public void initBidSpace() {
        for (int i = 0; i < this.utilityThresholds.size() - 1; i++) {
            this.bidsBuckets.put(i, new ArrayList<>());
        }
        BidIterator bidIterator = new BidIterator(this.utilSpace.getDomain());
        if (!bidIterator.hasNext()) {
            throw new IllegalStateException("The domain does not contain any bids!");
        } else {
            while (bidIterator.hasNext()) {
                Bid bid = bidIterator.next();
                double bidUtil = this.utilSpace.getUtility(bid);

                for (int i = 0; i < this.utilityThresholds.size() - 1; i++) {
                    if (bidUtil <= this.utilityThresholds.get(i) && bidUtil > this.utilityThresholds.get(i + 1)) {
                        this.bidsBuckets.get(i).add(bid);
                    }
                }
            }
        }
    }

    @Override
    public BidDetails determineNextBid() {
        double time = this.negotiationSession.getTime();
        for (int i = 0; i < this.timeBuckets.size() - 1; i++) {
            if (time >= this.timeBuckets.get(i) && time < this.timeBuckets.get(i + 1)) {
                List<Bid> possibleBids = this.bidsBuckets.get(i);
                int index = random.nextInt(possibleBids.size());
                return new BidDetails(possibleBids.get(index), this.utilSpace.getUtility(possibleBids.get(index)), time);
            }
        }
//        return this.negotiationSession.getMinBidinDomain();
        return null;

    }

    public BidDetails determineOpeningBid() {
        return this.determineNextBid();
    }

    public String getName() {
        return "2018 - Group31 - Bidding";
    }

}
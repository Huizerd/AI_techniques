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
    List<Double> utilMeans;
    UtilitySpace utilSpace;
    Map<Double, ArrayList<Bid>> bidBuckets;
    Random random;
    List<Double> sortedUtilities;


    public Group31_BS() {

    }

    @Override
    public void init(NegotiationSession var1, OpponentModel var2, OMStrategy var3, Map<String, Double> var4) throws Exception {
        this.timeBuckets = new ArrayList<>(Arrays.asList(0.0, 0.5, 0.95, 1.0));
        this.utilMeans = new ArrayList<>(Arrays.asList(1.0, 0.9, 0.8, 0.6));
        this.random = new Random();
        this.negotiationSession = var1;
        this.opponentModel = var2;
        this.omStrategy = var3;
        this.utilSpace = this.negotiationSession.getUtilitySpace();
        this.bidBuckets = new HashMap<>();
        this.initBidSpace();
        sortedUtilities = new ArrayList<>(this.bidBuckets.keySet());
        Collections.sort(this.sortedUtilities);
        for (double d : sortedUtilities) {
            System.out.println(d);
        }

    }

    public void initBidSpace() {
        BidIterator bidIterator = new BidIterator(this.utilSpace.getDomain());
        if (!bidIterator.hasNext()) {
            throw new IllegalStateException("The domain does not contain any bids!");
        } else {
            while (bidIterator.hasNext()) {
                Bid bid = bidIterator.next();
                double bidUtil = this.utilSpace.getUtility(bid);
                if (!this.bidBuckets.containsKey(bidUtil)) {
                    this.bidBuckets.put(bidUtil, new ArrayList<>());
                }
                this.bidBuckets.get(bidUtil).add(bid);
            }
        }
    }

    @Override
    public BidDetails determineNextBid() {
        double time = this.negotiationSession.getTime();
        for (int i = 0; i < this.timeBuckets.size() - 1; i++) {
            if (time >= this.timeBuckets.get(i) && time < this.timeBuckets.get(i + 1)) {
                double gauss = getGaussWithBounds(this.utilMeans.get(i), this.utilMeans.get(i+1), 1.0);
                // Get closest utility value.
                List<Double> sortedUtilcopy = new ArrayList<>(this.sortedUtilities);
                sortedUtilcopy.replaceAll(x -> Math.abs(x - gauss));
                int minIndex = sortedUtilcopy.indexOf(Collections.min(sortedUtilcopy));
                List<Bid> gaussBidList = this.bidBuckets.get(this.sortedUtilities.get(minIndex));
                Bid bid = gaussBidList.get(random.nextInt(gaussBidList.size()));
                return new BidDetails(bid, this.utilSpace.getUtility(bid), time);
            }
        }
        System.out.println("Return NUll");
        return null;
    }

    private double getGaussWithBounds(double mean, double lowerBound, double upperBound) {
        // Generate Gaussian value with utility mean and std of 0.1.
        while (true) {
            double gauss = random.nextGaussian() * 0.1 + mean;
            if (lowerBound <= gauss && gauss <= upperBound) {
                return gauss;
            }
        }
    }


    public BidDetails determineOpeningBid() {
        return this.determineNextBid();
    }

    public String getName() {
        return "2018 - Group31 - Bidding";
    }

}
package ai2018.group31;


import java.util.*;

import java.util.Map.Entry;

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.boaframework.NegotiationSession;
import genius.core.utility.*;

public class Group31_UncertaintyAgent extends AbstractNegotiationParty {


    AdditiveUtilitySpace ourUtilitySpace;
    NegotiationSession negotiationSession;
    int window = 11;

//    this.timeBuckets        = new ArrayList<>(Arrays.asList(0.0, 0.5, 0.95, 1.0));
//    this.utilMeans          = new ArrayList<>(Arrays.asList(1.0, 0.9, 0.8, 0.6));
//    this.random             = new Random();

    List<Double> timeBuckets;
    List<Double> utilMeans;
    UtilitySpace utilSpace;
    Map<Double, ArrayList<Bid>> bidBuckets;
    Random random;
    List<Double> sortedUtilities;
    OpponentModel opponentModel;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        timeline = info.getTimeline();
        estimateUtilitySpace();
        this.negotiationSession = new NegotiationSession(null, ourUtilitySpace, timeline, null, info.getUserModel());
//        this.opponentModel = new Group31_OM();
//        this.opponentModel.init(this.negotiationSession, null);
        System.out.println("AMUNA SIKIM 4");
        this.timeBuckets        = new ArrayList<>(Arrays.asList(0.0, 0.5, 0.95, 1.0));
        this.utilMeans          = new ArrayList<>(Arrays.asList(1.0, 0.9, 0.8, 0.6));
        this.random             = new Random();
        this.utilSpace          = this.negotiationSession.getUtilitySpace();
        this.bidBuckets         = new HashMap<>();
        this.initBidSpace();
        sortedUtilities         = new ArrayList<>(this.bidBuckets.keySet());
        Collections.sort(this.sortedUtilities);
        }

//    @Override
//    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
//        System.out.println("Choosing default action");
//        // Sample code that accepts offers that appear in the top 10% of offers
//        // in the user model
//        if (getLastReceivedAction() instanceof Offer) {
//            Bid receivedBid = ((Offer) getLastReceivedAction()).getBid();
//            List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
//
//            // If the rank of the received bid is known
//            if (bidOrder.contains(receivedBid)) {
//                double percentile = (bidOrder.size()
//                        - bidOrder.indexOf(receivedBid))
//                        / (double) bidOrder.size();
//                if (percentile < 0.1)
//                    return new Accept(getPartyId(), receivedBid);
//            }
//        }
//
//        // Otherwise, return a random offer
//        Bid rBid = generateRandomBid();
//        return new Offer(getPartyId(), rBid);
//    }

////////////////////////////////////
/////////////// AS /////////////////
////////////////////////////////////
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        double now = this.negotiationSession.getTime();
        int current_k = negotiationSession.getOpponentBidHistory().getHistory().size();
        BidDetails nextMyBid = determineNextBid();
//        Bid nextMyBid = generateRandomBid();
//        System.out.println("nextMyBid = " + nextMyBid.toString());
//        System.out.println("current_k = " + current_k);
        if (current_k > this.window) {
            List<BidDetails> history = negotiationSession.getOpponentBidHistory().getHistory();
            Bid oppLastBid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails().getBid();
            double discountedOppBid = this.negotiationSession.getUtilitySpace().getUtilityWithDiscount(oppLastBid, now);

            int lookback = Math.max(history.size() - this.window, 0);
            double sum = 0;
            for (int i = lookback; i < history.size(); i++) {
                sum += history.get(i).getMyUndiscountedUtil();
            }


            double nextMyBidUtil = nextMyBid.getMyUndiscountedUtil();
//            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil(); old

            double opponentWindowedAverage = sum / (double) window;

            if (discountedOppBid >= opponentWindowedAverage && discountedOppBid >= nextMyBidUtil) {
                return new Accept(getPartyId(), oppLastBid);
            }
        }
//        return Actions.Reject;
        return new Offer(getPartyId(), nextMyBid.getBid());
    }

////////////////////////////////////
/////////////// BS /////////////////
////////////////////////////////////

    public BidDetails determineNextBid() {
        double time = this.negotiationSession.getTime();
        // Step 1.1 -- Loop over the buckets
        for (int i = 0; i < this.timeBuckets.size() - 1; i++) {
            if (time >= this.timeBuckets.get(i) && time < this.timeBuckets.get(i + 1)) {
                double gauss = getGaussWithBounds(this.utilMeans.get(i), this.utilMeans.get(i+1), 1.0);

                // Step1.2 - Get a list of bids to offer.
                List<Double> sortedUtilcopy = new ArrayList<>(this.sortedUtilities);
                sortedUtilcopy.replaceAll(x -> Math.abs(x - gauss));
                int minIndex = sortedUtilcopy.indexOf(Collections.min(sortedUtilcopy));
                List<Bid> gaussBidList = this.bidBuckets.get(this.sortedUtilities.get(minIndex));

                // Step 1.3 - Pick best bid from that offer using Opponent modeling
                //Bid bid = gaussBidList.get(random.nextInt(gaussBidList.size()));
                // return new BidDetails(bid, this.utilSpace.getUtility(bid), time);
                List<BidDetails> returnBidDetails = new ArrayList<BidDetails>();
                for (Bid bid:gaussBidList){
                    returnBidDetails.add(new BidDetails(bid, this.utilSpace.getUtility(bid), time));
                }
                return getOMSBid(returnBidDetails);
            }
        }
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

////////////////////////////////////
/////////////// OMS ////////////////
////////////////////////////////////

    public BidDetails getOMSBid(List<BidDetails> allBids) {

        // 1. If there is only a single bid, return this bid
//        if (allBids.size() == 1) {
//            return allBids.get(0);
//        }
//        double bestUtil = -1;
//        BidDetails bestBid = allBids.get(0);

        // 2. Check that not all bids are assigned at utility of 0
        // to ensure that the opponent model works. If the opponent model
        // does not work, offer a random bid.
//        boolean allWereZero = true;
//        // 3. Determine the best bid from a given list of details
//        for (BidDetails bid : allBids) {
//            double uOwn       = this.negotiationSession.getUtilitySpace().getUtility(bid.getBid());
//            double uOpp       = opponentModel.getBidEvaluation(bid.getBid());
//            double evaluation = Math.sqrt(uOwn*uOwn + uOpp*uOpp);
//            if (evaluation > 0.000001) {
//                allWereZero = false;
//            }
//            if (evaluation > bestUtil) {
//                bestBid = bid;
//                bestUtil = evaluation;
//            }
//        }
        // 4. The opponent model did not work, therefore, offer a random bid.
//        if (allWereZero) {
            Random r = new Random();
//            return allBids.get(r.nextInt(allBids.size()));
            return allBids.get(0);
//        }
//        return bestBid;
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


    /**
     * With this method, you can override the default estimate of the utility
     * space given uncertain preferences specified by the user model. This
     * example sets every value to zero.
     */
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
        printMasterPreference();
        return ourUtilitySpace;
    }

    public void updateUtilitySpace() {
        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
        double goldenValue = 0.04;
        int learnValueAddition = 1;

        // Our previous OMS Model
        for (int i = 1; i < bidOrder.size() - 1; i++) {
            Bid oppBid = bidOrder.get(i);
            Bid prevOppBid = bidOrder.get(i - 1);
            HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);

            // Increment the weights of issues that did not change their value since the last bid.
            double totalWeight = 0;
            for (Integer j : lastDiffSet.keySet()) {
                Objective issue = this.ourUtilitySpace.getDomain()
                        .getObjectivesRoot().getObjective(j);
                double weight = this.ourUtilitySpace.getWeight(j);
                double newWeight;

                if (lastDiffSet.get(j) == 0) {
                    newWeight = (weight + goldenValue);
                } else {
                    newWeight = weight;
                }
                totalWeight += newWeight;
                this.ourUtilitySpace.setWeight(issue, newWeight);
            }

            // re-weighing issues while making sure that the sum remains 1
            for (Integer j : lastDiffSet.keySet()) {
                Objective issue = this.ourUtilitySpace.getDomain()
                        .getObjectivesRoot().getObjective(j);
                double weight = this.ourUtilitySpace.getWeight(j);
                this.ourUtilitySpace.setWeight(issue, weight / totalWeight);
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
                    value.setEvaluation(issuevalue, (learnValueAddition + eval));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void printMasterPreference() {
        double sum = 0;
        for (int j = 1; j < this.ourUtilitySpace.getEvaluators().size() + 1; j++) {
            System.out.println("Weight " + j + ": " + this.ourUtilitySpace.getWeight(j));
            sum += this.ourUtilitySpace.getWeight(j);
        }
        System.out.println("sum: " + sum);
        for (Entry<Objective, Evaluator> e : this.ourUtilitySpace
                .getEvaluators()) {
            EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
            IssueDiscrete issue = ((IssueDiscrete) e.getKey());
            System.out.println(issue.getDescription() + ":  " + value);
        }
    }

    @Override
    public double getUtility(Bid bid) {
        double result = 0;
        int count = 0;
        double gamma = 0.5;
        for (Entry<Objective, Evaluator> e : ourUtilitySpace.getEvaluators()) {
            Double weight = ourUtilitySpace.getWeight(count);
            try {
                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                IssueDiscrete issue = ((IssueDiscrete) e.getKey());
                ValueDiscrete issuevalue = (ValueDiscrete) bid.getValue(issue.getNumber());
                Double eval = Double.valueOf(value.getEvaluationNotNormalized(issuevalue));
                Double normalizer = Math.pow(value.getEvalMax() + 1, gamma);
                eval = Math.pow(1 + eval, gamma) / normalizer;
                result += weight * eval;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            count++;
        }
        System.out.println("Utility: " + result);
        System.out.println("=============================");
        return result;
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
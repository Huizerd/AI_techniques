package ai2018.group31;


import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import genius.core.Bid;
import genius.core.BidHistory;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.boaframework.NegotiationSession;
import genius.core.utility.*;

public class Group31_UncertaintyAgent extends AbstractNegotiationParty {


    AdditiveUtilitySpace ourUtilitySpace;
    NegotiationSession negotiationSession;

    Map<Bid, Double> smoothedUtils;
    List<Double> timeBuckets;
    List<Double> utilMeans;
    Map<Double, ArrayList<Bid>> bidBuckets;
    Random random;
    List<Double> sortedUtilities;
    BidHistory oppBidHistory;

    double gamma  = 1.0;
    int window    = 11;

    boolean verboseMasterModel = false;
    boolean verboseBids        = false;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        timeline = info.getTimeline();
        this.negotiationSession = new NegotiationSession(null, ourUtilitySpace, timeline, null, info.getUserModel());
        this.timeBuckets        = new ArrayList<>(Arrays.asList(0.0, 0.5, 0.95, 1.0));
        this.utilMeans          = new ArrayList<>(Arrays.asList(1.0, 0.9, 0.8, 0.6));

        this.random             = new Random();
        this.bidBuckets         = new HashMap<>();

        this.oppBidHistory = new BidHistory();
        this.initBidSpace();
        sortedUtilities = new ArrayList<>(this.bidBuckets.keySet());
        Collections.sort(this.sortedUtilities);

    }

    /*
        [ENTRY POINT] ACCEPTANCE AND BIDDING STRATEGY
    */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        double now = this.negotiationSession.getTime();
        if (getLastReceivedAction() instanceof Offer) {
            Bid oppLastBid = ((Offer) getLastReceivedAction()).getBid();
            this.oppBidHistory.add(new BidDetails(oppLastBid, this.getOurUtility(oppLastBid),now) );
        }
        int current_k = this.oppBidHistory.size();
        BidDetails nextMyBid = determineNextBid();

        if (current_k > this.window && getLastReceivedAction() instanceof Offer) {
            List<BidDetails> history = this.oppBidHistory.getHistory();
            Bid oppLastBid           = ((Offer) getLastReceivedAction()).getBid();
            double discountedOppBid  = this.getOurUtility(oppLastBid);

            int lookback = Math.max(history.size() - this.window, 0);
            double sum = 0;
            for (int i = lookback; i < history.size(); i++) {
                sum += this.getOurUtility(history.get(i).getBid());
            }

            double nextMyBidUtil = this.getOurUtility(nextMyBid.getBid());

            double opponentWindowedAverage = sum / (double) window;
            double time_left = negotiationSession.getTimeline().getTotalTime() - negotiationSession.getTimeline().getCurrentTime();
            if ((discountedOppBid >= opponentWindowedAverage && discountedOppBid >= nextMyBidUtil) || time_left <= 2 ) {
                return new Accept(getPartyId(), oppLastBid);
            }
        }
        return new Offer(getPartyId(), nextMyBid.getBid());
    }

    /*
        BOA FUNCTIONS
    */

    // [Bidding Strategy] Main Entry
    public BidDetails determineNextBid() {
        double time = this.negotiationSession.getTime();
        int closestBidWindow = 1;
        // Step 1.1 -- Loop over the buckets
        for (int i = 0; i < this.timeBuckets.size() - 1; i++) {
            if (time >= this.timeBuckets.get(i) && time < this.timeBuckets.get(i + 1)) {
                double mean = this.utilMeans.get(i);
                double lowerB = this.utilMeans.get(i + 1);
                double gauss = getGaussWithBounds(mean, lowerB, 1);

                // Step1.2 - Get a list of bids to offer.
                List<Double> sortedUtilcopy = new ArrayList<>(this.sortedUtilities);
                sortedUtilcopy.replaceAll(x -> Math.abs(x - gauss));  //distance to gauss
                List<Double> descendedDistance = new ArrayList<>(sortedUtilcopy);
                Collections.sort(descendedDistance);
                List<Bid> gaussBidList = new ArrayList<>();
                for (int j = 0; j < closestBidWindow; j++) {
                    int newIndex = sortedUtilcopy.indexOf(descendedDistance.get(j));
                    for( Bid b : this.bidBuckets.get(this.sortedUtilities.get(newIndex))) {
                        gaussBidList.add(b);
                    }
                }

                // Step 1.3 - Pick best bid from that offer using Opponent modeling
                List<BidDetails> returnBidDetails = new ArrayList<BidDetails>();
                for (Bid bid : gaussBidList) {
                    returnBidDetails.add(new BidDetails(bid, getUtility(bid), time));
                }
                BidDetails returnBidDetail = getOMSBid(returnBidDetails);

                if (verboseBids){
                    System.out.println("[" + this.utilMeans.get(i) + "/" + this.utilMeans.get(i + 1) + "][" + gauss + "][Offered Bid]: " + this.getUtility(returnBidDetail.getBid()));
                    System.out.println(gaussBidList.size());
                }

                return returnBidDetail;
            }
        }
        return null;
    }

    // [Bidding Strategy] Helper Function
    private double getGaussWithBounds(double mean, double lowerBound, double upperBound) {
        // Generate Gaussian value with utility mean and std of 0.1.
        while (true) {
            double gauss = random.nextGaussian() * 0.1 + mean;
            if (lowerBound <= gauss && gauss <= upperBound) {
                return gauss;
            }
        }
    }

    // [Bidding Strategy] Helper Function
    public BidDetails getOMSBid(List<BidDetails> allBids) {
        Random r = new Random();
        return allBids.get(r.nextInt(allBids.size()));
    }


    // [INIT FUNCTIONS]
    public void initBidSpace() {
        for (Bid bid : userModel.getBidRanking().getBidOrder()) {
            double bidUtil = getUtility(bid);
            if (!this.bidBuckets.containsKey(bidUtil)) {
                this.bidBuckets.put(bidUtil, new ArrayList<>());
            }
            this.bidBuckets.get(bidUtil).add(bid);
        }
    }

    // [INIT FUNCTIONS] Master Modelling
    @Override
    public AbstractUtilitySpace estimateUtilitySpace() {
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
        this.smoothedUtils   = new HashMap<>();

        updateUtilitySpace();
        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
        smoothUtils(bidOrder);

        if (verboseMasterModel){
           for (Bid bid : bidOrder) {
                System.out.println(" Smoothed Bid: " + round(getUtility(bid), 3));
            }
            printMasterPreference();
        }

        return ourUtilitySpace;
    }

    // [MASTER MODELLING] Step 1
    public void updateUtilitySpace() {
        List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
        List<Bid> bidOrder_reverse = new ArrayList<>(bidOrder);
        Collections.reverse(bidOrder_reverse);

        List<Bid> bidOrderExtended = new ArrayList<>();
        for (int i = 0; i < bidOrder_reverse.size(); i++) {
            Bid bidO = bidOrder_reverse.get(i);
            if (i < bidOrder_reverse.size() / 3) {
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
            } else if (i >= bidOrder_reverse.size() / 3 && i <= 2 * bidOrder_reverse.size() / 3) {
                bidOrderExtended.add(bidO);
                bidOrderExtended.add(bidO);
            } else {
                bidOrderExtended.add(bidO);
            }
        }

        double goldenValue = 0.04;
        int learnValueAddition = 1;

        // Our previous OMS Model
        for (int i = 1; i < bidOrderExtended.size() - 1; i++) {
            Bid oppBid = bidOrderExtended.get(i);
            Bid prevOppBid = bidOrderExtended.get(i - 1);
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
                    ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getValue(issue.getNumber());
                    Integer eval = value.getEvaluationNotNormalized(issuevalue);
                    value.setEvaluation(issuevalue, (learnValueAddition + eval));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // [MASTER MODELLING] Step 2
    public void smoothUtils(List<Bid> bidOrder) {
        int window = (int) (0.05 * bidOrder.size());
        for (int i = 0; i < window; i++) {
            smoothedUtils.put(bidOrder.get(i), this.getOurUtility(bidOrder.get(i)));
        }
        for (int i = window; i < bidOrder.size(); i++) {
            double sum = 0;
            for (int j = 0; j < window; j++) {
                sum += this.getOurUtility(bidOrder.get(i - j));
            }
            smoothedUtils.put(bidOrder.get(i), sum / (double) window);
        }
        double max = Collections.max(smoothedUtils.values());
        double min = Collections.min(smoothedUtils.values());
        for (Entry<Bid, Double> entry :
                smoothedUtils.entrySet()) {
            double newUtil = (entry.getValue()-min)/(max-min);
//            double newUtil = entry.getValue()/max;
            smoothedUtils.put(entry.getKey(), newUtil);
        }
    }

    // [MASTER MODELLING] Step 3
    public double getOurUtility(Bid bid) {
        double result = 0;
        int count     = 1;
        for (Entry<Objective, Evaluator> e : ourUtilitySpace.getEvaluators()) {
                double weight = ourUtilitySpace.getWeight(count);
                try {
                    EvaluatorDiscrete value  = (EvaluatorDiscrete) e.getValue();
                    IssueDiscrete issue      = ((IssueDiscrete) e.getKey());
                    ValueDiscrete issuevalue = (ValueDiscrete) bid.getValue(issue.getNumber());

                    Double eval       = Double.valueOf(value.getEvaluationNotNormalized(issuevalue));
                    Double normalizer = Math.pow(value.getEvalMax() + 1, this.gamma);
                    eval              = Math.pow(1 + eval, this.gamma) / normalizer;
                    result            += weight * eval;

                } catch (Exception e1) {
                }
                count++;
            }
        return result;
    }

    // [MASTER MODELLING] Helper function
    public void printMasterPreference() {
        System.out.println("\n =============================== ");

        double sum = 0;
        for (int j = 1; j < this.ourUtilitySpace.getEvaluators().size() + 1; j++) {
            System.out.println("Weight " + j + ": " + this.ourUtilitySpace.getWeight(j));
            sum += this.ourUtilitySpace.getWeight(j);
        }
        System.out.println("Total Sum: " + sum);

        System.out.println("\n =============================== ");
        for (Entry<Objective, Evaluator> e : ourUtilitySpace.getEvaluators()) {
            EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
            IssueDiscrete issue = ((IssueDiscrete) e.getKey());
            System.out.println("Issue : " + issue.getName());

            Double normalizer = Math.pow(value.getEvalMax() + 1, this.gamma );
            for (ValueDiscrete valdis : value.getValues()) {
                double eval = Math.pow(1 + value.getValue(valdis), this.gamma );
                double evaluation = round(eval / normalizer, 2);
                double evaluationOld = round(value.getValue(valdis) / value.getEvalMax(), 2);
                System.out.println("[" + valdis.toString() + "]\t\t: " + value.getValue(valdis) + "/" + value.getEvalMax() + " = "
                        + evaluationOld + " || Ours : " + evaluation);
            }
            System.out.println();
        }
        System.out.println("\n =============================== ");
    }

    // [MASTER MODELLING] Helper function
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

    ////////////////////////////// OTHER FUNCS ///////////////////////

    @Override
    public double getUtility(Bid bid) {
        try {
            return this.smoothedUtils.get(bid);
        } catch (Exception e) {
            System.out.println("Bid does not exist yet: " + bid);
        }
        return 0;
    }

    public static double round(double number, int decimalPlace) {
        BigDecimal bd = new BigDecimal(number);
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public String getDescription() {
        return "Group31_UncertaintyAgent - Example agent that can deal with uncertain preferences";
    }

}
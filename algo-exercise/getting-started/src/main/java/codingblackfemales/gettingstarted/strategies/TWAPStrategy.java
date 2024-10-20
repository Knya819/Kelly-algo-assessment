package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.gettingstarted.helpers.OrderManager;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 10;
    private static final double BUY_BUDGET = 150000;

    private List<BidLevel> localBidLevels = new ArrayList<>();
    private List<AskLevel> localAskLevels = new ArrayList<>();

    private double buyTotal = 0;
    private double sellTotal = 0;
    private long totalBoughtQuantity = 0;  // Track total bought quantity
    private long totalSoldQuantity = 0;    // Track total sold quantity


    @Override
    public Action execute(SimpleAlgoState state) {
        // Step 1: Populate order book if local lists are empty
        if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
            OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);

            // Remove null entries if present
            localBidLevels.removeIf(bidLevel -> bidLevel == null);
            localAskLevels.removeIf(askLevel -> askLevel == null);
       }

        // Step 2: Sort bid and ask levels to maintain time-price priority
        OrderHelper.sortOrderBook(localBidLevels, localAskLevels);
        

        double bidTwap = OrderHelper.calculateBidTWAP(state);
        double askTwap = OrderHelper.calculateAskTWAP(state);

        // Calculate and log remaining budget
        double remainingBudget = BUY_BUDGET - buyTotal;
        logger.info("[TWAPStrategy] Remaining Buy Budget: " + remainingBudget);
        logger.info("[TWAPStrategy] Bought Quantity: " + totalBoughtQuantity + ", Sold Quantity: " + totalSoldQuantity);


        // Step 3: Conditional Buy logic - Only buy if buyTotal is within BUY_BUDGET
        if (remainingBudget > 0 && !localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (AskLevel askLevel : localAskLevels) {
                if (askLevel == null) continue;
                long askPrice = askLevel.price;
                long askQuantity = askLevel.quantity;

                logger.info("[TWAPStrategy] Checking buy logic: Ask Price = " + askPrice + ", Bid TWAP = " + bidTwap);
                
                if (askPrice > bidTwap) {
                    logger.info("[TWAPStrategy] Price too high; checking the next bid level for possible buy...");
                    continue;
                }// not sure here
                
                if (askPrice <= bidTwap && remainingBudget >= (askPrice * askQuantity)) {
                    logger.info("[TWAPStrategy] Placing buy order at price: " + askPrice);
                    Action action = new CreateChildOrder(Side.BUY, askQuantity, askPrice);

                buyTotal += askPrice * askQuantity;
                totalBoughtQuantity += askQuantity;
                remainingBudget = BUY_BUDGET - buyTotal;
                logger.info("[TWAPStrategy] Remaining Buy Budget after purchase: " + remainingBudget);

                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[TWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }
            }
            logger.info("[TWAPStrategy] No bid level meets TWAP condition for buy.");
        }else if (buyTotal >= BUY_BUDGET) {
        logger.info("[TWAPStrategy] Buy budget reached or exceeded. Stopping further buys.");
    }

        // Step 4: Sell logic
if (!localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
    for (BidLevel bidLevel : localBidLevels) {
        if (bidLevel == null) continue;
        long bidPrice = bidLevel.price;
        long bidQuantity = bidLevel.quantity;

        // Ensure we only sell what we have bought
        if (totalSoldQuantity + bidQuantity > totalBoughtQuantity) {
            bidQuantity = totalBoughtQuantity - totalSoldQuantity;
            if (bidQuantity <= 0) {
                logger.info("[TWAPStrategy] No more quantity to sell. Stopping sell.");
                break;
            }
        }

        logger.info("[TWAPStrategy] Checking sell logic: Bid Price = " + bidPrice + ", Ask TWAP = " + askTwap);
       // logger.info("[TWAPStrategy] Bought Quantity: " + totalBoughtQuantity + ", Sold Quantity: " + totalSoldQuantity);

        // Check if bid price is within the profit target range (5%-7% above TWAP)
        if (bidPrice > askTwap && OrderHelper.isWithinProfitTargetInterval(bidTwap, bidPrice)) {
            logger.info("[TWAPStrategy] Bid price within profit target range; placing sell order.");
            Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

            sellTotal += bidPrice * bidQuantity;
            totalSoldQuantity += bidQuantity;  // Track total sold quantity

            OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
            logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

            OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
            return action;
        }

        // Check if bid price is within the stop-loss range (92%-95% of Bid TWAP)
        if (bidPrice < bidTwap && OrderHelper.isWithinStopLossInterval(bidTwap, bidPrice)) {
            logger.info("[TWAPStrategy] Bid price within stop-loss range; placing sell order.");
            Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

            sellTotal += bidPrice * bidQuantity;
            totalSoldQuantity += bidQuantity;  // Track total sold quantity

            OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity); // Correct update to bid levels
            logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

            OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
            return action;
        }

        if (totalSoldQuantity > totalBoughtQuantity) {
            logger.error("[TWAPStrategy] Sold quantity exceeds bought quantity! Investigate further.");
            return NoAction.NoAction;
        }

        // Log if the bid price does not meet TWAP or interval conditions
        logger.info("[TWAPStrategy] Bid price does not meet Ask TWAP, profit target, or stop-loss; checking the next bid level...");
    }
    logger.info("[TWAPStrategy] No bid level meets the conditions for sell.");
}


  // Clear lists to avoid reuse of stale data in the next call
        localBidLevels.clear();
        localAskLevels.clear();

        logger.info("[TWAPStrategy] No action required, done for now.");


        // Step 5: Final profit calculation and clearing lists
        OrderHelper.calculateProfit(buyTotal, sellTotal);

      
        return NoAction.NoAction;
    }
}

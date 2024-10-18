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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 10;
    private static final double BUY_BUDGET = 100000;

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

        double bidTWAP = OrderHelper.calculateBidTWAP(state);
        double askTWAP = OrderHelper.calculateAskTWAP(state);

        // Calculate and log remaining budget
        double remainingBudget = BUY_BUDGET - buyTotal;
        logger.info("[TWAPStrategy] Remaining Buy Budget: " + remainingBudget);
        logger.info("[TWAPStrategy] Filled Bought Quantity: " + totalBoughtQuantity + ", Filled Sold Quantity: " + totalSoldQuantity);

        // Step 3: Conditional Buy logic - Only buy if buyTotal is within BUY_BUDGET
        if (remainingBudget > 0 && !localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (BidLevel bidLevel : localBidLevels) {
                if (bidLevel == null) continue;
                long price = bidLevel.price;
                long bidQuantity = bidLevel.quantity;

                logger.info("[TWAPStrategy] Checking buy logic: Bid Price = " + price + ", Bid TWAP = " + bidTWAP);

                if (price > bidTWAP) {
                    logger.info("[TWAPStrategy] Price too high; checking the next bid level for possible buy...");
                    continue;
                }

                if (price <= bidTWAP && remainingBudget >= (price * bidQuantity)) {
                    logger.info("[TWAPStrategy] Placing buy order at price: " + price);
                    Action action = new CreateChildOrder(Side.BUY, bidQuantity, price);

                    buyTotal += price * bidQuantity;
                    totalBoughtQuantity += bidQuantity;  // Track total bought quantity
                    remainingBudget = BUY_BUDGET - buyTotal;
                    logger.info("[TWAPStrategy] Remaining Buy Budget after purchase: " + remainingBudget);
                    logger.info("[TWAPStrategy] Filled Bought Quantity: " + totalBoughtQuantity + ", Filled Sold Quantity: " + totalSoldQuantity);

                    OrderHelper.updateBidLevels(localBidLevels, price, bidQuantity);
                    logger.info("[TWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }
            }
            logger.info("[TWAPStrategy] No bid level meets TWAP condition for buy.");
        } else if (remainingBudget <= 0) {
            logger.info("[TWAPStrategy] Buy budget exhausted. Stopping further buys.");
        }

        // Step 4: Sell logic - Only sell if there is enough bought quantity to sell
        if (!localAskLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (AskLevel askLevel : localAskLevels) {
                if (askLevel == null) continue;
                long askPrice = askLevel.price;
                long askQuantity = askLevel.quantity;

                // Ensure we only sell what we have bought
                if (totalSoldQuantity + askQuantity > totalBoughtQuantity) {
                    askQuantity = totalBoughtQuantity - totalSoldQuantity;
                    if (askQuantity <= 0) {
                        logger.info("[TWAPStrategy] No more quantity to sell. Stopping sell.");
                        break;
                    }
                }

                logger.info("[TWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask TWAP = " + askTWAP);
                logger.info("[TWAPStrategy] Filled Bought Quantity: " + totalBoughtQuantity + ", Filled Sold Quantity: " + totalSoldQuantity);

                // Check if ask price is within the profit target range (5%-7% above TWAP)
                if (OrderHelper.isWithinProfitTargetIntervalTwap(askTWAP, askPrice)) {
                    logger.info("[TWAPStrategy] Ask price within profit target range; placing sell order.");
                    Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                    sellTotal += askPrice * askQuantity;
                    totalSoldQuantity += askQuantity;  // Track total sold quantity
                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
                    logger.info("[TWAPStrategy] Filled Bought Quantity: " + totalBoughtQuantity + ", Filled Sold Quantity: " + totalSoldQuantity);

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }

                // Check if ask price is within the stop-loss range (92%-95% of TWAP)
                if (OrderHelper.isWithinStopLossIntervalTwap(askTWAP, askPrice)) {
                    logger.info("[TWAPStrategy] Ask price within stop-loss range; placing sell order.");
                    Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                    sellTotal += askPrice * askQuantity;
                    totalSoldQuantity += askQuantity;  // Track total sold quantity
                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
                    logger.info("[TWAPStrategy] Filled Bought Quantity: " + totalBoughtQuantity + ", Filled Sold Quantity: " + totalSoldQuantity);

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }

                // Log if the ask price does not meet TWAP or interval conditions
                logger.info("[TWAPStrategy] Price does not meet TWAP, profit target, or stop-loss; checking the next ask level...");
            }
            logger.info("[TWAPStrategy] No ask level meets the conditions for sell.");
        }

        // Clear lists to avoid reuse of stale data in the next call
        localBidLevels.clear();
        localAskLevels.clear();

        logger.info("[TWAPStrategy] No action required, done for now.");

        // Step 5: Final profit calculation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        return NoAction.NoAction;
    }
}

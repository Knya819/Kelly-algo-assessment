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
    private static final long MAX_ACTIVE_ORDERS = 10; // Maximum active orders (can be made dynamic)

    private List<BidLevel> localBidLevels = new ArrayList<>();  // Local copy of bid levels
    private List<AskLevel> localAskLevels = new ArrayList<>();  // Local copy of ask levels

    // Variables to track buy and sell information for profit calculation
    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
public Action execute(SimpleAlgoState state) {
    // Refresh the order book from the latest state if both local lists are empty
    if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
        OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);


        // If order book is completely empty after an attempt to populate, stop the logic
        if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
            logger.info("[TWAPStrategy] Order book is completely empty after attempting to refresh. No action will be taken.");
            return NoAction.NoAction;
        }
    }

    // Sort bid and ask levels to maintain time-price priority
    OrderHelper.sortOrderBook(localBidLevels, localAskLevels);

    // Log the initial sorted state of the order book
    logger.info("[MyAlgoLogic] Current Market State (sorted by time-price priority):\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

    // Log bid and ask level counts after sorting
    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);

    double bidTwap = OrderHelper.calculateBidTWAP(state);
    double askTwap = OrderHelper.calculateAskTWAP(state);

    // BUY Logic: Execute only if bid levels are available
    if (!localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
        for (BidLevel bidLevel : localBidLevels) {
            long price = bidLevel.price;
            long bidQuantity = Math.round(bidLevel.quantity * 1); // Use 100% of bidLevel quantity

            logger.info("[TWAPStrategy] Checking buy logic: Bid Price = " + price + ", Bid TWAP = " + bidTwap);
            if (price <= bidTwap ) {
                logger.info("[TWAPStrategy] Placing buy order below Bid TWAP: " + bidTwap + " at price: " + price);
                Action action = new CreateChildOrder(Side.BUY, bidQuantity, price);

                // Accumulate the buy total
                buyTotal += price * bidQuantity;

                // Update the local bid levels using OrderHelper
                OrderHelper.updateBidLevels(localBidLevels, price, bidQuantity);
                logger.info("[TWAPStrategy] Updated local order book state after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                // Log updated bid and ask level counts after buy
                OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);

                return action;
            } else {
                logger.info("[TWAPStrategy] Bid price " + price + " does not meet TWAP condition. Skipping to next bid level.");
            }
        }
        logger.info("[TWAPStrategy] No bid level meets TWAP condition for buy.");
    }

    // SELL Logic: Execute only if ask levels are available
    if (!localAskLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
        for (AskLevel askLevel : localAskLevels) {
            long askPrice = askLevel.price;
            long askQuantity = Math.round(askLevel.quantity * 1); // Use 100% of askLevel quantity

            logger.info("[TWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask TWAP = " + askTwap);
            if (askPrice >= askTwap) {
                logger.info("[TWAPStrategy] Placing sell order above Ask TWAP: " + askTwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Accumulate the sell total
                sellTotal += askPrice * askQuantity;

                // Update the local ask levels using OrderHelper
                OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                logger.info("[TWAPStrategy] Updated local order book state after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                // Log updated bid and ask level counts after sell
                OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);

                return action;
            } else {
                logger.info("[TWAPStrategy] Ask price " + askPrice + " does not meet TWAP condition. Skipping to next ask level.");
            }
        }
        logger.info("[TWAPStrategy] No ask level meets TWAP condition for sell.");
    }

    // Calculate profit at the end of the evaluation
    OrderHelper.calculateProfit(buyTotal, sellTotal);

    logger.info("[TWAPStrategy] No action required, done for now.");

    // Clear lists to avoid reuse of stale data in the next call
    localBidLevels.clear();
    localAskLevels.clear();

    return NoAction.NoAction;
    }   


}
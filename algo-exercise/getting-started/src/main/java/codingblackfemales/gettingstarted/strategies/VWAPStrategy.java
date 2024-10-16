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

public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 10;
    private static final double BUY_BUDGET = 100000;

    private List<BidLevel> localBidLevels = new ArrayList<>();
    private List<AskLevel> localAskLevels = new ArrayList<>();

    private double buyTotal = 0;
    private double sellTotal = 0;

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
        

        double bidVwap = OrderHelper.calculateBidVWAP(state);
        double askVwap = OrderHelper.calculateAskVWAP(state);

        // Calculate and log remaining budget
        double remainingBudget = BUY_BUDGET - buyTotal;
        logger.info("[VWAPStrategy] Remaining Buy Budget: " + remainingBudget);
        

        // Step 3: Conditional Buy logic - Only buy if buyTotal is within BUY_BUDGET
        if (buyTotal < BUY_BUDGET && !localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (BidLevel bidLevel : localBidLevels) {
                if (bidLevel == null) continue;
                long price = bidLevel.price;
                long bidQuantity = bidLevel.quantity;

                logger.info("[VWAPStrategy] Checking buy logic: Bid Price = " + price + ", Bid VWAP = " + bidVwap);
                
                if (price > bidVwap) {
                    logger.info("[VWAPStrategy] Price too high; checking the next bid level for possible buy...");
                    continue;
                }
                
                if (price <= bidVwap) {
                    logger.info("[VWAPStrategy] Placing buy order at price: " + price);
                    Action action = new CreateChildOrder(Side.BUY, bidQuantity, price);

                buyTotal += price * bidQuantity;
                remainingBudget = BUY_BUDGET - buyTotal;
                logger.info("[VWAPStrategy] Remaining Buy Budget after purchase: " + remainingBudget);

                    OrderHelper.updateBidLevels(localBidLevels, price, bidQuantity);
                    logger.info("[VWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }
            }
            logger.info("[VWAPStrategy] No bid level meets VWAP condition for buy.");
        }else if (buyTotal >= BUY_BUDGET) {
        logger.info("[VWAPStrategy] Buy budget reached or exceeded. Stopping further buys.");
    }

        // Step 4: Sell logic
if (!localAskLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
    for (AskLevel askLevel : localAskLevels) {
        if (askLevel == null) continue;
        long askPrice = askLevel.price;
        long askQuantity = askLevel.quantity;

        logger.info("[VWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask VWAP = " + askVwap);

        // Check if ask price is within the profit target range (5%-7% above VWAP)
        if (OrderHelper.isWithinProfitTargetInterval(askVwap,  askPrice)) {
            logger.info("[VWAPStrategy] Ask price within profit target range; placing sell order.");
            Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

            sellTotal += askPrice * askQuantity;
            OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
            logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

            OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
            return action;
        }

        // Check if ask price is within the stop-loss range (92%-95% of VWAP)
        if (OrderHelper.isWithinStopLossInterval(askVwap, askPrice)) {
            logger.info("[VWAPStrategy] Ask price within stop-loss range; placing sell order.");
            Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

            sellTotal += askPrice * askQuantity;
            OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
            logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

            OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
            return action;
        }

        // Log if the ask price does not meet VWAP or interval conditions
        logger.info("[VWAPStrategy] Price does not meet VWAP, profit target, or stop-loss; checking the next ask level...");
    }
    logger.info("[VWAPStrategy] No ask level meets the conditions for sell.");
}


  // Clear lists to avoid reuse of stale data in the next call
        localBidLevels.clear();
        localAskLevels.clear();

                logger.info("[VWAPStrategy] No action required, done for now.");


        // Step 5: Final profit calculation and clearing lists
        OrderHelper.calculateProfit(buyTotal, sellTotal);

      
        return NoAction.NoAction;
    }
}

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

import java.util.List;

public class VWAPStrategy implements ExecutionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 3; // Can be made dynamic

    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> localBidLevels, List<AskLevel> localAskLevels) {
        // Initialize local bid and ask levels if they are empty
        if (localBidLevels.isEmpty()) {
            for (int i = 0; i < state.getBidLevels(); i++) {
                localBidLevels.add(state.getBidAt(i));  
            }
        }
        if (localAskLevels.isEmpty()) {
            for (int i = 0; i < state.getAskLevels(); i++) {
                localAskLevels.add(state.getAskAt(i));  
            }
        }

        // Log the state of the order book
        logger.info("[MYALGO] The state of the order book is:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

        // Step 1: Manage orders (e.g., cancel fully filled or if too many active orders)
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            return manageOrdersAction;  // Handle any actions like cancellations
        }

        double bidVwap = OrderHelper.calculateBidVWAP(state);
        double askVwap = OrderHelper.calculateAskVWAP(state);

        if (localBidLevels.isEmpty()) {
            logger.info("[MYALGO] No available bid levels, no action possible.");
            return NoAction.NoAction;
        }

        if (localAskLevels.isEmpty()) {
            logger.info("[MYALGO] No available ask levels, no action possible.");
            return NoAction.NoAction;
        }

        boolean canPlaceMoreOrders = state.getActiveChildOrders().size() < 3;

        // Buy logic: Buy if the price is below BidVWAP and fewer than the max allowed active orders
        if (canPlaceMoreOrders) {
            BidLevel bestBid = localBidLevels.get(0);
            long bidPrice = bestBid.price;
            long bidQuantity = bestBid.quantity;

            if (bidPrice <= bidVwap) {
                logger.info("[MYALGO] Placing buy order below BidVWAP: " + bidVwap + " at price: " + bidPrice);
                Action action = new CreateChildOrder(Side.BUY, bidQuantity, bidPrice);

                // Accumulate the buy total
                buyTotal += bidPrice * bidQuantity;

                // Update the local bid levels
                OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
                logger.info("[MYALGO] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                return action;
            }
        }

        // SELL Logic: Sell if the price is above AskVWAP and fewer than the max allowed active orders
        if (canPlaceMoreOrders) {
            AskLevel bestAsk = localAskLevels.get(0);
            long askPrice = bestAsk.price;
            long askQuantity = bestAsk.quantity;

            if (askPrice >= askVwap) {
                logger.info("[MYALGO] Placing sell order above AskVWAP: " + askVwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Accumulate the sell total
                sellTotal += askPrice * askQuantity;

                // Update the local ask levels
                OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                logger.info("[MYALGO] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                return action;
            }
        }

        // Calculate profit at the end of the evaluation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[MYALGO] No action required, done for now.");
        return NoAction.NoAction;
    }
}

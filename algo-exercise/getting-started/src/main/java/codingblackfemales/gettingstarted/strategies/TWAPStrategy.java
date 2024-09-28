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

public class TWAPStrategy implements ExecutionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);

    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> localBidLevels, List<AskLevel> localAskLevels) {
        // Initialize local bid and ask levels if empty
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

        // Step 1: Use OrderManager to handle fully filled orders or too many active orders
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            return manageOrdersAction;  // If any action is returned (cancel order), return it
        }

        double bidTwap = OrderHelper.calculateBidTWAP(state);
        double askTwap = OrderHelper.calculateAskTWAP(state);

        if (localBidLevels.isEmpty()) {
            logger.info("[MYALGO] No available bid levels, no action possible.");
            return NoAction.NoAction;
        }

        if (localAskLevels.isEmpty()) {
            logger.info("[MYALGO] No available ask levels, no action possible.");
            return NoAction.NoAction;
        }

        // Buy logic: Buy if the price is below BidTWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < 3) {
            BidLevel bestBid = localBidLevels.get(0);
            long price = bestBid.price;
            long quantity = bestBid.quantity;

            if (price <= bidTwap) {
                logger.info("[MYALGO] Placing buy order below BidTWAP: " + bidTwap + " at price: " + price);
                Action action = new CreateChildOrder(Side.BUY, quantity, price);

                // Accumulate the buy total
                buyTotal += price * quantity;

                // Update the local bid levels using OrderHelper
                OrderHelper.updateBidLevels(localBidLevels, price, quantity);
                logger.info("[MYALGO] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                return action;
            }
        }

        // SELL Logic: Sell if the price is above AskTWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < 3) {
            AskLevel bestAsk = localAskLevels.get(0);  // Get the best ask level
            long askPrice = bestAsk.price;
            long askQuantity = bestAsk.quantity;

            if (askPrice >= askTwap) {
                logger.info("[MYALGO] Placing sell order above AskTWAP: " + askTwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Accumulate the sell total
                sellTotal += askPrice * askQuantity;

                // Update the local ask levels using OrderHelper
                OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                logger.info("[MYALGO] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                return action;
            }
        }

        // Call the OrderHelper to calculate profit at the end of the evaluation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[MYALGO] No action required, done for now.");
        return NoAction.NoAction;
    }
}

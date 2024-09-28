package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.gettingstarted.helpers.OrderManager;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *TO DO
 *1) make sure OrderManager is comprehensive enough to handle all the edge cases
 * like stale orders, partial fills, etc.
 * 2) Consider enhancing this method by integrating profit/loss 
 * 3) call state.getActiveChildOrders().size() in the helper
 * 4)sell first, then buy, or balance them or handle both simultaneously
 * I can just supose that we have a specific buject or we are in a certain position ~(long short)
 * 5) find a solution for MAX_ACTIVE_ORDERS
 * 6)Consider adding logic to handle edge cases like what happens when no orders can be placed ,This could include a strategy for waiting, changing tactics, or even exiting the market temporarily.
 */



public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    private static final long MAX_ACTIVE_ORDERS = 3; // Maximum active orders (can be made dynamic)

    private List<BidLevel> localBidLevels = new ArrayList<>();  // Local copy of bid levels
    private List<AskLevel> localAskLevels = new ArrayList<>();  // Local copy of ask levels

    // Variables to track buy and sell information for profit calculation
    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action evaluate(SimpleAlgoState state) {

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

        //Use OrderManager to handle fully filled orders or too many active orders
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
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
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
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
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

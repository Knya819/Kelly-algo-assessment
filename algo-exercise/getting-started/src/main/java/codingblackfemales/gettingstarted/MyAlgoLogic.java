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
 * MyAlgoLogic implementing TWAP strategy.
 */
public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    private static final long MAX_ACTIVE_ORDERS = 101; // Maximum active orders (can be made dynamic)

    private List<BidLevel> localBidLevels = new ArrayList<>();  // Local copy of bid levels
    private List<AskLevel> localAskLevels = new ArrayList<>();  // Local copy of ask levels

    // Variables to track buy and sell totals for profit calculation
    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action evaluate(SimpleAlgoState state) {

        // Initialize local bid and ask levels if empty
        if (localBidLevels.isEmpty()) {
            for (int i = 0; i < state.getBidLevels(); i++) {  // Assuming this returns the number of bid levels
                BidLevel bidLevel = state.getBidAt(i);  // Retrieve each BidLevel
                if (bidLevel != null) {
                    localBidLevels.add(bidLevel);  // Add BidLevel to the list
                }
            }
        }
        if (localAskLevels.isEmpty()) {
            for (int i = 0; i < state.getAskLevels(); i++) {  // Assuming this returns the number of ask levels
                AskLevel askLevel = state.getAskAt(i);  // Retrieve each AskLevel
                if (askLevel != null) {
                    localAskLevels.add(askLevel);  // Add AskLevel to the list
                }
            }
        }

        // Log the state of the order book
        logger.info("[MYALGO] The state of the order book is:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

        // Use OrderManager to handle fully filled orders or too many active orders
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            return manageOrdersAction;  // If any action is returned (cancel order), return it
        }

        // Calculate TWAP for bids and asks
        double bidTwap = OrderHelper.calculateBidTWAP(state);
        double askTwap = OrderHelper.calculateAskTWAP(state);

        // Buy logic: Buy if the price is below BidTWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            if (!localBidLevels.isEmpty()) {
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
            } else {
                logger.info("[MYALGO] No available bid levels, no buy action possible.");
            }
        }

        // Sell logic: Sell if the price is above AskTWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            if (!localAskLevels.isEmpty()) {
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
            } else {
                logger.info("[MYALGO] No available ask levels, no sell action possible.");
            }
        }

        // Call the OrderHelper to calculate profit at the end of the evaluation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[MYALGO] No action required, done for now.");
        return NoAction.NoAction;
    }
}

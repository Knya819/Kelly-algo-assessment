package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.gettingstarted.helpers.OrderManager;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    private static final long MAX_ACTIVE_ORDERS = 3; // Maximum active orders (can be made dynamic)

    private List<BidLevel> localBidLevels = new ArrayList<>();  // Local copy of bid levels
    private List<AskLevel> localAskLevels = new ArrayList<>();  // Local copy of ask levels

    // Variables to track buy and sell information for profit calculation
    private long buyPrice = 0;
    private long buyQuantity = 0;
    private long sellPrice = 0;
    private long sellQuantity = 0;

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
        logger.info("[MYALGO] The state of the order book is:\n" + formatOrderBook());

        // Step 1: Use OrderManager to handle fully filled orders or too many active orders
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            return manageOrdersAction;  // If any action is returned (cancel order), return it
        }

        // Buy logic: Buy if the price is below BidVWAP and fewer than the max allowed active orders
        double bidVwap = OrderHelper.calculateBidVWAP(state);

        if (localBidLevels.isEmpty()) {
            logger.info("[MYALGO] No available bid levels, no action possible.");
            return NoAction.NoAction;
        }

        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            BidLevel bestBid = localBidLevels.get(0);
            long price = bestBid.price;
            long quantity = bestBid.quantity;

            if (price <= bidVwap) {
                logger.info("[MYALGO] Placing buy order below BidVWAP: " + bidVwap + " at price: " + price);
                Action action = new CreateChildOrder(Side.BUY, quantity, price);

                // Track the buy price and quantity for profit calculation
                buyPrice = price;
                buyQuantity = quantity;

                // Update the local bid levels
                updateLocalBidLevels(price, quantity);
                logger.info("[MYALGO] Updated local order book state:\n" + formatOrderBook());

                return action;
            }
        }

        // SELL Logic: Sell if the price is above AskVWAP and fewer than the max allowed active orders
        double askVwap = OrderHelper.calculateAskVWAP(state);

        if (localAskLevels.isEmpty()) {
            logger.info("[MYALGO] No available ask levels, no action possible.");
            return NoAction.NoAction;
        }

        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            AskLevel bestAsk = localAskLevels.get(0);  // Get the best ask level
            long askPrice = bestAsk.price;
            long askQuantity = bestAsk.quantity;

            if (askPrice >= askVwap) {
                logger.info("[MYALGO] Placing sell order above AskVWAP: " + askVwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Track the sell price and quantity for profit calculation
                sellPrice = askPrice;
                sellQuantity = askQuantity;

                // Update the local ask levels
                updateLocalAskLevels(askPrice, askQuantity);
                logger.info("[MYALGO] Updated local order book state:\n" + formatOrderBook());

                // Calculate profit after sell
                calculateProfit();

                return action;
            }
        }

        logger.info("[MYALGO] No action required, done for now.");
        return NoAction.NoAction;
    }

  

    // Method to update local bid levels by reducing quantity and removing fully filled levels
    private void updateLocalBidLevels(long price, long filledQuantity) {
        for (int i = 0; i < localBidLevels.size(); i++) {
            BidLevel bidLevel = localBidLevels.get(i);
            if (bidLevel.price == price) {
                long remainingQuantity = bidLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[MYALGO] Removing bid level at price: " + price + " as quantity " + filledQuantity + " is fully bought.");
                    localBidLevels.remove(i);  // Remove from the local copy
                } else {
                    bidLevel.quantity = remainingQuantity;  // Update the quantity locally
                    logger.info("[MYALGO] Updated bid level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;  // Exit the loop once the level is updated or removed
            }
        }
    }

    // Method to update local ask levels by reducing quantity and removing fully filled levels
    private void updateLocalAskLevels(long price, long filledQuantity) {
        for (int i = 0; i < localAskLevels.size(); i++) {
            AskLevel askLevel = localAskLevels.get(i);
            if (askLevel.price == price) {
                long remainingQuantity = askLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[MYALGO] Removing ask level at price: " + price + " as quantity " + filledQuantity + " is fully sold.");
                    localAskLevels.remove(i);  // Remove from the local copy
                } else {
                    askLevel.quantity = remainingQuantity;  // Update the quantity locally
                    logger.info("[MYALGO] Updated ask level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;  // Exit the loop once the level is updated or removed
            }
        }
    }

    // Utility method to format the order book for better logging
    private String formatOrderBook() {
        StringBuilder sb = new StringBuilder("|----BID-----|\n");
        for (BidLevel level : localBidLevels) {
            sb.append(String.format("%6d @ %6d\n", level.quantity, level.price));
        }
        sb.append("|----ASK-----|\n");
        for (AskLevel level : localAskLevels) {
            sb.append(String.format("%6d @ %6d\n", level.quantity, level.price));
        }
        return sb.toString();
    }

      // Profit calculation method
    private void calculateProfit() {
        long buyTotal = buyPrice * buyQuantity;
        long sellTotal = sellPrice * sellQuantity;
        long profit = sellTotal - buyTotal;
        logger.info("[MYALGO] Profit from the trade: " + profit);
    }
}

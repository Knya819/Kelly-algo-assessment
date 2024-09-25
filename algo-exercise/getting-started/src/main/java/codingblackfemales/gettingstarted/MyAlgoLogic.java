package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.gettingstarted.helpers.OrderHelper;  

import messages.order.Side;


import codingblackfemales.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    private static final long MAX_ACTIVE_ORDERS = 3; // Maximum active orders (can be made dynamic)

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        long quantity = 75; // Define the quantity for orders
        long price = state.getBidAt(0).price; // Use the best bid price for reference
        long filledQuantity = calculateFilledQuantity(state); // Filled quantity of orders
        long activeOrders = state.getActiveChildOrders().size(); // Current number of active child orders
        double vwap = OrderHelper.calculateVWAP(state); // Calculate VWAP for decision making

        logger.info("[MYALGO] VWAP: " + vwap + ", Current price: " + price);

        // BUY Logic: Buy if the price is below VWAP and fewer than the max allowed active orders
        if (price < vwap && activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[MYALGO] Placing buy order below VWAP at price: " + price);
            Action action = new CreateChildOrder(Side.BUY, quantity, price);
            updateOrderBook(state, action);  // Update the order book after the action
            return action;
        }

        // SELL Logic: Profit-taking if the price is within the profit-taking interval
        if (filledQuantity >= quantity && OrderHelper.isWithinProfitTargetInterval(state, price)) {
            logger.info("[MYALGO] Taking profit by selling at price: " + price);
            Action action = new CreateChildOrder(Side.SELL, filledQuantity, price);
            updateOrderBook(state, action);  // Update the order book after the action
            return action;
        }

        // Stop-Loss Logic: Sell if the price falls within the stop-loss interval
        if (filledQuantity >= quantity && OrderHelper.isWithinStopLossInterval(state, price)) {
            logger.info("[MYALGO] Cutting losses by selling at price: " + price);
            Action action = new CreateChildOrder(Side.SELL, filledQuantity, price);
            updateOrderBook(state, action);  // Update the order book after the action
            return action;
        }

        // CANCEL Logic: Cancel the oldest order if too many active orders are present
        if (activeOrders >= MAX_ACTIVE_ORDERS) {
            Action action = handleCancelLogic(state);
            updateOrderBook(state, action);  // Update the order book after the action
            return action;
        }

        // No action if none of the conditions are met
        return NoAction.NoAction;
    }

    // Helper method to calculate the total filled quantity
    private long calculateFilledQuantity(SimpleAlgoState state) {
        return state.getChildOrders().stream()
                .mapToLong(ChildOrder::getFilledQuantity)
                .sum();
    }

    // CANCEL LOGIC: Cancel the oldest active order to keep the number of active orders within the limit
    private Action handleCancelLogic(SimpleAlgoState state) {
        var oldestOrder = state.getActiveChildOrders().stream().findFirst();
        if (oldestOrder.isPresent()) {
            logger.info("[MYALGO] Canceling oldest order: " + oldestOrder.get());
            return new CancelChildOrder(oldestOrder.get());
        }
        return NoAction.NoAction;
    }

    // ORDER BOOK UPDATE: Update the order book after an action is taken (buy, sell, cancel)
    private void updateOrderBook(SimpleAlgoState state, Action action) {
        // Logic to update the order book based on the action taken
        logger.info("[MYALGO] Updating the order book after action: " + action);

        // This is a placeholder for actual order book update logic.
        // You would update the bid/ask levels or child orders based on the action.
        if (action instanceof CreateChildOrder) {
            // Add logic to handle adding orders to the order book
            logger.info("[MYALGO] Order book updated with new order: " + action);
        } else if (action instanceof CancelChildOrder) {
            // Add logic to handle removing or modifying the order in the order book
            logger.info("[MYALGO] Order book updated with canceled order: " + action);
        }
    }
}


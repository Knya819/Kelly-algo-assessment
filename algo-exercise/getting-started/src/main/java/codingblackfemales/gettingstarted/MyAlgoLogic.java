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

        long quantity = 99; // Define the quantity for orders
        long price = state.getBidAt(0).price; // Use the best bid price for reference
        //long filledQuantity = calculateFilledQuantity(state); // Filled quantity of orders
        long activeOrders = state.getActiveChildOrders().size(); // Current number of active child orders
        double vwap = OrderHelper.calculateVWAP(state); // Calculate VWAP for decision making

        logger.info("[MYALGO] VWAP: " + vwap + ", Current price: " + price);

        // BUY Logic: Buy if the price is below VWAP and fewer than the max allowed active orders
        if ( activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[MYALGO] Placing buy order below VWAP at price: " + price);
            logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children, want 3, joining passive side of book with: " + quantity + " @ " + price);

            Action action = new CreateChildOrder(Side.BUY, quantity, price);
            return action;
        }else{
            logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children, want 3, done.");
            return NoAction.NoAction;
        }

       }
    }

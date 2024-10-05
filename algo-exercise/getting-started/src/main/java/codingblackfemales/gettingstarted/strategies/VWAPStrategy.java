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

public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 100; // Maximum active orders (can be made dynamic)

    private List<BidLevel> localBidLevels = new ArrayList<>();  // Local copy of bid levels
    private List<AskLevel> localAskLevels = new ArrayList<>();  // Local copy of ask levels

    // Variables to track buy and sell information for profit calculation
    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state) {

        // Initialize local bid and ask levels if empty
        if (localBidLevels.isEmpty()) {
            for (int i = 0; i < state.getBidLevels(); i++) {
                localBidLevels.add(state.getBidAt(i));  
            }
            logger.info("[VWAPStrategy] Bid levels initialized: " + localBidLevels);
        }
        if (localAskLevels.isEmpty()) {
            for (int i = 0; i < state.getAskLevels(); i++) {
                localAskLevels.add(state.getAskAt(i));  
            }
            logger.info("[VWAPStrategy] Ask levels initialized: " + localAskLevels);
        }

        // Use OrderManager to handle fully filled orders or too many active orders
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            logger.info("[VWAPStrategy] OrderManager is canceling orders.");
            return manageOrdersAction;  // If any action is returned (cancel order), return it
        }

        double bidVwap = OrderHelper.calculateBidVWAP(state);
        double askVwap = OrderHelper.calculateAskVWAP(state);

        // Buy logic: Buy if the price is below BidVWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            BidLevel bestBid = localBidLevels.get(0);
            long price = bestBid.price;
            long quantity = bestBid.quantity;

            logger.info("[VWAPStrategy] Checking buy logic: Bid Price = " + price + ", Bid VWAP = " + bidVwap);
            if (price <= bidVwap) {
                logger.info("[VWAPStrategy] Placing buy order below Bid VWAP: " + bidVwap + " at price: " + price);
                Action action = new CreateChildOrder(Side.BUY, quantity, price);

                // Accumulate the buy total
                buyTotal += price * quantity;

                // Update the local bid levels using OrderHelper
                OrderHelper.updateBidLevels(localBidLevels, price, quantity);
                logger.info("[VWAPStrategy] Updated local order book state after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                logger.info("[VWAPStrategy] Active orders count after buy: " + state.getActiveChildOrders().size());
                return action;
            }
        }

        // SELL Logic: Sell if the price is above AskVWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            AskLevel bestAsk = localAskLevels.get(0);  // Get the best ask level
            long askPrice = bestAsk.price;
            long askQuantity = bestAsk.quantity;

            logger.info("[VWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask VWAP = " + askVwap);
            if (askPrice >= askVwap) {
                logger.info("[VWAPStrategy] Placing sell order above Ask VWAP: " + askVwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Accumulate the sell total
                sellTotal += askPrice * askQuantity;

                // Update the local ask levels using OrderHelper
                OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                logger.info("[VWAPStrategy] Updated local order book state after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                return action;
            }
        }

        // Call the OrderHelper to calculate profit at the end of the evaluation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[VWAPStrategy] No action required, done for now.");
        return NoAction.NoAction;
    }
}

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

/**
 * VWAP strategy execution based on market conditions and bid/ask levels.
 */
public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 101; // Maximum active orders (can be made dynamic)

    // Variables to track buy and sell totals for profit calculation
    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> localBidLevels, List<AskLevel> localAskLevels) {

        // Log the state of the order book
        logger.info("[VWAPStrategy] The state of the order book is:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

        // Use OrderManager to handle fully filled orders or too many active orders
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            return manageOrdersAction;  // Return if any order management action (like cancel) is required
        }

        // Calculate VWAP for bids and asks
        double bidVwap = OrderHelper.calculateBidVWAP(state);
        double askVwap = OrderHelper.calculateAskVWAP(state);

        // Buy logic: Buy if the price is below BidVWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            if (!localBidLevels.isEmpty()) {
                BidLevel bestBid = localBidLevels.get(0);
                long price = bestBid.price;
                long quantity = bestBid.quantity;

                if (price <= bidVwap) {
                    logger.info("[VWAPStrategy] Placing buy order below BidVWAP: " + bidVwap + " at price: " + price);
                    Action action = new CreateChildOrder(Side.BUY, quantity, price);

                    // Accumulate the buy total
                    buyTotal += price * quantity;

                    // Update the local bid levels using OrderHelper
                    OrderHelper.updateBidLevels(localBidLevels, price, quantity);
                    logger.info("[VWAPStrategy] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    return action;
                }
            } else {
                logger.info("[VWAPStrategy] No available bid levels, no buy action possible.");
            }
        }

        // Sell logic: Sell if the price is above AskVWAP and fewer than the max allowed active orders
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            if (!localAskLevels.isEmpty()) {
                AskLevel bestAsk = localAskLevels.get(0);  // Get the best ask level
                long askPrice = bestAsk.price;
                long askQuantity = bestAsk.quantity;

                if (askPrice >= askVwap) {
                    logger.info("[VWAPStrategy] Placing sell order above AskVWAP: " + askVwap + " at price: " + askPrice);
                    Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                    // Accumulate the sell total
                    sellTotal += askPrice * askQuantity;

                    // Update the local ask levels using OrderHelper
                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[VWAPStrategy] Updated local order book state:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    return action;
                }
            } else {
                logger.info("[VWAPStrategy] No available ask levels, no sell action possible.");
            }
        }

        // Call the OrderHelper to calculate profit at the end of the evaluation
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[VWAPStrategy] No action required, done for now.");
        return NoAction.NoAction;
    }
}

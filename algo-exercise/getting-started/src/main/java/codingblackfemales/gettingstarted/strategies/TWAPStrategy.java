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

public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 10;
    private static final double tolerance = 15.0;

    private List<BidLevel> localBidLevels = new ArrayList<>();
    private List<AskLevel> localAskLevels = new ArrayList<>();

    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state) {

        // Refresh local bid and ask levels on each tick to get the latest market data
        localBidLevels.clear();
        for (int i = 0; i < state.getBidLevels(); i++) {
            localBidLevels.add(state.getBidAt(i));
        }
        logger.info("[TWAPStrategy] Refreshed bid levels: " + localBidLevels);

        localAskLevels.clear();
        for (int i = 0; i < state.getAskLevels(); i++) {
            localAskLevels.add(state.getAskAt(i));
        }
        logger.info("[TWAPStrategy] Refreshed ask levels: " + localAskLevels);

        // Enforce active order limit using OrderManager
        Action manageOrdersAction = OrderManager.manageOrders(state);
        if (manageOrdersAction != null) {
            logger.info("[TWAPStrategy] Enforcing active order limit by canceling oldest order.");
            return manageOrdersAction;
        }

        double bidTwap = OrderHelper.calculateBidTWAP(state);
        double askTwap = OrderHelper.calculateAskTWAP(state);

        // Buy logic
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS && !localBidLevels.isEmpty()) {
            BidLevel bestBid = localBidLevels.get(0);
            long bidPrice = bestBid.price;
            long bidQuantity = bestBid.quantity;

            logger.info("[TWAPStrategy] Checking buy logic: Bid Price = " + bidPrice + ", Bid TWAP = " + bidTwap);
            if (bidPrice <= bidTwap + tolerance) {
                logger.info("[TWAPStrategy] Placing buy order within tolerance of Bid TWAP: " + bidTwap + " at price: " + bidPrice);
                Action action = new CreateChildOrder(Side.BUY, bidQuantity, bidPrice);

                // Accumulate the buy total for profit calculation
                buyTotal += bidPrice * bidQuantity;

                // Remove or adjust the local bid levels after buy
                OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
                logger.info("[TWAPStrategy] Updated local order book state after buy:\n" + 
                    (localBidLevels.isEmpty() ? "|----BID-----|  No active bids left\n" : OrderHelper.formatOrderBook(localBidLevels, localAskLevels)));

                return action;
            }
        }

        // Sell logic
        if (state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS && !localAskLevels.isEmpty()) {
            AskLevel bestAsk = localAskLevels.get(0);
            long askPrice = bestAsk.price;
            long askQuantity = bestAsk.quantity;

            logger.info("[TWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask TWAP = " + askTwap);
            if (askPrice >= askTwap) {
                logger.info("[TWAPStrategy] Placing sell order above Ask TWAP: " + askTwap + " at price: " + askPrice);
                Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                // Accumulate the sell total for profit calculation
                sellTotal += askPrice * askQuantity;

                // Remove or adjust the local ask levels after sell
                OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                logger.info("[TWAPStrategy] Updated local order book state after sell:\n" +
                    (localAskLevels.isEmpty() ? "|----ASK-----|  No active asks left\n" : OrderHelper.formatOrderBook(localBidLevels, localAskLevels)));

                return action;
            }
        }

        // Calculate profit after each iteration
        OrderHelper.calculateProfit(buyTotal, sellTotal);

        logger.info("[TWAPStrategy] No action required, done for now.");
        return NoAction.NoAction;
    }
}

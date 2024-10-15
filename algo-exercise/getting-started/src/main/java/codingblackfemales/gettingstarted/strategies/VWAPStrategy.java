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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 30;

    private List<BidLevel> localBidLevels = new ArrayList<>();
    private List<AskLevel> localAskLevels = new ArrayList<>();

    private double buyTotal = 0;
    private double sellTotal = 0;

    @Override
    public Action execute(SimpleAlgoState state) {
        // Step 1: Populate order book if local lists are empty
        if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
            OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);

            // Remove null entries if present
            localBidLevels.removeIf(bidLevel -> bidLevel == null);
            localAskLevels.removeIf(askLevel -> askLevel == null);

            if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
                logger.info("[VWAPStrategy] Order book is empty after refresh. No action will be taken.");
                return NoAction.NoAction;
            }
        }


        // Step 2: Sort bid and ask levels to maintain time-price priority
        OrderHelper.sortOrderBook(localBidLevels, localAskLevels);
        

        double bidVwap = OrderHelper.calculateBidVWAP(state);
        double askVwap = OrderHelper.calculateAskVWAP(state);

        // Step 3: Buy logic
        if (!localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (BidLevel bidLevel : localBidLevels) {
                if (bidLevel == null) continue;
                long price = bidLevel.price;
                long bidQuantity = bidLevel.quantity;

                logger.info("[VWAPStrategy] Checking buy logic: Bid Price = " + price + ", Bid VWAP = " + bidVwap);
                
                if (price > bidVwap) {
                    logger.info("[VWAPStrategy] Price too high; checking the next bid level for possible buy...");
                    continue;
                }
                
                if (price <= bidVwap) {
                    logger.info("[VWAPStrategy] Placing buy order at price: " + price);
                    Action action = new CreateChildOrder(Side.BUY, bidQuantity, price);

                    buyTotal += price * bidQuantity;
                    OrderHelper.updateBidLevels(localBidLevels, price, bidQuantity);
                    logger.info("[VWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }
            }
            logger.info("[VWAPStrategy] No bid level meets VWAP condition for buy.");
        }

        // Step 4: Sell logic
        if (!localAskLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (AskLevel askLevel : localAskLevels) {
                if (askLevel == null) continue;
                long askPrice = askLevel.price;
                long askQuantity = askLevel.quantity;

                logger.info("[VWAPStrategy] Checking sell logic: Ask Price = " + askPrice + ", Ask VWAP = " + askVwap);
                
                if (askPrice < askVwap) {
                    logger.info("[VWAPStrategy] Price too low; checking the next ask level for possible sell...");
                    continue;
                }
                
                if (askPrice >= askVwap) {
                    logger.info("[VWAPStrategy] Placing sell order at price: " + askPrice);
                    Action action = new CreateChildOrder(Side.SELL, askQuantity, askPrice);

                    sellTotal += askPrice * askQuantity;
                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    return action;
                }
            }
            logger.info("[VWAPStrategy] No ask level meets VWAP condition for sell.");

        }

  // Clear lists to avoid reuse of stale data in the next call
        localBidLevels.clear();
        localAskLevels.clear();

        // Step 5: Final profit calculation and clearing lists
        OrderHelper.calculateProfit(buyTotal, sellTotal);
        logger.info("[VWAPStrategy] No action required, done for now.");

      
        return NoAction.NoAction;
    }
}

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

public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private static final long MAX_ACTIVE_ORDERS = 10;
    private static final double BUY_BUDGET = 150000;

    private List<BidLevel> localBidLevels = new ArrayList<>();
    private List<AskLevel> localAskLevels = new ArrayList<>();

    private double buyTotal = 0;
    private double sellTotal = 0;
    private long totalBoughtQuantity = 0;  // Track total bought quantity
    private long totalSoldQuantity = 0;    // Track total sold quantity


    @Override
    public Action execute(SimpleAlgoState state) {
        //  Populate order book if local lists are empty
        if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
            OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);

            // Remove null entries if present
            localBidLevels.removeIf(bidLevel -> bidLevel == null);
            localAskLevels.removeIf(askLevel -> askLevel == null);
            
        }

        // Sort bid and ask levels to maintain time-price priority
        OrderHelper.sortOrderBook(localBidLevels, localAskLevels);
        

        double bidTwap = OrderHelper.calculateBidTWAP(state);
        double askTwap = OrderHelper.calculateAskTWAP(state);


        // Calculate remaining budget
        double remainingBudget = BUY_BUDGET - buyTotal;
        logger.info("[TWAPStrategy] Remaining Buy Budget: " + remainingBudget);
        logger.info("[TWAPStrategy] Bought Quantity: " + totalBoughtQuantity + ", Sold Quantity: " + totalSoldQuantity);


        // Conditional Buy logic - Only buy if buyTotal is within BUY_BUDGET,askPrice < bidTwap or we are within the stop-loss interval
        if (remainingBudget > 0 && !localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (AskLevel askLevel : localAskLevels) {
                if (askLevel == null) continue;
                long askPrice = askLevel.price;
                long askQuantity = askLevel.quantity;

                logger.info("[TWAPStrategy] Checking buy logic: Ask Price = " + askPrice + ", Bid TWAP = " + bidTwap);
                
                // Check if the ask price is within the stop-loss interval
                if (askPrice > bidTwap && !OrderHelper.isWithinStopLossInterval(bidTwap, askPrice)) {
                    logger.info("[TWAPStrategy] Price too high and not within stop-loss interval; skipping this level...");
                    continue;  // Skip if not within the stop-loss interval
                }
        
                if ( remainingBudget >= (askPrice * askQuantity)) {
                    logger.info("[TWAPStrategy] Placing buy order at price: " + askPrice);
                    Action action = new CreateChildOrder(Side.BUY, askQuantity, askPrice);

                    buyTotal += askPrice * askQuantity;
                    totalBoughtQuantity += askQuantity;
                    remainingBudget = BUY_BUDGET - buyTotal;
                    logger.info("[TWAPStrategy] Remaining Buy Budget after purchase: " + remainingBudget);

                    OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
                    logger.info("[TWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                    OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                    // Call OrderManager to manage filled or excess orders after buy
                    Action manageOrdersAction = OrderManager.manageOrders(state);
                    if (manageOrdersAction != null) {
                        return manageOrdersAction;
                    }
                    return action;
                }
            }
            logger.info("[TWAPStrategy] No bid level meets TWAP condition for buy.");
        }else if (buyTotal >= BUY_BUDGET) {
        logger.info("[TWAPStrategy] Buy budget reached or exceeded. Stopping further buys.");
        }

        // Sell logic
        if (!localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
            for (BidLevel bidLevel : localBidLevels) {
                if (bidLevel == null) continue;
                 long bidPrice = bidLevel.price;
                 long bidQuantity = bidLevel.quantity;

                    // Ensure we only sell what we have bought
                    if (totalSoldQuantity + bidQuantity > totalBoughtQuantity) {
                            bidQuantity = totalBoughtQuantity - totalSoldQuantity;
                        if (bidQuantity <= 0) {
                            logger.info("[TWAPStrategy] No more quantity to sell. Stopping sell.");
                            break;
                        }
                    }

                    // New logic: Check if bought quantity is greater than sold, and proceed with selling
                    if (totalBoughtQuantity > totalSoldQuantity) {
                        logger.info("[TWAPStrategy] Bought quantity is greater than sold quantity. Proceeding with sell even if Ask TWAP is NaN.");

                        // Place a sell order at the current bid price
                        Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

                        sellTotal += bidPrice * bidQuantity;
                        totalSoldQuantity += bidQuantity;  // Track total sold quantity

                        // Update the bid levels after the sell
                        OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
                        logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                        OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                        return action;
                    }

                    // Existing logic for profit target or stop-loss based on Ask TWAP
                    if (bidPrice > askTwap && OrderHelper.isWithinProfitTargetInterval(bidTwap, bidPrice)) {
                        logger.info("[TWAPStrategy] Bid price within profit target range; placing sell order.");
                        Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

                        sellTotal += bidPrice * bidQuantity;
                        totalSoldQuantity += bidQuantity;  // Track total sold quantity

                        OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
                        logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                        OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                        return action;
                    }

                    // Existing stop-loss logic
                    if (bidPrice < bidTwap && OrderHelper.isWithinStopLossInterval(bidTwap, bidPrice)) {
                        logger.info("[TWAPStrategy] Bid price within stop-loss range; placing sell order.");
                        Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

                        sellTotal += bidPrice * bidQuantity;
                        totalSoldQuantity += bidQuantity;  // Track total sold quantity

                        OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
                        logger.info("[TWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

                        OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
                        // Call OrderManager to manage filled or excess orders after buy
                            Action manageOrdersAction = OrderManager.manageOrders(state);
                            if (manageOrdersAction != null) {
                                return manageOrdersAction;
                            }
                        return action;
                    }

                    // Log if the bid price does not meet TWAP or interval conditions
                 logger.info("[TWAPStrategy] Bid price does not meet Ask TWAP, profit target, or stop-loss; checking the next bid level...");
            }
                logger.info("[TWAPStrategy] No bid level meets the conditions for sell.");
        }

                // Step 3: Handle Order Management
                Action manageOrdersAction = OrderManager.manageOrders(state);
                if (manageOrdersAction != null) {
                    logger.info("[TWAPStrategy] Canceling order due to OrderManager condition.");
                    return manageOrdersAction;  // If an order was canceled, return that action
                }

            // OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);
                            

            logger.info("[TWAPStrategy] No action required, done for now.");


            // Step 5: Final profit calculation and clearing lists
            OrderHelper.calculateProfit(buyTotal, sellTotal);


                localBidLevels.clear();
                localAskLevels.clear();
                
                     return NoAction.NoAction;
    }
} 
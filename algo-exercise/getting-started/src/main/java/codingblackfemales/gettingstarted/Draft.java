package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.marketdata.AskLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static codingblackfemales.action.NoAction.NoAction;

public class Draft implements AlgoLogic {

    protected static final Logger logger = LoggerFactory.getLogger(Draft.class);

    protected long MAX_ACTIVE_ORDERS = 3;  // Dynamic threshold in the future

    @Override
    public Action evaluate(SimpleAlgoState state) {
        long maxSize = state.getChildOrders().size();
        BidLevel bestBid = state.getBidAt(0);
        AskLevel bestAsk = state.getAskAt(0);
        long quantity = 100;
        long price = bestBid.price;

        logger.info("[MYALGO] Current order book state:\n" + state);

        // Calculate total filled quantity from all child orders
        long filledQuantity = state.getChildOrders().stream()
            .map(ChildOrder::getFilledQuantity)
            .reduce(0L, Long::sum);  // Summing the filled quantities

        // Dynamically switch between TWAP and VWAP based on market conditions
        String selectedStrategy = selectExecutionStrategy(state);
        logger.info("[MYALGO] Selected strategy: " + selectedStrategy);

        if ("TWAP".equals(selectedStrategy)) {
            return evaluateTWAP(state, quantity, price, filledQuantity);
        } else if ("VWAP".equals(selectedStrategy)) {
            return evaluateVWAP(state, quantity, price, bestAsk.price, filledQuantity);
        }

        // Cancel logic: cancel one order if we have too many active orders
        if (state.getActiveChildOrders().size() >= MAX_ACTIVE_ORDERS) {
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);  // Cancels the first active order
            logger.info("[MYALGO] Cancelling order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);
        }

        return NoAction;
    }

    /**
     * Dynamically selects between TWAP and VWAP based on market conditions.
     */
    protected String selectExecutionStrategy(SimpleAlgoState state) {
        double marketVolatility = calculateMarketVolatility(state);
        double volumeThreshold = 0.05;  // This is just a random threshold. to ajust base on the data we have

        if (marketVolatility > volumeThreshold) {
            return "VWAP";  // High volatility -> use VWAP
        } else {
            return "TWAP";  // Low volatility -> use TWAP
        }
    }

    /**
     * TWAP (Time-Weighted Average Price) Execution Strategy
     */
    public Action evaluateTWAP(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();

        // Calculate TWAP as the average price over time
        double twap = calculateTWAP(state);

        if (activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[TWAP] Adding buy order at price: " + price + ", TWAP: " + twap);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        if (filledQuantity >= quantity) {
            // Add a profit threshold: sell if the price is significantly above TWAP
            if (price >= twap * 1.05) {  // Example: take-profit threshold 5% above TWAP
                logger.info("[TWAP] Selling for profit at price: " + price + " (Take Profit), TWAP: " + twap);
                return new CreateChildOrder(Side.SELL, quantity, price);
            } else {
                logger.info("[TWAP] Price is above TWAP but not profitable enough to sell.");
            }
        }

        // Optional: Add stop-loss logic to prevent further losses
        if (price <= twap * 0.95 && filledQuantity >= quantity) {  // Example: stop-loss threshold 5% below TWAP
            logger.info("[TWAP] Selling to cut losses at price: " + price + " (Stop Loss), TWAP: " + twap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }


        return NoAction;
    }

    /**
     * VWAP (Volume-Weighted Average Price) Execution Strategy
     */
    public Action evaluateVWAP(SimpleAlgoState state, long quantity, long bidPrice, long askPrice, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();

        double vwap = calculateVWAP(state);

        if (bidPrice < vwap && activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[VWAP] Adding buy order below VWAP at price: " + bidPrice + ", VWAP: " + vwap);
            return new CreateChildOrder(Side.BUY, quantity, bidPrice);
        }

            if (askPrice > vwap && filledQuantity >= quantity) {
                // Add a profit threshold: only sell if the price is significantly above VWAP
                if (askPrice >= vwap * 1.05) {  // Example: profit threshold 5% above VWAP
                    logger.info("[VWAP] Selling for profit at price: " + askPrice + " (Take Profit), VWAP: " + vwap);
                    return new CreateChildOrder(Side.SELL, quantity, askPrice);
                } else {
                    logger.info("[VWAP] Price is above VWAP but not profitable enough to sell.");
                }
            }

            // Optional: Add stop-loss logic to prevent further losses
            if (askPrice <= vwap * 0.95 && filledQuantity >= quantity) {  // Example: 5% below VWAP
                logger.info("[VWAP] Selling to cut losses at price: " + askPrice + " (Stop Loss), VWAP: " + vwap);
                return new CreateChildOrder(Side.SELL, quantity, askPrice);
            }


        return NoAction;
    }

    /**
     * Calculate TWAP (Time-Weighted Average Price).
     */
    protected double calculateTWAP(SimpleAlgoState state) {
        List<ChildOrder> allOrders = state.getChildOrders();
        double totalPrice = 0;
        int totalPeriods = allOrders.size();  // Assuming each order represents a period

        for (ChildOrder order : allOrders) {
            totalPrice += order.getPrice();
        }

        return totalPrice / totalPeriods;  // TWAP: Average price over time
    }

    /**
     * Calculate VWAP (Volume-Weighted Average Price) over active orders.
     */
    protected double calculateVWAP(SimpleAlgoState state) {
        List<ChildOrder> activeOrders = state.getActiveChildOrders();
        double totalVolume = 0;
        double totalPriceVolume = 0;

        for (ChildOrder order : activeOrders) {
            totalVolume += order.getQuantity();
            totalPriceVolume += order.getPrice() * order.getQuantity();
        }

        return totalPriceVolume / totalVolume;
    }

    /**
     * Calculate market volatility as an example of a market condition for dynamic strategy selection.
     */
    protected double calculateMarketVolatility(SimpleAlgoState state) {
        double bestBid = state.getBidAt(0).price;
        double bestAsk = state.getAskAt(0).price;

        return Math.abs(bestAsk - bestBid) / bestBid;
    }
}

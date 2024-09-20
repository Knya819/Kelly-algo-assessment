package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.strategies.ExecutionStrategy;
import codingblackfemales.gettingstarted.strategies.TWAPStrategy;
import codingblackfemales.gettingstarted.strategies.VWAPStrategy;
import codingblackfemales.gettingstarted.strategies.ShortfallStrategy;
import codingblackfemales.gettingstarted.strategies.POVStrategy;
import codingblackfemales.gettingstarted.strategies.LiquiditySeekingStrategy;
import codingblackfemales.gettingstarted.strategies.IcebergStrategy;

import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Draft implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(Draft.class);

    // Instantiate all strategies at the same level
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();
    private final ExecutionStrategy isStrategy = new ShortfallStrategy();
    private final ExecutionStrategy povStrategy = new POVStrategy();
    private final ExecutionStrategy liquiditySeekingStrategy = new LiquiditySeekingStrategy();
    private final ExecutionStrategy icebergStrategy = new IcebergStrategy();

    @Override
    public Action evaluate(SimpleAlgoState state) {
        long quantity = 100;
        long price = state.getBidAt(0).price;
        long filledQuantity = calculateFilledQuantity(state);

        // Dynamically select the strategy based on market conditions
        String selectedStrategy = selectExecutionStrategy(state);
        logger.info("[MYALGO] Selected strategy: " + selectedStrategy);

        switch (selectedStrategy) {
            case "TWAP":
                return twapStrategy.execute(state, quantity, price, filledQuantity);
            case "VWAP":
                return vwapStrategy.execute(state, quantity, price, filledQuantity);
            case "IS":
                return isStrategy.execute(state, quantity, price, filledQuantity);
            case "POV":
                return povStrategy.execute(state, quantity, price, filledQuantity);  // Adjusted method call to match the interface
            case "LiquiditySeeking":
                return liquiditySeekingStrategy.execute(state, quantity, price, filledQuantity);
            case "Iceberg":
                return icebergStrategy.execute(state, quantity, price, filledQuantity);  // Adjusted method call to match the interface
            default:
                // If no strategy is selected, cancel any old orders if too many are active
                return cancelOldestOrder(state);
        }
    }

    /**
     * Dynamically selects between TWAP, VWAP, IS, POV, Liquidity Seeking, and Iceberg based on market conditions.
     */
    protected String selectExecutionStrategy(SimpleAlgoState state) {
        double marketVolatility = calculateMarketVolatility(state);
        double volumeThreshold = 0.05;
        long largeOrderSizeThreshold = 1000;  // Example threshold for large orders

        // Select IS if the spread between decision price and current price is large
        if (Math.abs(state.getBidAt(0).price - state.getAskAt(0).price) > 10) {  // Example condition
            return "IS";  // Use Implementation Shortfall if the spread is large
        }

        // Select Liquidity Seeking if there is high liquidity at the best bid/ask
        if (state.getBidAt(0).getQuantity() > 500 || state.getAskAt(0).getQuantity() > 500) {  // Example condition
            return "LiquiditySeeking";  // High liquidity, seek it out
        }

        // Select Iceberg if the order size is large
        if (calculateTotalOrderSize(state) > largeOrderSizeThreshold) {
            return "Iceberg";  // Large order size, hide it with iceberg strategy
        }

        // Default to existing logic based on volatility for TWAP, VWAP, and POV
        if (marketVolatility > volumeThreshold) {
            return "VWAP";  // High volatility -> use VWAP
        } else if (marketVolatility < 0.02) {
            return "POV";  // Low volatility -> use Percent of Volume
        } else {
            return "TWAP";  // Default to TWAP
        }
    }

    // Helper methods for calculation
    private long calculateFilledQuantity(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getFilledQuantity)
            .sum();
    }

    private double calculateMarketVolatility(SimpleAlgoState state) {
        double bestBid = state.getBidAt(0).price;
        double bestAsk = state.getAskAt(0).price;
        return Math.abs(bestAsk - bestBid) / bestBid;
    }

    private long calculateTotalOrderSize(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }

    private Action cancelOldestOrder(SimpleAlgoState state) {
        if (state.getActiveChildOrders().size() > 0) {
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);
            logger.info("[OrderManager] Cancelling order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);
        }
        return null;
    }
}

package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;  
import codingblackfemales.gettingstarted.helpers.OrderManager;
import codingblackfemales.gettingstarted.strategies.ExecutionStrategy;
import codingblackfemales.gettingstarted.strategies.TWAPStrategy;
import codingblackfemales.gettingstarted.strategies.VWAPStrategy;
import codingblackfemales.gettingstarted.strategies.ImplementationShortfallStrategy;
import codingblackfemales.gettingstarted.strategies.POVStrategy;
import codingblackfemales.gettingstarted.strategies.LiquiditySeekingStrategy;
import codingblackfemales.gettingstarted.strategies.IcebergStrategy;

import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * My algo dynamically selects and executes different execution strategies like
 * TWAP, VWAP, IS (Implementation Shortfall), POV (Percentage of Volume), Liquidity Seeking, 
 * and Iceberg based on the current market conditions and the state of the trading algorithm.
 * 
 * These strategies represent different ways to execute large orders without causing excessive market impact. 
 * The idea is to optimise execution based on market dynamics such as price, volume, volatility, and liquidity.
 * 
 * If multiple conditions are met simultaneously, only the first condition that evaluates as true will be executed.
 */

public class Draft implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(Draft.class);

    // Instantiate all strategies at the same level
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();
    private final ExecutionStrategy isStrategy = new ImplementationShortfallStrategy();
    private final ExecutionStrategy povStrategy = new POVStrategy();
    private final ExecutionStrategy liquiditySeekingStrategy = new LiquiditySeekingStrategy();
    private final ExecutionStrategy icebergStrategy = new IcebergStrategy();

    @Override
    public Action evaluate(SimpleAlgoState state) {
        long quantity = 100;
        long price = state.getBidAt(0).price;
        long filledQuantity = OrderHelper.calculateFilledQuantity(state);  
        
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
                return OrderManager.cancelOldestOrder(state);
        }
    }

    /**
     * Dynamically selects between TWAP, VWAP, IS, POV, Liquidity Seeking, and Iceberg based on market conditions.
     */
    protected String selectExecutionStrategy(SimpleAlgoState state) {
        double marketVolatility = OrderHelper.calculateMarketVolatility(state);
        double volumeThreshold = 0.05;
        long largeOrderSizeThreshold = 1000;  // Example threshold for large orders

        // Select IS if the spread between decision price and current price is large
        if (Math.abs(state.getBidAt(0).price - state.getAskAt(0).price) > 10) {  // I'm using a random condition
            return "IS";  // Use Implementation Shortfall if the spread is large
        }

        // Select Liquidity Seeking if there is high liquidity at the best bid/ask
        if (state.getBidAt(0).getQuantity() > 500 || state.getAskAt(0).getQuantity() > 500) {  // Random condition
            return "LiquiditySeeking";  // High liquidity, seek it out
        }

        // Select Iceberg if the order size is large
        if (OrderHelper.calculateTotalOrderSize(state)  > largeOrderSizeThreshold) {
            return "Iceberg";  // Large order size, hide it with iceberg strategy
        }

        // Default to existing logic based on volatility for TWAP, VWAP, and POV
        if (marketVolatility > volumeThreshold) {
            return "VWAP";  // High volatility -> use VWAP
        } else if (marketVolatility < Math.round(volumeThreshold*0.4)) { // 40% of the volumeThreshold
            return "POV";  // Low volatility -> use Percent of Volume
        } else {
            return "TWAP";  // Default to TWAP
        }
    }

}

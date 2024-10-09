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

public class SelectStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SelectStrategy.class);

    // Instantiate all strategies at the same level
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();
    private final ExecutionStrategy isStrategy = new ImplementationShortfallStrategy();
    private final ExecutionStrategy povStrategy = new POVStrategy();
    private final ExecutionStrategy liquiditySeekingStrategy = new LiquiditySeekingStrategy();
    private final ExecutionStrategy icebergStrategy = new IcebergStrategy();

    /**
     * Selects the execution strategy based on market conditions and the current state.
     * 
     * @param state the current market state
     * @return the selected ExecutionStrategy
     */
    public ExecutionStrategy selectExecutionStrategy(SimpleAlgoState state) {
        double marketVolatility = OrderHelper.calculateMarketVolatility(state);
        double volumeThreshold = 0.05;
        long largeOrderSizeThreshold = 1000;  // Example threshold for large orders

        logger.info("[SelectStrategy] Evaluating market conditions for strategy selection...");

        // Select IS if the spread between decision price and current price is large
        if (Math.abs(state.getBidAt(0).price - state.getAskAt(0).price) > 10) {  // Example condition
            logger.info("[SelectStrategy] Selected IS Strategy based on large spread.");
            return isStrategy;  // Use Implementation Shortfall if the spread is large
        }

        // Select Liquidity Seeking if there is high liquidity at the best bid/ask
        if (state.getBidAt(0).getQuantity() > 500 || state.getAskAt(0).getQuantity() > 500) {  // Example condition
            logger.info("[SelectStrategy] Selected Liquidity Seeking Strategy based on high liquidity.");
            return liquiditySeekingStrategy;  // High liquidity, seek it out
        }

        // Select Iceberg if the order size is large
        if (OrderHelper.calculateTotalOrderSize(state) > largeOrderSizeThreshold) {
            logger.info("[SelectStrategy] Selected Iceberg Strategy based on large order size.");
            return icebergStrategy;  // Large order size, hide it with iceberg strategy
        }

        // Default to existing logic based on volatility for TWAP, VWAP, and POV
        if (marketVolatility > volumeThreshold) {
            logger.info("[SelectStrategy] Selected VWAP Strategy based on high market volatility.");
            return vwapStrategy;  // High volatility -> use VWAP
        } else if (marketVolatility < Math.round(volumeThreshold * 0.4)) { // 40% of the volumeThreshold
            logger.info("[SelectStrategy] Selected POV Strategy based on low market volatility.");
            return povStrategy;  // Low volatility -> use Percent of Volume
        } else {
            logger.info("[SelectStrategy] Selected TWAP Strategy by default.");
            return twapStrategy;  // Default to TWAP
        }
    }
}

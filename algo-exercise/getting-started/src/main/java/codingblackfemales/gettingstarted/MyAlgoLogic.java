package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.gettingstarted.strategies.ExecutionStrategy;
import codingblackfemales.gettingstarted.strategies.TWAPStrategy;
import codingblackfemales.gettingstarted.strategies.VWAPStrategy;
import codingblackfemales.gettingstarted.strategies.ImplementationShortfallStrategy;
import codingblackfemales.gettingstarted.strategies.LiquiditySeekingStrategy;
import codingblackfemales.gettingstarted.strategies.IcebergStrategy;
import codingblackfemales.gettingstarted.strategies.POVStrategy;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    // Instantiate strategies with required parameters
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();
    private final ExecutionStrategy isStrategy = new ImplementationShortfallStrategy();
    private final ExecutionStrategy liquiditySeekingStrategy = new LiquiditySeekingStrategy();
    private final ExecutionStrategy icebergStrategy = new IcebergStrategy();
    private final ExecutionStrategy povStrategy = new POVStrategy();

    @Override
    public Action evaluate(SimpleAlgoState state) {
        // Create local copies of bid and ask levels
        List<BidLevel> localBidLevels = new ArrayList<>();
        List<AskLevel> localAskLevels = new ArrayList<>();

        // Retrieve bid levels
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel != null) {
                localBidLevels.add(bidLevel);
            }
        }

        // Retrieve ask levels
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null) {
                localAskLevels.add(askLevel);
            }
        }

        // Select execution strategy based on market conditions
        ExecutionStrategy selectedStrategy = selectExecutionStrategy(state);

        // Execute the selected strategy
        Action action = selectedStrategy.execute(state);

        return action != null ? action : new NoAction(); // Return action or NoAction if null
    }

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
        if (Math.abs(state.getBidAt(0).getPrice() - state.getAskAt(0).getPrice()) > 10) {
            logger.info("[SelectStrategy] Selected IS Strategy based on large spread.");
            return isStrategy;
        }

        // Select Liquidity Seeking if there is high liquidity at the best bid/ask
        if (state.getBidAt(0).getQuantity() > 500 || state.getAskAt(0).getQuantity() > 500) {
            logger.info("[SelectStrategy] Selected Liquidity Seeking Strategy based on high liquidity.");
            return liquiditySeekingStrategy;
        }

        // Select Iceberg if the order size is large
        if (OrderHelper.calculateTotalOrderSize(state) > largeOrderSizeThreshold) {
            logger.info("[SelectStrategy] Selected Iceberg Strategy based on large order size.");
            return icebergStrategy;
        }

        // Default to existing logic based on volatility for TWAP, VWAP, and POV
        if (marketVolatility > volumeThreshold) {
            logger.info("[SelectStrategy] Selected VWAP Strategy based on high market volatility.");
            return vwapStrategy;
        } else if (marketVolatility < Math.round(volumeThreshold * 0.4)) {
            logger.info("[SelectStrategy] Selected POV Strategy based on low market volatility.");
            return povStrategy;
        } else {
            logger.info("[SelectStrategy] Selected TWAP Strategy by default.");
            return twapStrategy;
        }
    }
}

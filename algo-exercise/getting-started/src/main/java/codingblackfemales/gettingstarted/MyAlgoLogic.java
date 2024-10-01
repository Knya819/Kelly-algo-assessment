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
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    // Instantiate both TWAP and VWAP strategies
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();

    @Override
    public Action evaluate(SimpleAlgoState state) {

        // Create local copies of bid and ask levels
        List<BidLevel> localBidLevels = new ArrayList<>();
        List<AskLevel> localAskLevels = new ArrayList<>();

        // Retrieve bid levels
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel != null) {
                localBidLevels.add(bidLevel);  // Add the retrieved bid level to the local list
            }
        }

        // Retrieve ask levels
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null) {
                localAskLevels.add(askLevel);  // Add the retrieved ask level to the local list
            }
        }

        // Determine the market volatility
        double marketVolatility = OrderHelper.calculateMarketVolatility(state);
        double volatilityThreshold = 0.1;  // Example threshold to choose between TWAP and VWAP
        logger.info("[MyAlgoLogic] the marketVolatility is "+  marketVolatility);

        // Choose the strategy based on market conditions
        ExecutionStrategy selectedStrategy;
        if (marketVolatility < volatilityThreshold) { // i put < because of the mismatch between bid and ask. if you fix that change here aswell
            logger.info("[MyAlgoLogic] Market volatility is high, selecting VWAP strategy.");
            selectedStrategy = vwapStrategy;  // High volatility -> VWAP
        } else {
            logger.info("[MyAlgoLogic] Market volatility is low, selecting TWAP strategy.");
            selectedStrategy = twapStrategy;  // Low volatility -> TWAP
        }

        // Execute the selected strategy
        Action action = selectedStrategy.execute(state);

        // // Return the action or NoAction if nothing is returned
        // if (action == null) {
        //     logger.info("[MyAlgoLogic] No action required.");
        //     return new NoAction();  // Or return NoAction.INSTANCE if it's a singleton
        // }

        return action;
    }
}

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
    

    // Instantiate strategies with required parameters
    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();

        // Flag to ensure the market state is logged only once
    private boolean isMarketStateLogged = false;

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
        // Log the market state only once, when the flag is false
        if (!isMarketStateLogged) {
            logger.info("[MyAlgoLogic] Current Market State:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
            isMarketStateLogged = true; // Set the flag to true to prevent further logging
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
        

        logger.info("[SelectStrategy] Evaluating market conditions for strategy selection...");

        if (marketVolatility > volumeThreshold) {
            logger.info("[SelectStrategy] Selected VWAP Strategy based on high market volatility: "+ marketVolatility);
            return vwapStrategy;
        } else {
            logger.info("[SelectStrategy] Selected TWAP Strategy by default.");
            return twapStrategy;
        }
       
    }
}
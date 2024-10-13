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

import java.util.ArrayList;
import java.util.List;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    private final ExecutionStrategy twapStrategy = new TWAPStrategy();
    private final ExecutionStrategy vwapStrategy = new VWAPStrategy();

    private boolean isMarketStateLogged = false;
    private ExecutionStrategy selectedStrategy;

    @Override
    public Action evaluate(SimpleAlgoState state) {
        List<BidLevel> localBidLevels = getLocalBidLevels(state);
        List<AskLevel> localAskLevels = getLocalAskLevels(state);

        // Log the market state and sorted order book only once
        if (!isMarketStateLogged) {
            logger.info("[MyAlgoLogic] Initial Market State:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
            OrderHelper.sortOrderBook(localBidLevels, localAskLevels);
            logger.info("[MyAlgoLogic] Current Market State (sorted by time-price priority) \n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
            isMarketStateLogged = true;
        }

        // Select the execution strategy based on conditions if not already selected
        if (selectedStrategy == null) {
            selectedStrategy = selectExecutionStrategy(state);
        }

        // Execute the selected strategy for each evaluation tick
        return selectedStrategy.execute(state);
    }

    private List<BidLevel> getLocalBidLevels(SimpleAlgoState state) {
        List<BidLevel> bidLevels = new ArrayList<>();
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel != null) {
                bidLevels.add(bidLevel);
            }
        }
        return bidLevels;
    }

    private List<AskLevel> getLocalAskLevels(SimpleAlgoState state) {
        List<AskLevel> askLevels = new ArrayList<>();
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null) {
                askLevels.add(askLevel);
            }
        }
        return askLevels;
    }

    private ExecutionStrategy selectExecutionStrategy(SimpleAlgoState state) {
        double marketVolatility = OrderHelper.calculateMarketVolatility(state);
        double volumeThreshold = 0.05;

        logger.info("[SelectStrategy] Evaluating market conditions for strategy selection...");

        if (marketVolatility > volumeThreshold) {
            logger.info("[SelectStrategy] Selected VWAP Strategy due to high market volatility: " + marketVolatility);
            return vwapStrategy;
        } else {
            logger.info("[SelectStrategy] Selected TWAP Strategy as default.");
            return twapStrategy;
        }
    }
}
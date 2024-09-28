package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.SimpleAlgoState;

import java.util.List;

public interface ExecutionStrategy {
    Action execute(SimpleAlgoState state, List<BidLevel> localBidLevels, List<AskLevel> localAskLevels);
}

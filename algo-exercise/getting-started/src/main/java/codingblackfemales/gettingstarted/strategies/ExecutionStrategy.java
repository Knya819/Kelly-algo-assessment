package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.sotw.SimpleAlgoState;

public interface ExecutionStrategy {
    Action execute(SimpleAlgoState state);
}

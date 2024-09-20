package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;


public interface ExecutionStrategy {
    Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity);
}

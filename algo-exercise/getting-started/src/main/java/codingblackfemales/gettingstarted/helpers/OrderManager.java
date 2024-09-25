package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderManager {

    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);

    public static Action cancelOldestOrder(SimpleAlgoState state) {
        if (state.getActiveChildOrders().size() >= 3) { // use % of calculateTotalVolume next, see what happens
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);  
            logger.info("[OrderManager] Cancelling order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);
        }
        return null;
    }
}
// Cancel the first active order: Not sure if is better to cancel  the first or the less profitable one
package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.ChildOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderManager {

    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    public static final long MAX_ACTIVE_ORDERS = 3;

    // Method to handle both fully filled orders and excess active orders
    public static Action manageOrders(SimpleAlgoState state) {
        // Check for fully filled orders
        Action cancelFilledOrder = cancelFilledOrder(state);
        if (cancelFilledOrder != null) {
            return cancelFilledOrder;
        }

        // Check if there are too many active orders
        Action cancelOldest = cancelOldestOrder(state);
        if (cancelOldest != null) {
            return cancelOldest;
        }

        return null;  // No action needed
    }

    public static Action cancelOldestOrder(SimpleAlgoState state) {
        if (state.getActiveChildOrders().size() > MAX_ACTIVE_ORDERS) {
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);
            logger.info("[OrderManager] Cancelling oldest order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);
        }
        return null;
    }

    public static Action cancelFilledOrder(SimpleAlgoState state) {
        for (ChildOrder childOrder : state.getActiveChildOrders()) {
            if (childOrder.getFilledQuantity() >= childOrder.getQuantity()) {
                logger.info("[OrderManager] Cancelling fully filled order: " + childOrder);
                return new CancelChildOrder(childOrder);
            }
        }
        return null;
    }
}

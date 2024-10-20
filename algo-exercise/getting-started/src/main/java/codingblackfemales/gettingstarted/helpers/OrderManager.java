package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.ChildOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderManager {

    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    public static final long MAX_ACTIVE_ORDERS = 10;

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

    // Method to cancel the oldest order if too many are active
    public static Action cancelOldestOrder(SimpleAlgoState state) {
        if (state.getActiveChildOrders().size() > MAX_ACTIVE_ORDERS) {
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);  // Assuming oldest order is at index 0
            logger.info("[OrderManager] Cancelling oldest order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);  // Return a CancelChildOrder action
        }
        return null;
    }

    // Method to cancel orders that are fully filled
    public static Action cancelFilledOrder(SimpleAlgoState state) {
        for (ChildOrder childOrder : state.getActiveChildOrders()) {
            // If the filled quantity is equal to or greater than the total quantity, the order is fully filled
            if (childOrder.getFilledQuantity() >= childOrder.getQuantity()) {
                logger.info("[OrderManager] Cancelling fully filled order: " + childOrder);
                return new CancelChildOrder(childOrder);  // Return a CancelChildOrder action
            }
        }
        return null;
    }
}

package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private final long MAX_ACTIVE_ORDERS = 3;

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();
        double twap = OrderHelper.calculateTWAP(state);

        if (activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[TWAP] Adding buy order at price: " + price + ", TWAP: " + twap);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        if (filledQuantity >= quantity && price >= twap * 1.05) {
            logger.info("[TWAP] Selling for profit at price: " + price + " (Take Profit), TWAP: " + twap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        if (price <= twap * 0.95 && filledQuantity >= quantity) {
            logger.info("[TWAP] Selling to cut losses at price: " + price + " (Stop Loss), TWAP: " + twap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        return NoAction.NoAction;
    }
}

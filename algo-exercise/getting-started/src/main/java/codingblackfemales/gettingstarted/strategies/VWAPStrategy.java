package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private final long MAX_ACTIVE_ORDERS = 3;

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();
        double vwap = OrderHelper.calculateVWAP(state);

        if (price < vwap && activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[VWAP] Adding buy order below VWAP at price: " + price + ", VWAP: " + vwap);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        if (price >= vwap * 1.05 && filledQuantity >= quantity) {
            logger.info("[VWAP] Selling for profit at price: " + price + " (Take Profit), VWAP: " + vwap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        if (price <= vwap * 0.95 && filledQuantity >= quantity) {
            logger.info("[VWAP] Selling to cut losses at price: " + price + " (Stop Loss), VWAP: " + vwap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        return NoAction.NoAction;
    }
}

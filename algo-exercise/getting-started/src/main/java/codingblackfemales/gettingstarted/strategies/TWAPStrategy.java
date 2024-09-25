package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TWAPStrategy.class);
    private final long MAX_ACTIVE_ORDERS = 3; // to be changed to something more dynamic

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();
        double twap = OrderHelper.calculateTWAP(state);

        // Place a BUY order if there are fewer than the maximum allowed active orders
        if (activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[TWAP] Adding buy order at price: " + price + ", TWAP: " + twap);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        // Profit-taking logic: sell if the price is within the profit-taking interval
        if (filledQuantity >= quantity && OrderHelper.isWithinProfitTargetInterval(state, price)) {
            logger.info("[TWAP] Selling for profit at price: " + price + " (Take Profit), TWAP: " + twap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        // Stop-loss logic: sell if the price is within the stop-loss interval
        if (filledQuantity >= quantity && OrderHelper.isWithinStopLossInterval(state, price)) {
            logger.info("[TWAP] Selling to cut losses at price: " + price + " (Stop Loss), TWAP: " + twap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        // No action if none of the conditions are met
        return NoAction.NoAction;
    }
}


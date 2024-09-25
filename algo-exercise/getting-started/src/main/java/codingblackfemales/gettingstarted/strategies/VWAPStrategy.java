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

public class VWAPStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    private final long MAX_ACTIVE_ORDERS = 3;// make it more dynamic in the future

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long activeOrders = state.getActiveChildOrders().size();
        double vwap = OrderHelper.calculateVWAP(state);

        // Buy if price is below VWAP and fewer than the max active orders
        if (price < vwap && activeOrders < MAX_ACTIVE_ORDERS) {
            logger.info("[VWAP] Adding buy order below VWAP at price: " + price + ", VWAP: " + vwap);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        // Profit-taking logic: sell if the price is within the profit-taking interval
        if (filledQuantity >= quantity && OrderHelper.isWithinProfitTargetInterval(state, price)) {
            logger.info("[VWAP] Selling for profit at price: " + price + " (Take Profit), VWAP: " + vwap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        // Stop-loss logic: sell if the price is within the stop-loss interval
        if (filledQuantity >= quantity && OrderHelper.isWithinStopLossInterval(state, price)) {
            logger.info("[VWAP] Selling to cut losses within stop-loss range at price: " + price + " (Stop Loss), VWAP: " + vwap);
            return new CreateChildOrder(Side.SELL, quantity, price);
        }

        // No action if none of the conditions are met
        return NoAction.NoAction;
    }
}

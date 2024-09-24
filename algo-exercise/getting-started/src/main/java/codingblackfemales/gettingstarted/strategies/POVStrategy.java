package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class POVStrategy implements ExecutionStrategy {

    // Initialize logger
    private static final Logger logger = LoggerFactory.getLogger(POVStrategy.class);

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        long totalMarketVolume = OrderHelper.calculateTotalVolume(state);
        long availableVolume = (totalMarketVolume * 5) / 100;  // 5% of market volume

        if (availableVolume > 0) {
            long orderSize = Math.min(availableVolume, quantity);

            // Buy logic based on market volume
            if (filledQuantity < quantity) {
                return new CreateChildOrder(Side.BUY, orderSize, price);
            }

            // Profit-taking logic: sell if the price is within the profit-taking interval
            if (filledQuantity >= quantity && OrderHelper.isWithinProfitTargetInterval(state, price)) {
                logger.info("[POV] Selling for profit at price: " + price + " (Take Profit), based on POV strategy.");
                return new CreateChildOrder(Side.SELL, quantity, price);
            }

            // Stop-loss logic: sell if the price is within the stop-loss interval
            if (filledQuantity >= quantity && OrderHelper.isWithinStopLossInterval(state, price)) {
                logger.info("[POV] Selling to cut losses at price: " + price + " (Stop Loss), based on POV strategy.");
                return new CreateChildOrder(Side.SELL, quantity, price);
            }
        }

        return NoAction.NoAction;
    }
}

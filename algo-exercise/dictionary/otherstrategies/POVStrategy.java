package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.gettingstarted.helpers.OrderHelper;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class POVStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(POVStrategy.class);
    
    private long quantity = 1000;        // Default quantity to trade
    private long price = 100;            // Default price at which to trade
    private long filledQuantity = 0;      // Quantity already filled

    @Override
    public Action execute(SimpleAlgoState state) {
        long totalMarketVolume = OrderHelper.calculateTotalVolume(state);
        long availableVolume = (totalMarketVolume * 5) / 100;  // 5% of market volume

        if (availableVolume > 0) {
            long orderSize = Math.min(availableVolume, quantity - filledQuantity);

            // Buy logic based on market volume
            if (filledQuantity < quantity) {
                logger.info("[POV] Placing buy order of size: " + orderSize);
                filledQuantity += orderSize; // Update filled quantity
                return new CreateChildOrder(Side.BUY, orderSize, price);
            }

            // Profit-taking logic
            if (filledQuantity >= quantity && OrderHelper.isWithinProfitTargetInterval(state, price)) {
                logger.info("[POV] Selling for profit at price: " + price + " (Take Profit).");
                return new CreateChildOrder(Side.SELL, quantity, price);
            }

            // Stop-loss logic
            if (filledQuantity >= quantity && OrderHelper.isWithinStopLossInterval(state, price)) {
                logger.info("[POV] Selling to cut losses at price: " + price + " (Stop Loss).");
                return new CreateChildOrder(Side.SELL, quantity, price);
            }
        }

        return NoAction.NoAction; // Default to no action
    }
}

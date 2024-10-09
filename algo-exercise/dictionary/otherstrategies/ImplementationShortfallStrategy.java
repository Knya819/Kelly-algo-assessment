package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImplementationShortfallStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ImplementationShortfallStrategy.class);
    
    private long quantity = 1000; // Default quantity to trade
    private long price = 0;       // Default price at which to trade
    private long decisionPrice = -1; // Initial decision price

    @Override
    public Action execute(SimpleAlgoState state) {
        if (decisionPrice == -1) {
            decisionPrice = state.getBidAt(0).price;  // Set decision price based on current market state
        }

        double shortfall = Math.abs(price - decisionPrice) / (double) decisionPrice;

        // Adjusting threshold constants for readability
        double FAVORABLE_THRESHOLD = 0.01;
        double UNFAVORABLE_THRESHOLD = 0.05;

        if (shortfall < FAVORABLE_THRESHOLD) {
            logger.info("[IS] Price is favorable, placing aggressive buy order.");
            return new CreateChildOrder(Side.BUY, quantity, price); // Place the buy order
        } else if (shortfall > UNFAVORABLE_THRESHOLD) {
            logger.info("[IS] Price is unfavorable, slowing down.");
            return NoAction.NoAction; // No action if price is too unfavorable
        }

        return NoAction.NoAction; // Default to no action
    }
}

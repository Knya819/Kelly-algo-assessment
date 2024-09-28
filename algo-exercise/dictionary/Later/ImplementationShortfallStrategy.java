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
    private long decisionPrice = -1;

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        if (decisionPrice == -1) {
            decisionPrice = state.getBidAt(0).price;
        }

        double shortfall = Math.abs(price - decisionPrice) / (double) decisionPrice;

        if (shortfall < 0.01) {
            logger.info("[IS] Price is favorable, placing aggressive buy order.");
            return new CreateChildOrder(Side.BUY, quantity, price);
        } else if (shortfall > 0.05) {
            logger.info("[IS] Price is unfavorable, slowing down.");
            return NoAction.NoAction;
        }

        return NoAction.NoAction;
    }
}

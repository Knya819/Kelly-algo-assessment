package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquiditySeekingStrategy implements ExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LiquiditySeekingStrategy.class);

    private long quantity = 1000;  // Default quantity to trade
    private long price = 0;         // Default price at which to trade

    @Override
    public Action execute(SimpleAlgoState state) {
        long liquidityAtBid = state.getBidAt(0).getQuantity();

        if (liquidityAtBid > quantity) {
            logger.info("[Liquidity Seeking] High liquidity at best bid, placing order.");
            return new CreateChildOrder(Side.BUY, quantity, price); // Place the order
        } else {
            logger.info("[Liquidity Seeking] Low liquidity, waiting for better conditions.");
        }

        return NoAction.NoAction; // Default to no action
    }
}

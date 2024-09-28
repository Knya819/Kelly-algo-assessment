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

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        long liquidityAtBid = state.getBidAt(0).getQuantity();

        if (liquidityAtBid > quantity) {
            logger.info("[Liquidity Seeking] High liquidity at best bid, placing order.");
            return new CreateChildOrder(Side.BUY, quantity, price);
        } else {
            logger.info("[Liquidity Seeking] Low liquidity, waiting for better conditions.");
        }

        return NoAction.NoAction;
    }
}

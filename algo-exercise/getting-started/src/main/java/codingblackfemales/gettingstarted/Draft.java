package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySimpleAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MySimpleAlgoLogic.class);

    private long buyPrice;
    private long sellPrice;
    private long quantity = 100;
    private boolean hasBought = false;

    /**
     * This simple Algo waits for the stock to drop below a certain price, 100, to buy,
     * and once the stock price rises 10 units above the buy price, it sells.
     */
    @Override
    public Action evaluate(SimpleAlgoState state) {

        // Get the current price from the order book (best bid)
        var currentPrice = state.getBidAt(0).price;

        logger.info("[MYALGO] Current price is: " + currentPrice);

        // Buy Logic: Buy if we haven't bought yet and the price is low
        if (!hasBought && currentPrice < 100) {
            buyPrice = currentPrice;
            hasBought = true;
            logger.info("[MYALGO] Buying " + quantity + " at price " + buyPrice);
            return new CreateChildOrder(Side.BUY, quantity, buyPrice);
        }

        // Sell Logic: Sell if the price goes higher than our buy price + 10
        if (hasBought && currentPrice > buyPrice + 10) {
            sellPrice = currentPrice;
            hasBought = false;
            logger.info("[MYALGO] Selling " + quantity + " at price " + sellPrice);
            return new CreateChildOrder(Side.SELL, quantity, sellPrice);
        }

        // Hold: No action if we haven't met the buying or selling conditions
        logger.info("[MYALGO] Holding position. Current price: " + currentPrice);
        return Action.noAction();
    }
}

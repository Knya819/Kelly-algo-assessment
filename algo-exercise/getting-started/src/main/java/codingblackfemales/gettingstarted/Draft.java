package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Draft implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(Draft.class);

    private long buyPrice;
    private long sellPrice;
    private boolean hasBought = false;
    private long Quantity = 10;  
    /**
     * For this first attempt i just want to buy and sell in order to have a profit. 
     * Once this code is giving me the result I want I'll extend it. Like finding the price, getting the quantity or else.
     */

    @Override
    public Action evaluate(SimpleAlgoState state) {

        // Get the current bid and ask prices from the order book (this can be find in AbstractAlgoTest)
        var currentBidPrice = state.getBidAt(0).price;
        var currentAskPrice = state.getAskAt(0).price;

        logger.info("[MYALGO] Current bid price is: " + currentBidPrice);
        logger.info("[MYALGO] Current ask price is: " + currentAskPrice);

        // Buying Logic: Buy if we haven't bought yet and the bid price is below 100
        if (!hasBought && currentBidPrice <= 100) {
            buyPrice = currentBidPrice;
            hasBought = true;
            logger.info("[MYALGO] Buying at price " + buyPrice);
            return new CreateChildOrder(Side.BUY, Quantity, buyPrice);  
        }

        // Selling Logic: Sell if the ask price is higher than or equal to our buy price + 10, i want to have a proffit of at least 10
        if (hasBought && currentAskPrice >= buyPrice + 10) {
            sellPrice = currentAskPrice;
            hasBought = false;
            logger.info("[MYALGO] Selling at price " + sellPrice);
            return new CreateChildOrder(Side.SELL, Quantity, sellPrice);  
        }

        // No action if we haven't met the buying or selling conditions (just wait)
        logger.info("[MYALGO] Holding position. Current bid price: " + currentBidPrice + ", ask price: " + currentAskPrice);
        return NoAction.NoAction; 
    }
}

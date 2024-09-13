package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static codingblackfemales.action.NoAction.NoAction;

public class Draft implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(Draft.class);

    private static final long MAX_ACTIVE_ORDERS = 3;// this is something I'll remove to make it more dynamic dipending on the market size

    @Override
    public Action evaluate(SimpleAlgoState state) {

        long maxSize = state.getChildOrders().size();
        BidLevel bestBid = state.getBidAt(0);
        long quantity = 100; // I'll use get quantity or other
        long price = bestBid.price;// this should be getASkat(0)

        logger.info("[MYALGO] Current order book state:\n" + state);

        // Buy logic: create less than the active orders
        if (maxSize < MAX_ACTIVE_ORDERS ) {
            logger.info("[MYALGO] Adding order at price: " + price);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }

        // Sell logic: only when we have 2 active orders in somehow i need to add the filled quantity, ask what is
        if (state.getActiveChildOrders().size()  == quantity * maxSize) {
                if (price > 100 ) {// twap, vwap, traning stock pluss
                return new CreateChildOrder(Side.SELL, quantity * maxSize, price);
                } else return NoAction;
        }

        // Cancel logic: cancel one order if we have too many active orders
        if (state.getActiveChildOrders().size() == MAX_ACTIVE_ORDERS) {
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0);
            logger.info("[MYALGO] Cancelling order: " + orderToCancel);
            return new CancelChildOrder(orderToCancel);
        }

        return NoAction;
    }


    }
/**
 * Buy Logic: You might use the total filled orders to determine if you have accumulated enough 
 * shares to make a profit or to decide whether to enter a new position.
   Sell Logic: The total filled quantity helps in calculating how much of the order needs to
   be sold to achieve the desired profit or minimize losses. 
   */
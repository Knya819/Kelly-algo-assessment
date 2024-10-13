package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.agrona.concurrent.UnsafeBuffer;

import org.junit.Test;

/**
 * This test plugs together all of the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 *
 * If you cross the srpead (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object.
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 *
 */
public class MyAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        UnsafeBuffer tick;

        while ((tick = createTick()) != null) {
            send(tick);  // Send each tick for processing in AlgoLogic
        }

        // After processing all ticks, you may want to output the final state of the order book
       // System.out.println("[MyAlgoBackTest] Final Cumulative Order Book State:\n" + cumulativeOrderBook);
    }
}

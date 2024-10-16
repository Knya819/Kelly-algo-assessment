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
            try {
                System.out.println("Processing tick data: " + tick.toString());  // Log tick data for inspection
                send(tick);  // Send each tick for processing in AlgoLogic
            } catch (NumberFormatException e) {
                System.err.println("[ERROR] NumberFormatException while processing tick: " + tick.toString());
                e.printStackTrace();
                throw new RuntimeException("Failed to process tick: " + tick.toString(), e);  // Provide tick data in the exception
            }
        }
    }

}

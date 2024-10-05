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
 * If you cross the spread (i.e. you BUY an order with a price which is == or > askPrice()) you will match,
 * and receive a fill back into your order from the order book (visible from the algo in the childOrders of the state object).
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 */
public class MyAlgoBackTest extends AbstractAlgoBackTest {

    // This is where you define which algorithm logic to use (your custom logic)
    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();  // Your custom algorithm logic
    }

    // Test method to run the backtest
    @Test
    public void testExampleBackTest() throws Exception {
        // Instead of manually creating market data ticks, use the processMarketData() method.
        // This method reads from your JSON file and applies your algorithm logic.
        processMarketData();

        // After processing market data, get the current state of the algo
        var state = container.getState();

        // Check things like filled quantity, canceled order count, etc.
        // For example:
        // long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        // assertEquals(225, filledQuantity);  // Example assertion

        // Add additional checks/assertions as needed based on your algo logic
    }

    @Override
    public void send(UnsafeBuffer buffer) {
        // This method sends the encoded market data to the container’s MarketDataService
        container.getMarketDataService().onMessage(buffer);  // Pass the data for processing
    }
}

package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.api.MarketDataEncoder;
import codingblackfemales.marketdata.api.MarketDataProvider;
import codingblackfemales.service.MarketDataService;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test plugs together all of the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 */
public class MyAlgoBackTest extends AbstractAlgoBackTest {

    private MarketDataProvider marketDataProvider;
    private MarketDataEncoder encoder;

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        // Initialize the market data provider to read from the JSON file
        marketDataProvider = new SimpleFileMarketDataProvider("src/test/java/codingblackfemales/gettingstarted/marketdatatest.json");
        encoder = new MarketDataEncoder();

        // Retrieve the first tick (market data) from the file
        MarketDataMessage tick = marketDataProvider.poll();

        // Encode the market data message
        UnsafeBuffer encodedTick = encoder.encode(tick);

        // Send the encoded tick to the backtest using onMessage
        send(encodedTick);

        // When: Market data moves towards us, retrieve the next tick
        MarketDataMessage tick2 = marketDataProvider.poll();
        UnsafeBuffer encodedTick2 = encoder.encode(tick2);
        assertEquals(0, container.getState().getChildOrders().size());

        // Simulate the next market data movement
        send(encodedTick2);

        // Then: Retrieve the current state of the algorithm
        var state = container.getState();

        // Example assertions
        // long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        // assertEquals(225, filledQuantity);
    }

    // Override send to match the base class method signature (DirectBuffer) and call onMessage
    @Override
    public void send(DirectBuffer tick) {
        if (tick != null) {
            container.onMessage(tick);  // Call onMessage to process the market data
        }
    }
}
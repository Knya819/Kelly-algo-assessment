package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyAlgoBackTest extends AbstractAlgoBackTest {

    private SimpleFileMarketDataProvider marketDataProvider;

    @Before
    public void setup() {
        // Specify the path to the market data JSON file
        String filePath = "src/test/resources/MarketData/marketdatatest.json"; // Adjust the path to the JSON file

        // Initialize the market data provider
        marketDataProvider = new SimpleFileMarketDataProvider(filePath);

        // Initialize the container by calling getSequencer() 
        // This ensures the test network, order book, and other services are set up
        getSequencer();
    }

    @Test
    public void testBackTestWithMarketData() throws Exception {
        MarketDataMessage marketDataMessage;

        // Poll the market data and apply the algorithm logic for each tick
        while ((marketDataMessage = marketDataProvider.poll()) != null) {
            // Create the tick from the MarketDataMessage
            UnsafeBuffer tick = createTickFromMarketData(marketDataMessage);  // Adjusted method name

            // Send the tick to the backtest
            sendToBackTest(tick);

            // Directly evaluate the algo logic from MyAlgoLogic
            AlgoLogic algoLogic = new MyAlgoLogic();  // Create the algo logic
            algoLogic.evaluate(container.getState());  // Evaluate the logic with the current state
        }

        // Final checks for the algorithm state
        var finalState = container.getState();
        assertEquals(1000, finalState.getChildOrders().size());

        // Other assertions can go here
    }

    // Helper method to send the tick to the backtest
    private void sendToBackTest(UnsafeBuffer tick) {
        if (tick != null) {
            container.onMessage(tick);  // Pass the tick to the container
        }
    }

    @Override
    public AlgoLogic createAlgoLogic() {
        // Use MyAlgoLogic in the backtest
        return new MyAlgoLogic();  // Your algorithm logic here
    }
}

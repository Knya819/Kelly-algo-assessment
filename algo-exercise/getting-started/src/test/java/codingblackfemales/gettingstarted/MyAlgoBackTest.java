package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Before;
import org.junit.Test;

public class MyAlgoBackTest extends AbstractAlgoBackTest {

    private SimpleFileMarketDataProvider marketDataProvider;

    @Override
    public AlgoLogic createAlgoLogic() {
        // Use MyAlgoLogic in the backtest,  Add it to the container for testing
        return new MyAlgoLogic();
    }

    @Before
    public void setup() {
        // Specify the path to the market data JSON file
        String filePath = "src/test/resources/MarketData/marketdatatest.json";

        // Initialize the market data provider
        marketDataProvider = new SimpleFileMarketDataProvider(filePath);

        // Initialize the container by calling getSequencer()
        getSequencer();
    }

   @Test
public void testBackTestWithMarketData() throws Exception {
    MarketDataMessage marketDataMessage;

    // Poll the market data and apply the algorithm logic for each tick
    while ((marketDataMessage = marketDataProvider.poll()) != null) {
        System.out.println("Processing message: " + marketDataMessage.getClass().getSimpleName());

        // Create the tick from the MarketDataMessage
        UnsafeBuffer tick = createTickFromMarketData(marketDataMessage);

        // Send the tick to the backtest
        sendToBackTest(tick);

        // Log the state of the order book before applying the algorithm logic
        int bidLevels = container.getState().getBidLevels();
        int askLevels = container.getState().getAskLevels();

        System.out.println("Bid Levels: " + bidLevels);
        System.out.println("Ask Levels: " + askLevels);

        if (bidLevels == 0 || askLevels == 0) {
            System.out.println("No bid or ask levels available, skipping this tick.");
            continue;  // Skip this tick if no bid or ask levels are available
        }

        // Directly evaluate the algo logic from MyAlgoLogic
        AlgoLogic algoLogic = new MyAlgoLogic();  // Create the algo logic
        algoLogic.evaluate(container.getState());  // Evaluate the logic with the current state
    }

    // Final checks for the algorithm state
    var finalState = container.getState();
    // Perform any final assertions or checks here
}

    // Helper method to send the tick to the backtest
    private void sendToBackTest(UnsafeBuffer tick) {
        if (tick != null) {
            container.onMessage(tick);  // Pass the tick to the container
        }
    }
}

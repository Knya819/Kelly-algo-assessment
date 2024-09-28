package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class MyAlgoLogicTest {

    private MyAlgoLogic algoLogic;
    private SimpleAlgoState mockState;

    @Before
    public void setUp() {
        // Initialize the algorithm logic
        algoLogic = new MyAlgoLogic();

        // Create a mock state with some dummy bid and ask levels for testing
        mockState = new SimpleAlgoState();
        mockState.setBidLevels(createBidLevels());
        mockState.setAskLevels(createAskLevels());

        // Add child orders to the state (to simulate order history for TWAP)
        mockState.setChildOrders(createChildOrders());
    }

    private List<BidLevel> createBidLevels() {
        List<BidLevel> bidLevels = new ArrayList<>();
        bidLevels.add(new BidLevel(100, 10));
        bidLevels.add(new BidLevel(95, 20));
        bidLevels.add(new BidLevel(90, 30));
        return bidLevels;
    }

    private List<AskLevel> createAskLevels() {
        List<AskLevel> askLevels = new ArrayList<>();
        askLevels.add(new AskLevel(105, 10));
        askLevels.add(new AskLevel(110, 20));
        askLevels.add(new AskLevel(115, 30));
        return askLevels;
    }

    private List<ChildOrder> createChildOrders() {
        List<ChildOrder> orders = new ArrayList<>();
        orders.add(new ChildOrder(Side.BUY, 100, 10));  // Buy order
        orders.add(new ChildOrder(Side.SELL, 110, 10)); // Sell order
        return orders;
    }

    @Test
    public void testVWAPStrategySelection_LowVolatility() {
        // Simulate a low-volatility market (volatility below 0.05)
        double lowVolatility = 0.03;
        mockState.setMarketVolatility(lowVolatility); // Set the volatility in the state

        // Run the evaluation
        Action action = algoLogic.evaluate(mockState);

        // Ensure the algorithm selected the VWAP strategy and placed a buy order
        assertTrue(action instanceof CreateChildOrder);
        CreateChildOrder order = (CreateChildOrder) action;
        assertEquals(Side.BUY, order.getSide());
    }

    @Test
    public void testTWAPStrategySelection_HighVolatility() {
        // Simulate a high-volatility market (volatility above 0.05)
        double highVolatility = 0.06;
        mockState.setMarketVolatility(highVolatility); // Set the volatility in the state

        // Run the evaluation
        Action action = algoLogic.evaluate(mockState);

        // Ensure the algorithm selected the TWAP strategy and placed a sell order
        assertTrue(action instanceof CreateChildOrder);
        CreateChildOrder order = (CreateChildOrder) action;
        assertEquals(Side.SELL, order.getSide());
    }

    @Test
    public void testNoActionWhenNoOrdersNeeded() {
        // Simulate a situation where no action is needed (e.g., no levels available)
        mockState.setBidLevels(new ArrayList<>()); // Empty bid levels

        // Run the evaluation
        Action action = algoLogic.evaluate(mockState);

        // Ensure no action is returned
        assertTrue(action instanceof NoAction);
    }
}

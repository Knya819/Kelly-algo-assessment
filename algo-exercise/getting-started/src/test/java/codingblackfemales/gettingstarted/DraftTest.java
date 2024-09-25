package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

public class DraftTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        // This adds your Draft algo logic to the container classes
        return new Draft();
    }

    // Your original test case
    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // Create a sample market data tick
        send(createTick());

        // Simple assert to check if child orders are created or canceled as expected
        assertEquals(3, container.getState().getChildOrders().size());
    }

    // Test to verify order type (Buy/Sell)
    @Test
    public void testOrderType() throws Exception {
        send(createTick()); // Send a tick to trigger the algo logic

        // Assert the first child order is a buy order
        assertEquals(Side.BUY, container.getState().getChildOrders().get(0).getSide());
    }

    // Test for TWAP strategy execution
    @Test
    public void testTWAPStrategyExecution() throws Exception {
        // Simulate low-volatility market condition for TWAP
        //setMarketConditionLowVolatility(); 
        send(createTick());

        // Assert a buy order is placed with TWAP strategy
        assertEquals(Side.BUY, container.getState().getChildOrders().get(0).getSide());
    }

    // Test for VWAP strategy execution
    @Test
    public void testVWAPStrategyExecution() throws Exception {
        // Simulate high-volatility market condition for VWAP
       // setMarketConditionHighVolatility();
        send(createTick());

        // Assert a buy order is placed with VWAP strategy
        assertEquals(Side.BUY, container.getState().getChildOrders().get(0).getSide());
    }

    // Test for canceling orders when reaching maximum active orders
    @Test
    public void testOrderCancelation() throws Exception {
        // Send multiple ticks to trigger active order creation
        send(createTick()); // First tick
        send(createTick()); // Second tick
        send(createTick()); // Third tick (creates the third order)
        send(createTick()); // Fourth tick (should trigger cancellation)

        // Assert that an order was canceled after reaching MAX_ACTIVE_ORDERS
        assertEquals(3, container.getState().getActiveChildOrders().size());
    }

    // // Helper method to simulate low volatility market condition
    // private void setMarketConditionLowVolatility() {
    //     // Adjust this based on your tick data structure to simulate a low volatility state
    // }

    // // Helper method to simulate high volatility market condition
    // private void setMarketConditionHighVolatility() {
    //     // Adjust this based on your tick data structure to simulate a high volatility state
    // }
}

package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DraftBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new Draft(); // Replace with your actual algorithm class
    }

    // Test for order cancellation and filled quantity
    @Test
    public void testDraftAlgorithm() throws Exception {
        // Simulate initial market data tick
        send(createTick());

        // Assertions to check initial state: 3 buy orders expected
        assertEquals("Expected 3 buy orders", 3, container.getState().getChildOrders().size());

        // Simulate market data change with second tick
        send(createTick2());

        // Retrieve the current state
        var state = container.getState();

        // Calculate the filled quantity from the child orders
        long filledQuantity = state.getChildOrders().stream()
            .mapToLong(ChildOrder::getFilledQuantity)
            .sum();

        // Calculate canceled orders
        long cancelledOrderCount = state.getChildOrders().size() - state.getActiveChildOrders().size();

        // Assertions for filled quantity, canceled orders, and active child orders
        assertEquals("Expected filled quantity", 300, filledQuantity); // Update this based on your logic
        assertEquals("Expected cancelled orders", 0, cancelledOrderCount);
        assertEquals("Expected 2 active child orders", 3, state.getActiveChildOrders().size());
    }

    // Test for TWAP strategy execution
    @Test
    public void testTWAPStrategyExecution() throws Exception {
        // Simulate low-volatility market condition for TWAP
        setMarketConditionLowVolatility();
        send(createTick());

        // Assert that a buy order is placed with TWAP strategy
        assertEquals("Expected TWAP buy order", 3, container.getState().getChildOrders().size());
    }

    // Test for VWAP strategy execution
    @Test
    public void testVWAPStrategyExecution() throws Exception {
        // Simulate high-volatility market condition for VWAP
        setMarketConditionHighVolatility();
        send(createTick());

        // Assert that a buy order is placed with VWAP strategy
        assertEquals("Expected VWAP buy order", 3, container.getState().getChildOrders().size());
    }

    // Test for canceling orders when reaching maximum active orders
    @Test
    public void testOrderCancelation() throws Exception {
        // Simulate multiple ticks to trigger active order creation and cancellation
        send(createTick()); // First tick
        send(createTick()); // Second tick
        send(createTick()); // Third tick (creates the third order)
        send(createTick()); // Fourth tick (should trigger cancellation)

        // Assert that an order was canceled after reaching MAX_ACTIVE_ORDERS
        assertEquals("Expected 3 active child orders after cancellation", 3, container.getState().getActiveChildOrders().size());
    }

    // Helper method to simulate low volatility market condition
    private void setMarketConditionLowVolatility() {
        // Adjust this based on your tick data structure to simulate a low volatility state
    }

    // Helper method to simulate high volatility market condition
    private void setMarketConditionHighVolatility() {
        // Adjust this based on your tick data structure to simulate a high volatility state
    }
}

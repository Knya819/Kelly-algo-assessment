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

    @Test
    public void testDraftAlgorithm() throws Exception {
        // Simulate initial market data
        send(createTick());

        // Assertions to check initial state
        assertEquals("Expected 1 buy order", 1, container.getState().getChildOrders().size());

        // Simulate market data change
        send(createTick2());

        // Assertions to check state after market data change
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        long cancelledOrderCount = state.getChildOrders().size() - state.getActiveChildOrders().size();
        
        assertEquals("Expected filled quantity", 100, filledQuantity);
        assertEquals("Expected cancelled orders", 0, cancelledOrderCount);
        assertEquals("Expected 2 active child orders", 1, container.getState().getActiveChildOrders().size());
    }
}

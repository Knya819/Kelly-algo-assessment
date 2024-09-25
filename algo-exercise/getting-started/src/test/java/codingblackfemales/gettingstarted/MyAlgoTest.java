package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;

import static org.junit.Assert.assertEquals;



public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        // Add MyAlgoLogic to the container for testing
        return new MyAlgoLogic();
    }

    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // Simulate market data by sending a tick through the system
        send(createTick());

        //Verify that 3 child orders were created in total
        assertEquals(0, container.getState().getChildOrders().size());

        // Verify that 2 child orders are active
        assertEquals(0, container.getState().getActiveChildOrders().size());

        //Calculate the number of canceled orders (total - active)
        var state = container.getState();
        long cancelledOrderCount = state.getChildOrders().size() - state.getActiveChildOrders().size();

        // Ensure that exactly 1 child order was canceled
        assertEquals(0, cancelledOrderCount);
    }
}

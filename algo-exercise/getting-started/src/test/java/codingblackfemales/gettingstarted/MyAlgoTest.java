package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.gettingstarted.helpers.OrderHelper;  


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

        // Verify that 3 child orders were created in total
        assertEquals(3, container.getState().getChildOrders().size());

        // Simulate additional market data moving towards us
        send(createTick());

        // Then: get the state
        var state = container.getState();

        // Calculate the total filled quantity across all child orders
         long filledQuantity = OrderHelper.calculateFilledQuantity(state);

        // Check that our algo state was updated to reflect fills
        assertEquals(0, filledQuantity);
    }
}


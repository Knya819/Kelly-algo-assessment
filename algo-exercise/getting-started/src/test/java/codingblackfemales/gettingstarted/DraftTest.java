package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.gettingstarted.Draft;
import org.junit.Test;
import static org.junit.Assert.assertEquals; // Importing the correct assertion method

public class DraftTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        // This adds your Draft algo logic to the container classes
        return new Draft();
    }

    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // Create a sample market data tick
        send(createTick());

        // Simple assert to check if child orders are created or canceled as expected
        assertEquals(3, container.getState().getChildOrders().size());
    }
}

package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.gettingstarted.helpers.OrderHelper;  
import messages.order.Side;



import org.junit.Test;

import static org.junit.Assert.assertEquals;



public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        // Process different types of market data ticks
        send(createTick());  // 
        send(createTick1());  // Tick with only bids
        send(createTick2());  // Tick with only asks
        send(createTick3());  // Tick with both bids and asks
        // send(createTick4());  // Random
    // }
}
}
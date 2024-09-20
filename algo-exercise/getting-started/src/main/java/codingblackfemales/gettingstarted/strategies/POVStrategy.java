package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;


public class POVStrategy implements ExecutionStrategy {

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        // Implement the POV strategy logic here
        // Example: use desired percentage of market volume
        long totalMarketVolume = calculateTotalVolume(state);
        long availableVolume = (totalMarketVolume * 5) / 100; // 5% of the market volume

        if (availableVolume > 0) {
            long orderSize = Math.min(availableVolume, quantity);
            return new CreateChildOrder(Side.BUY, orderSize, price); // Assuming this is your action
        }

        return NoAction.NoAction;
    }

    // Helper to calculate total volume
    private long calculateTotalVolume(SimpleAlgoState state) {
        return state.getChildOrders().stream().mapToLong(ChildOrder::getQuantity).sum();
    }
}

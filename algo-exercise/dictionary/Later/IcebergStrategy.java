package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;


public class IcebergStrategy implements ExecutionStrategy {

    @Override
    public Action execute(SimpleAlgoState state, List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        // Iceberg logic: reveal 10% of the total order size
        long remainingQuantity = quantity - filledQuantity;
        
        // Reveal 10% of the total order, but not more than the remaining quantity
        long visibleQuantity = (long) Math.ceil(quantity * 0.10); // Calculate 10% of the total order
        
        if (remainingQuantity > 0) {
            long orderSize = Math.min(visibleQuantity, remainingQuantity);  // Make sure not to exceed the remaining quantity
            return new CreateChildOrder(Side.BUY, orderSize, price); // Create a new child order for the visible portion
        }

        return NoAction.NoAction;  // No action if the order is fully filled
    }
}


package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;


public class IcebergStrategy implements ExecutionStrategy {

    @Override
    public Action execute(SimpleAlgoState state, long quantity, long price, long filledQuantity) {
        // Iceberg logic: reveal only part of the order
        long visibleQuantity = 100; // Example: reveal only 100 at a time
        long remainingQuantity = quantity - filledQuantity;

        if (remainingQuantity > 0) {
            long orderSize = Math.min(visibleQuantity, remainingQuantity);
            return new CreateChildOrder(Side.BUY, orderSize, price); // Assuming this is your action
        }

        return NoAction.NoAction;
    }
}

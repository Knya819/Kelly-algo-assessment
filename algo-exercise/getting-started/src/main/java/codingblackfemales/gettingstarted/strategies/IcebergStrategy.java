package codingblackfemales.gettingstarted.strategies;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.action.CreateChildOrder;
import messages.order.Side;

public class IcebergStrategy implements ExecutionStrategy {

    private long quantity = 100; // Default total order quantity
    private long price = 0;      // Default price at which to place the order
    private long filledQuantity = 0; // Quantity already filled

    @Override
    public Action execute(SimpleAlgoState state) {
        // Iceberg logic: reveal 10% of the total order size
        long remainingQuantity = quantity - filledQuantity;

        // Calculate 10% of the total order, ensuring we reveal at least some
        long visibleQuantity = Math.max(1, (long) Math.ceil(quantity * 0.10)); 
        
        if (remainingQuantity > 0) {
            long orderSize = Math.min(visibleQuantity, remainingQuantity);  // Ensure we don't exceed the remaining quantity
            
            // Update filled quantity based on the executed order
            filledQuantity += orderSize; 
            
            // Pass orderSize as long
            return new CreateChildOrder(Side.BUY, orderSize, price); // Create a new child order for the visible portion
        }

        return NoAction.NoAction;  // No action if the order is fully filled
    }
}

package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;

import java.util.List;

public class OrderHelper {

    public static double calculateTWAP(SimpleAlgoState state) {
        List<ChildOrder> allOrders = state.getChildOrders();
        double totalPrice = 0;
        int totalPeriods = allOrders.size();

        for (ChildOrder order : allOrders) {
            totalPrice += order.getPrice();
        }

        return totalPrice / totalPeriods;  // TWAP: Average price over time
    }

    public static double calculateVWAP(SimpleAlgoState state) {
        List<ChildOrder> activeOrders = state.getActiveChildOrders();
        double totalVolume = 0;
        double totalPriceVolume = 0;

        for (ChildOrder order : activeOrders) {
            totalVolume += order.getQuantity();
            totalPriceVolume += order.getPrice() * order.getQuantity();
        }

        return totalPriceVolume / totalVolume;
    }

    public static long calculateTotalVolume(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }
}

package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;

import java.util.List;

public class OrderHelper {

   // Calculate TWAP: Time-Weighted Average Price
    public static double calculateTWAP(SimpleAlgoState state) {
        List<ChildOrder> allOrders = state.getChildOrders();  // Get a list of all child orders (executed over time)
        double totalPrice = 0;  // This will store the sum of all prices from the orders
        int totalPeriods = allOrders.size();  // The number of orders corresponds to the time periods

        for (ChildOrder order : allOrders) {  // Loop through each order in the list
            totalPrice += order.getPrice();  // Sum up the prices of all orders
        }

        return totalPrice / totalPeriods;  // TWAP: Average price over time periods
    }


        // Calculate VWAP: Volume-Weighted Average Price
    public static double calculateVWAP(SimpleAlgoState state) {
        List<ChildOrder> activeOrders = state.getActiveChildOrders();  // Get a list of all active child orders (trades executed)
        double totalVolume = 0;  // To hold the total volume of trades
        double totalPriceVolume = 0;  // To hold the sum of price * volume (price-volume product)

        for (ChildOrder order : activeOrders) {  // Loop through each active order
            totalVolume += order.getQuantity();  // Sum up the total quantity (volume) traded
            totalPriceVolume += order.getPrice() * order.getQuantity();  // Sum up the price-volume products
        }

        return totalPriceVolume / totalVolume;  // VWAP: Total price-volume product divided by total volume
    }


    // Calculate total market volume from child orders
    public static long calculateTotalVolume(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }

    // Calculate the total filled quantity from child orders
    public static long calculateFilledQuantity(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getFilledQuantity)
            .sum();
    }

    // Calculate market volatility based on the spread between bid and ask prices
    public static double calculateMarketVolatility(SimpleAlgoState state) {
        double bestBid = state.getBidAt(0).price;
        double bestAsk = state.getAskAt(0).price;
        return Math.abs(bestAsk - bestBid) / bestBid;
    }

    // Calculate total order size based on child orders
    public static long calculateTotalOrderSize(SimpleAlgoState state) {// is actually the same as volume I need to think more about it
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }


    // Determine the percentage of market volume to trade based on market volatility
    public static double determineVolumePercentage(SimpleAlgoState state) {
        double marketVolatility = calculateMarketVolatility(state);
        if (marketVolatility > 0.05) {  // Example: Higher percentage in volatile markets
            return 10;  // Trade 10% of the market volume in volatile markets
        }
        return 5;  // Default to 5% of the market volume in calmer markets
    }

    // Calculate profit target with an interval (e.g., between 5% and 7% above VWAP)
    public static boolean isWithinProfitTargetInterval(SimpleAlgoState state, double price) {
        double vwap = calculateVWAP(state);
        double lowerBound = vwap * 1.05;  // 5% above VWAP
        double upperBound = vwap * 1.07;  // 7% above VWAP
        
        // Check if the price is within the interval for taking profit
        return price >= lowerBound && price <= upperBound;
    }

    // Calculate stop-loss threshold with an interval (e.g., between 92% and 95% of VWAP)
    public static boolean isWithinStopLossInterval(SimpleAlgoState state, double price) {
        double vwap = calculateVWAP(state);
        double lowerBound = vwap * 0.92;  // 92% of VWAP
        double upperBound = vwap * 0.95;  // 95% of VWAP
        
        // Check if the price is within the interval for cutting losses
        return price >= lowerBound && price <= upperBound;
    }
}

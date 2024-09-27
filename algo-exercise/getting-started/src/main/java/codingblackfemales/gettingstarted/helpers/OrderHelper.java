package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.marketdata.AskLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderHelper {

    private static final Logger logger = LoggerFactory.getLogger(OrderHelper.class);

    // Calculate TWAP: Time-Weighted Average Price using the last 100 child orders for Bids
    public static double calculateBidTWAP(SimpleAlgoState state) {
        int i = 0;
        double totalPrice = 0;
        int count = 0;

        while (i < state.getBidLevels() && count < 100) {
            BidLevel bidLevel = state.getBidAt(i);

            if (bidLevel == null) {
                break;
            }

            totalPrice += bidLevel.price;
            count++;
            i++;
        }

        return count > 0 ? totalPrice / count : 0;
    }

    // Calculate TWAP for Asks using the last 100 ask orders
    public static double calculateAskTWAP(SimpleAlgoState state) {
        int i = 0;
        double totalPrice = 0;
        int count = 0;

        while (i < state.getAskLevels() && count < 100) {
            AskLevel askLevel = state.getAskAt(i);

            if (askLevel == null) {
                break;
            }

            totalPrice += askLevel.price;
            count++;
            i++;
        }

        return count > 0 ? totalPrice / count : 0;
    }

    // Calculate the overall TWAP as an average of BidTWAP and AskTWAP
    public static double calculateTWAP(SimpleAlgoState state) {
        double bidTWAP = calculateBidTWAP(state);
        double askTWAP = calculateAskTWAP(state);

        return (bidTWAP + askTWAP) / 2;
    }

    // Calculate VWAP: Volume-Weighted Average Price for Bids
    public static double calculateBidVWAP(SimpleAlgoState state) {
        int i = 0;
        double totalVolume = 0;
        double totalPriceVolume = 0;

        while (true) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel == null) {
                break;
            }

            long quantity = bidLevel.quantity;
            double price = bidLevel.price;

            totalVolume += quantity;
            totalPriceVolume += price * quantity;
            i++;
        }

        if (totalVolume == 0) {
            logger.warn("[VWAP] Total bid volume is zero. Returning best bid price.");
            return state.getBidAt(0).price;
        }

        return totalPriceVolume / totalVolume;
    }

    // Calculate VWAP: Volume-Weighted Average Price for Asks
    public static double calculateAskVWAP(SimpleAlgoState state) {
        int i = 0;
        double totalVolume = 0;
        double totalPriceVolume = 0;

        while (true) {
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel == null) {
                break;
            }

            long quantity = askLevel.quantity;
            double price = askLevel.price;

            totalVolume += quantity;
            totalPriceVolume += price * quantity;
            i++;
        }

        if (totalVolume == 0) {
            logger.warn("[VWAP] Total ask volume is zero. Returning best ask price.");
            return state.getAskAt(0).price;
        }

        return totalPriceVolume / totalVolume;
    }

    // Calculate the overall VWAP as an average of BidVWAP and AskVWAP
    public static double calculateVWAP(SimpleAlgoState state) {
        double bidVWAP = calculateBidVWAP(state);
        double askVWAP = calculateAskVWAP(state);

        return (bidVWAP + askVWAP) / 2;
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
    public static long calculateTotalOrderSize(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }

    // Determine the percentage of market volume to trade based on market volatility
    public static double determineVolumePercentage(SimpleAlgoState state) {
        double marketVolatility = calculateMarketVolatility(state);
        if (marketVolatility > 0.05) {
            return 10;  // Trade 10% of the market volume in volatile markets
        }
        return 5;  // Default to 5% of the market volume in calmer markets
    }

    // Calculate profit target with an interval (e.g., between 5% and 7% above VWAP)
    public static boolean isWithinProfitTargetInterval(SimpleAlgoState state, double price) {
        double vwap = calculateAskVWAP(state);
        double lowerBound = vwap * 1.05;
        double upperBound = vwap * 1.07;

        return price >= lowerBound && price <= upperBound;
    }

    // Calculate stop-loss threshold with an interval (e.g., between 92% and 95% of VWAP)
    public static boolean isWithinStopLossInterval(SimpleAlgoState state, double price) {
        double vwap = calculateAskVWAP(state);
        double lowerBound = vwap * 0.92;
        double upperBound = vwap * 0.95;

        return price >= lowerBound && price <= upperBound;
    }
}

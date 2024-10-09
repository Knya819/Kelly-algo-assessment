package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.marketdata.AskLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


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

    // Calculate market volatility based on the spread between all bid and ask prices
   public static double calculateMarketVolatility(SimpleAlgoState state) {
    int bidSize = state.getBidLevels();
    int askSize = state.getAskLevels();

    // Return default volatility if only bids or only asks are available
    if (bidSize == 0 || askSize == 0) {
        logger.info("[OrderHelper] Market volatility calculation incomplete; using default volatility: 0.01");
        return 0.01;
    }

    double totalSpread = 0.0;
    int count = 0;

    // Determine how many levels to compare (use the smaller of the two)
    int levelsToCompare = Math.min(bidSize, askSize);

    // Iterate through each bid and ask level, calculating the spread
    for (int i = 0; i < levelsToCompare; i++) {
        BidLevel bidLevel = state.getBidAt(i);
        AskLevel askLevel = state.getAskAt(i);

        if (bidLevel == null || askLevel == null) {
            break; // Exit if we run out of valid levels to compare
        }

        double bidPrice = bidLevel.price;
        double askPrice = askLevel.price;

        // Avoid division by zero
        if (bidPrice == 0) {
            throw new ArithmeticException("Bid price is zero at level " + i);
        }

        // Calculate the spread between the current bid and ask level
        double spread = Math.abs(askPrice - bidPrice) / bidPrice;
        totalSpread += spread;
        count++;
    }

    // Return the average spread as a measure of market volatility
    return count > 0 ? totalSpread / count : 0.01;  // Default to 0.1 if no levels to compare
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

public static void calculateProfit(double buyTotal, double sellTotal) {
    if (buyTotal > 0 || sellTotal > 0) {
        double profit = sellTotal - buyTotal;

        // ANSI color codes for green (profit), red (loss), and bold
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_BOLD = "\u001B[1m";
        String ANSI_RESET = "\u001B[0m";

        if (profit > 0) {
            logger.info("[ORDERHELPER]" + ANSI_BOLD + ANSI_GREEN + " Total Profit" + ANSI_RESET + " from the trades: " + sellTotal + " - " + buyTotal + " = "  
                        + ANSI_BOLD + ANSI_GREEN + profit + ANSI_RESET);
        } else if (profit < 0) {
            logger.info("[ORDERHELPER] " + ANSI_BOLD + ANSI_RED  + "Total Loss" + ANSI_RESET + " from the trades: " + sellTotal + " - " + buyTotal + " = " 
                        + ANSI_BOLD + ANSI_RED  + profit + ANSI_RESET);
        } else {
            logger.info("[ORDERHELPER] Total Profit from the trades: " + sellTotal + " - " + buyTotal + " = " + profit); // No color for zero profit
        }
    } else {
        logger.info("[ORDERHELPER] No trades were executed, no profit calculation possible");
    }
}


    // Method to update bid levels by reducing quantity and removing fully filled levels
    public static void updateBidLevels(List<BidLevel> bidLevels, long price, long filledQuantity) {
        for (int i = 0; i < bidLevels.size(); i++) {
            BidLevel bidLevel = bidLevels.get(i);
            if (bidLevel.price == price) {
                long remainingQuantity = bidLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[ORDERHELPER] Removing bid level at price: " + price + " as quantity " + filledQuantity + " is fully bought");
                    bidLevels.remove(i);  // Remove from the local copy
                } else {
                    bidLevel.quantity = remainingQuantity;  // Update the quantity locally
                    logger.info("[ORDERHELPER] Updated bid level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;  // Exit the loop once the level is updated or removed
            }
        }
    }

    // Method to update ask levels by reducing quantity and removing fully filled levels
    public static void updateAskLevels(List<AskLevel> askLevels, long price, long filledQuantity) {
        for (int i = 0; i < askLevels.size(); i++) {
            AskLevel askLevel = askLevels.get(i);
            if (askLevel.price == price) {
                long remainingQuantity = askLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[ORDERHELPER] Removing ask level at price: " + price + " as quantity " + filledQuantity + " is fully sold.");
                    askLevels.remove(i);  // Remove from the local copy
                } else {
                    askLevel.quantity = remainingQuantity;  // Update the quantity locally
                    logger.info("[ORDERHELPER] Updated ask level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;  // Exit the loop once the level is updated or removed
            }
        }
    }

   // Utility method to format the order book for better logging
        public static String formatOrderBook(List<BidLevel> bidLevels, List<AskLevel> askLevels) {
            StringBuilder sb = new StringBuilder();

            // Check if we have only bids or only asks
            boolean onlyBids = !bidLevels.isEmpty() && askLevels.isEmpty();
            boolean onlyAsks = !askLevels.isEmpty() && bidLevels.isEmpty();

            // Header
            if (onlyBids) {
                sb.append(String.format("%-15s\n", "|----BID-----|"));
            } else if (onlyAsks) {
                sb.append(String.format("%-15s\n", "|----ASK-----|"));
            } else {
                sb.append(String.format("%-15s %-15s\n", "|----BID-----|", "|----ASK-----|"));
            }

            // Determine the maximum number of levels to display
            int maxLevels = Math.max(bidLevels.size(), askLevels.size());

            // Loop through the levels
            for (int i = 0; i < maxLevels; i++) {
                String bidStr = i < bidLevels.size() 
                    ? String.format("%5d @ %4d", bidLevels.get(i).price, bidLevels.get(i).quantity)
                    : "       -       ";  // Empty space if there are no more bid levels

                String askStr = i < askLevels.size() 
                    ? String.format("%5d @ %4d", askLevels.get(i).price, askLevels.get(i).quantity)
                    : "       -       ";  // Empty space if there are no more ask levels

                // Append bid and ask levels based on what is available
                if (onlyBids) {
                    sb.append(String.format("%-15s\n", bidStr));
                } else if (onlyAsks) {
                    sb.append(String.format("%-15s\n", askStr));
                } else {
                    sb.append(String.format("%-15s %-15s\n", bidStr, askStr));
                }
            }

            return sb.toString();
}


}




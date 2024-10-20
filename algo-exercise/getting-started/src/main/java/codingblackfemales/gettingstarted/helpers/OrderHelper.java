package codingblackfemales.gettingstarted.helpers;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.marketdata.AskLevel;
import messages.order.Side;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public class OrderHelper {

    private static final Logger logger = LoggerFactory.getLogger(OrderHelper.class);

    // Calculate TWAP: Time-Weighted Average Price using the last 10 child orders for Bids
    public static double calculateBidTWAP(SimpleAlgoState state) {
        int i = 0;
        double totalPrice = 0;
        int count = 0;

        while (i < state.getBidLevels() && count < 10) {
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

    // Calculate TWAP for Asks using the last 10 ask orders
    public static double calculateAskTWAP(SimpleAlgoState state) {
        int i = 0;
        double totalPrice = 0;
        int count = 0;

        while (i < state.getAskLevels() && count < 10) {
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
        // Check if there are any valid bid levels
        if (state.getBidLevels() == 0 || state.getBidAt(0) == null) {
            logger.warn("[VWAP] No bid levels available. Cannot calculate Bid VWAP.");
            return Double.NaN; // Or return a suitable default value
        }

        int i = 0;
        double totalVolume = 0;
        double totalPriceVolume = 0;


        while (i < state.getBidLevels()) {
        
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
            return state.getBidAt(0).price; // Return best bid if no volume exists
        }

        return totalPriceVolume / totalVolume;
    }

    // Calculate VWAP: Volume-Weighted Average Price for Asks
    public static double calculateAskVWAP(SimpleAlgoState state) {
        // Check if there are any valid ask levels
        if (state.getAskLevels() == 0 || state.getAskAt(0) == null) {
            logger.warn("[VWAP] No ask levels available. Cannot calculate Ask VWAP.");
            return Double.NaN; // Or return a suitable default value
        }

        int i = 0;
        double totalVolume = 0;
        double totalPriceVolume = 0;

        while (i < state.getBidLevels()) {
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
            return state.getAskAt(0).price; // Return best ask if no volume exists
        }

        return totalPriceVolume / totalVolume;
    }

    // Calculate the overall VWAP as an average of BidVWAP and AskVWAP
    public static double calculateVWAP(SimpleAlgoState state) {
        double bidVWAP = calculateBidVWAP(state);
        double askVWAP = calculateAskVWAP(state);

        return (bidVWAP + askVWAP) / 2;
    }

    // Calculate total market volume from child orders (this is for the POV strategy)
    public static long calculateTotalVolume(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(ChildOrder::getQuantity)
            .sum();
    }

   

    // Calculate market volatility based on the spread between all bid and ask prices
    public static double calculateMarketVolatility(SimpleAlgoState state) {
        int bidSize = state.getBidLevels();
        int askSize = state.getAskLevels();

        // Return default volatility if only bids or only asks are available
        if (bidSize == 0 || askSize == 0) {
            logger.info("[OrderHelper] Market volatility calculation incomplete; using default volatility: 0.01");
            return 0.1;
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
        return count > 0 ? totalSpread / count : 0.1;  // Default to 0.1 if no levels to compare (in this case I use TWAP strategy)
    }



    // Method to retrieve filled quantity based on order side
    public static long getFilledQuantityForOrder(ChildOrder order) {
        long filledQuantity = 0L;
        
        if (order.getSide() == Side.BUY) {
            filledQuantity = Math.min(order.getQuantity(), order.getQuantity()); // Assuming full fill here for demo
        } else if (order.getSide() == Side.SELL) {
            filledQuantity = Math.min(order.getQuantity(), order.getQuantity());
        }
        
        return filledQuantity;
    }



    // Calculate total order size based on child orders
    public static long calculateFilledQuantity(SimpleAlgoState state) {
        return state.getChildOrders().stream()
            .mapToLong(OrderHelper::getFilledQuantityForOrder)
            .sum();
    }

    // Check if price is within profit target interval (e.g., 0.1%-7% above VWAP)
    public static boolean isWithinProfitTargetInterval(double bidVwap, double price) {
        double lowerBound = bidVwap * 1.001;
        double upperBound = bidVwap * 1.07;
        return price >= lowerBound && price <= upperBound;
    }

    // Check if price is within stop-loss interval (80%-99% of VWAP)
    public static boolean isWithinStopLossInterval(double bidVwap, double price) {
        double lowerBound = bidVwap * 0.8;
        double upperBound = bidVwap * 0.999;
        return price >= lowerBound && price <= upperBound;
    }


    // Check if price is within profit target interval (e.g., 0.1%-7% above TWAP)
    public static boolean isWithinProfitTargetIntervalTwap(double bidTwap, double price) {
        double lowerBound = bidTwap * 1.001; // 0.1% above TWAP
        double upperBound = bidTwap * 1.07;  // 7% above TWAP
        return price >= lowerBound && price <= upperBound;
    }

    // Check if price is within stop-loss interval (e.g., 80%-99% of TWAP)
    public static boolean isWithinStopLossIntervalTwap(double bidTwap, double price) {
        double lowerBound = bidTwap * 0.80;  // 92% of TWAP
        double upperBound = bidTwap * 0.999; // 99% of TWAP
        return price >= lowerBound && price <= upperBound;
    }
 //Method to calculate the profit and returen on investement
    public static void calculateProfit(double buyTotal, double sellTotal) {
        if (buyTotal > 0 || sellTotal > 0) {
            double profit = sellTotal - buyTotal;
            double roi = (buyTotal > 0) ? (profit / buyTotal) * 100 : 0; // ROI as percentage

            // ANSI color codes for green (profit), red (loss), and bold
            String ANSI_GREEN = "\u001B[32m";
            String ANSI_RED = "\u001B[31m";
            String ANSI_BOLD = "\u001B[1m";
            String ANSI_RESET = "\u001B[0m";
            
            String profitMessage;

            if (profit > 0) {
                profitMessage = ANSI_BOLD + ANSI_GREEN + " \t Total Profit  " + ANSI_RESET + " from the trades:  " + sellTotal + " - " + buyTotal + " = "  
                                + ANSI_BOLD + ANSI_GREEN + profit + ANSI_RESET
                                + "\n \t" + ANSI_BOLD + ANSI_GREEN + " ROI: " + String.format("%.2f", roi) + "%" + ANSI_RESET;
            } else if (profit < 0) {
                profitMessage = ANSI_BOLD + ANSI_RED + " \t Total Loss  " + ANSI_RESET + " from the trades:  " + sellTotal + " - " + buyTotal + " = " 
                                + ANSI_BOLD + ANSI_RED + profit + ANSI_RESET
                                + "\n \t" + ANSI_BOLD + ANSI_RED + " ROI: " + String.format("%.2f", roi) + "%" + ANSI_RESET;
            } else {
                profitMessage = " Total Profit from the trades:  " + sellTotal + " - " + buyTotal + " = " + profit
                                + "\n \t" + ANSI_BOLD + " ROI: " + String.format("%.2f", roi) + "%" + ANSI_RESET;
            }
        
            logger.info(" \n \n------------------------------------------------------------------------ \n"  
            + profitMessage + "\n------------------------------------------------------------------------ \n");
        
        } else {
            logger.info("\n \n------------------------------------------------------------------------ \n "+
        "[ORDERHELPER]  No trades were executed, no profit calculation possible "+
            " \n ------------------------------------------------------------------------ \n");
        }
    }

    // Method to update bid levels by reducing quantity and removing fully filled levels
    public static void updateBidLevels(List<BidLevel> bidLevels, double price, long filledQuantity) {
        for (int i = 0; i < bidLevels.size(); i++) {
            BidLevel bidLevel = bidLevels.get(i);
            if (bidLevel.price == price) {
                long remainingQuantity = bidLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[ORDERHELPER] Removing bid level at price: " + price + " as quantity " + filledQuantity + " is fully bought");
                    bidLevels.remove(i);
                } else {
                    bidLevel.quantity = remainingQuantity;
                    logger.info("[ORDERHELPER] Updated bid level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;
            }
        }
    }

    // Method to update ask levels by reducing quantity and removing fully filled levels
    public static void updateAskLevels(List<AskLevel> askLevels, double price, long filledQuantity) {
        for (int i = 0; i < askLevels.size(); i++) {
            AskLevel askLevel = askLevels.get(i);
            if (askLevel.price == price) {
                long remainingQuantity = askLevel.quantity - filledQuantity;
                if (remainingQuantity <= 0) {
                    logger.info("[ORDERHELPER] Removing ask level at price: " + price + " as quantity " + filledQuantity + " is fully sold.");
                    askLevels.remove(i);
                } else {
                    askLevel.quantity = remainingQuantity;
                    logger.info("[ORDERHELPER] Updated ask level at price: " + price + ", remaining quantity: " + remainingQuantity);
                }
                break;
            }
        }
    }

    // Utility method to format the order book for better logging (use for the template)
    public static String formatOrderBook(List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        StringBuilder sb = new StringBuilder();

        boolean onlyBids = !bidLevels.isEmpty() && askLevels.isEmpty();
        boolean onlyAsks = !askLevels.isEmpty() && bidLevels.isEmpty();

        if (onlyBids) {
            sb.append(String.format("%-15s\n", "|----BID-----|"));
        } else if (onlyAsks) {
            sb.append(String.format("%-15s\n", "|----ASK-----|"));
        } else {
            sb.append(String.format("%-15s %-15s\n", "|----BID-----|", "|----ASK-----|"));
        }

        int maxLevels = Math.max(bidLevels.size(), askLevels.size());

        for (int i = 0; i < maxLevels; i++) {
            String bidStr = i < bidLevels.size() 
                ? String.format("%5d @ %4d", bidLevels.get(i).price, bidLevels.get(i).quantity)
                : "       -       "; 

            String askStr = i < askLevels.size() 
                ? String.format("%5d @ %4d", askLevels.get(i).price, askLevels.get(i).quantity)
                : "       -       ";

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

    // Static method to log the count of bid and ask levels
    public static void logBidAskLevelCounts(List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        int bidLevelCount = bidLevels.size();
        int askLevelCount = askLevels.size();
        logger.info("[ORDERHELPER] Bid Level Count: " + bidLevelCount + ", Ask Level Count: " + askLevelCount);
    }

 

    public static void populateLocalOrderBook(List<BidLevel> localBidLevels, List<AskLevel> localAskLevels, SimpleAlgoState state) {
        // Add new bid levels from the state if they don't already exist in localBidLevels
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel newBidLevel = state.getBidAt(i);
            if (newBidLevel != null && !localBidLevels.contains(newBidLevel)) { // Avoid duplicates
                localBidLevels.add(newBidLevel);
            }
        }

        // Add new ask levels from the state if they don't already exist in localAskLevels
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel newAskLevel = state.getAskAt(i);
            if (newAskLevel != null && !localAskLevels.contains(newAskLevel)) { // Avoid duplicates
                localAskLevels.add(newAskLevel);
            }
        }

        // Sort the order book after adding new levels
        sortOrderBook(localBidLevels, localAskLevels);
        logger.info("[OrderHelper] Current Market State (sorted by time-price priority):\n" + formatOrderBook(localBidLevels, localAskLevels));

    }

        // Method to sort using the time  price priotity
    public static void sortOrderBook(List<BidLevel> bidLevels, List<AskLevel> askLevels) {
        // Sort bids in descending order (highest price first)
        bidLevels.sort((b1, b2) -> Long.compare(b2.price, b1.price));
        // Sort asks in ascending order (lowest price first)
        askLevels.sort((a1, a2) -> Long.compare(a1.price, a2.price));
    }

        

}
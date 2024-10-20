
// public class VWAPStrategy implements ExecutionStrategy {

//     private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
//     private static final long MAX_ACTIVE_ORDERS = 10;
//     private static final double BUY_BUDGET = 100000;

//     private List<BidLevel> localBidLevels = new ArrayList<>();
//     private List<AskLevel> localAskLevels = new ArrayList<>();

//     private double buyTotal = 0;
//     private double sellTotal = 0;
//     private long totalBoughtQuantity = 0;  // Track total bought quantity
//     private long totalSoldQuantity = 0;    // Track total sold quantity

//     @Override
//     public Action execute(SimpleAlgoState state) {
//         // Step 1: Populate order book if local lists are empty
//         if (localBidLevels.isEmpty() && localAskLevels.isEmpty()) {
//             OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);

//             // Remove null entries if present
//             localBidLevels.removeIf(bidLevel -> bidLevel == null);
//             localAskLevels.removeIf(askLevel -> askLevel == null);
//         }

//         // Step 2: Sort bid and ask levels to maintain time-price priority
//         OrderHelper.sortOrderBook(localBidLevels, localAskLevels);

//         double bidVwap = OrderHelper.calculateBidVWAP(state);
//         double askVwap = OrderHelper.calculateAskVWAP(state);

//    // Calculate and log remaining budget
//     double remainingBudget = BUY_BUDGET - buyTotal;
//     logger.info("[VWAPStrategy] Remaining Buy Budget: " + remainingBudget);
//     logger.info("[VWAPStrategy] Bought Quantity: " + totalBoughtQuantity + ", Sold Quantity: " + totalSoldQuantity);

   

//         // Step 3: Buy logic - Buy on the ask side when the ask price is below the Bid VWAP
//     if (remainingBudget > 0 && !localAskLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
//         for (AskLevel askLevel : localAskLevels) {
//             if (askLevel == null) continue;
//             long askPrice = askLevel.price;
//             long askQuantity = askLevel.quantity;

//             logger.info("[VWAPStrategy] Checking buy logic: Ask Price = " + askPrice + ", Bid VWAP = " + bidVwap);

//             // If ask price is below Bid VWAP, place a buy order
//             if (askPrice < bidVwap && remainingBudget >= (askPrice * askQuantity)) {
//                 logger.info("[VWAPStrategy] Placing buy order at ask price: " + askPrice);
//                 Action action = new CreateChildOrder(Side.BUY, askQuantity, askPrice);

//                 buyTotal += askPrice * askQuantity;
//                 totalBoughtQuantity += askQuantity;
//                 remainingBudget = BUY_BUDGET - buyTotal;
//                 logger.info("[VWAPStrategy] Remaining Buy Budget after purchase: " + remainingBudget);

//                 OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
//                 logger.info("[VWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

//                 OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
//                 return action;
//             }

//             // Handle partial buy if the remaining budget is less than the total price
//             if (askPrice < bidVwap && remainingBudget > 0 && remainingBudget < (askPrice * askQuantity)) {
//                 long partialAskQuantity = (long) (remainingBudget / askPrice); // Calculate the affordable quantity
//                 logger.info("[VWAPStrategy] Partially buying quantity: " + partialAskQuantity + " at ask price: " + askPrice);
//                 buyTotal += askPrice * partialAskQuantity;
//                 totalBoughtQuantity += partialAskQuantity;
//                 remainingBudget = BUY_BUDGET - buyTotal;

//                 // Update ask levels after partial buy
//                 OrderHelper.updateAskLevels(localAskLevels, askPrice, partialAskQuantity);
//                 logger.info("[VWAPStrategy] Updated order book after partial buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

//                 OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
//                 return new CreateChildOrder(Side.BUY, partialAskQuantity, askPrice);
//             }
//         }
//         logger.info("[VWAPStrategy] No ask level meets Bid VWAP condition for buy.");
//     } else if (remainingBudget <= 0) {
//         logger.info("[VWAPStrategy] Buy budget exhausted. Stopping further buys.");
//     }

//         // Step 4: Sell logic - Only sell if there is enough bought quantity to sell
//             if (!localBidLevels.isEmpty() && state.getActiveChildOrders().size() < MAX_ACTIVE_ORDERS) {
//                 for (BidLevel bidLevel : localBidLevels) {
//                     if (bidLevel == null) continue;
//                     long bidPrice = bidLevel.price;
//                     long bidQuantity = bidLevel.quantity;

//                     // Ensure we only sell what we have bought
//                     if (totalSoldQuantity + bidQuantity > totalBoughtQuantity) {
//                         bidQuantity = totalBoughtQuantity - totalSoldQuantity;
//                         if (bidQuantity <= 0) {
//                             logger.info("[VWAPStrategy] No more quantity to sell. Stopping sell.");
//                             break;
//                         }
//                     }

//                     logger.info("[VWAPStrategy] Checking sell logic: Bid Price = " + bidPrice + ", Ask VWAP = " + askVwap);
//                     logger.info("[VWAPStrategy] Bought Quantity: " + totalBoughtQuantity + ", Sold Quantity: " + totalSoldQuantity);

//                     // Check if bid price is above the Ask VWAP (Profit Target)
//                     if (bidPrice > askVwap && OrderHelper.isWithinProfitTargetInterval(bidVwap, bidPrice)) {
//                         logger.info("[VWAPStrategy] Bid price above Ask VWAP and within profit target range; placing sell order.");
//                         Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

//                         sellTotal += bidPrice * bidQuantity;
//                         totalSoldQuantity += bidQuantity;  // Track total sold quantity
//                         OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
//                         logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

//                         OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
//                         return action;
//                     }

//                     // Check if bid price is above the Ask VWAP and within stop-loss range
//                     if (bidPrice > askVwap && OrderHelper.isWithinStopLossInterval(bidVwap, bidPrice)) {
//                         logger.info("[VWAPStrategy] Bid price above Ask VWAP and within stop-loss range; placing sell order.");
//                         Action action = new CreateChildOrder(Side.SELL, bidQuantity, bidPrice);

//                         sellTotal += bidPrice * bidQuantity;
//                         totalSoldQuantity += bidQuantity;  // Track total sold quantity
//                         OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
//                         logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));

//                         OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
//                         return action;
//                     }

//                     if (totalSoldQuantity > totalBoughtQuantity) {
//                         logger.error("[VWAPStrategy] Sold quantity exceeds bought quantity! Investigate further.");
//                         return NoAction.NoAction;
//                     }

//                     // Log if the bid price does not meet Ask VWAP or interval conditions
//                     logger.info("[VWAPStrategy] Bid price does not meet Ask VWAP, profit target, or stop-loss; checking the next bid level...");
//                 }
//                 logger.info("[VWAPStrategy] No bid level meets Ask VWAP condition for sell.");
//             }


//         logger.info("[VWAPStrategy] No action required, done for now.");

//         // Step 5: Final profit calculation
//         OrderHelper.calculateProfit(buyTotal, sellTotal);

//         return NoAction.NoAction;
//     }
// }


# Project Overview

The aim of this project is to develop an algorithmic trading system that executes buy, sell, and cancel actions on child orders with the objective of generating a profit.
# `main` Folder

## Main Class: `MyAlgoLogic`

In the main folder, you will find the `MyAlgoLogic` class, which serves as the primary class of the project. This class is responsible for selecting between the TWAP and VWAP strategies based on market volatility:
- If the volatility is greater than 0.05, the VWAP strategy is selected.
- Otherwise, the TWAP strategy is applied.

This dynamic selection enables the algorithm to adjust to varying market conditions in order to optimise trading performance.

## Subfolders: `strategies` and `helpers`

In addition to the main folder, the project contains two subfolders: `strategies` and `helpers`.

### `strategies` Folder

The `strategies` folder includes the `TWAP`, `VWAP`, and `ExecutionStrategy` classes.

The `ExecutionStrategy` is an interface that defines the core structure for all trading strategies in the project. It contains the method:

```java
Action execute(SimpleAlgoState state);
```
This method is responsible for executing a strategy based on the current market state. This design allows different strategies, such as TWAP and VWAP, to implement their specific logic while following a common interface.

Both TWAP and VWAP are strategies that we have implemented and will discuss in detail later in the README.

### `helpers` Folder

In the `helpers` folder, there are two classes: `OrderHelper` and `OrderManager`.

- **`OrderHelper`** includes various methods that are utilised in the strategies, such as calculating profits, managing execution intervals, handling different price levels, and more. It provides essential functionality that supports the TWAP and VWAP strategies.

- **`OrderManager`** is responsible for managing both fully filled orders and situations where there are too many active orders. It includes the following key methods:

  - `manageOrders(SimpleAlgoState state)`: Handles fully filled orders and ensures that the number of active orders does not exceed the allowed limit.
  - `cancelOldestOrder(SimpleAlgoState state)`: Cancels the oldest active order when there are more than the maximum number of allowed orders.
  - `cancelFilledOrder(SimpleAlgoState state)`: Cancels any orders that have been fully filled.

The `OrderManager` helps maintain order flow efficiency, preventing issues such as having too many active orders or unaddressed filled orders.

# `resources` Folder

At the same level as the main folder, you will find the `resources` folder, which contains market data in JSON format. This data is used during simulations in the backtesting process, allowing the algorithm to be tested against historical market conditions to assess its performance.

# `test` Folder

Finally, we have the `test` folder, which contains four classes dedicated to testing and backtesting the algorithm. These tests are essential for validating the logic of the trading strategies and ensuring the system behaves as expected under different market conditions, as initially outlined.


## How to Run the Code

To run the tests for the project, you can use the following commands:

- To run the `MyAlgoTest`, use:

```bash
./mvnw -Dtest=MyAlgoTest test --projects algo-exercise/getting-started
```
- To run the MyAlgoBackTest, use:
```bash
./mvnw -Dtest=MyAlgoBackTest test --projects algo-exercise/getting-started
```
### Logging for Better Understanding
I have used various logger.info, logger.debug, and logger.warn statements throughout the project to make the execution and understanding of the strategy clear. These logs provide insights into different stages of the algorithm execution, helping to follow the logic step by step.



# Strategy Explanation: TWAP and VWAP

Both the TWAP and VWAP strategies follow the same underlying logic, with the only difference being the prices they use. For simplicity, we will focus on explaining VWAP in detail.

### TWAP (Time Weighted Average Price)

In traditional implementations, there is usually a concept of time involved, where prices are calculated at set intervals, such as every 10 minutes. However, in this case, we have removed the time component and instead simulate TWAP by calculating the moving average every 10 entries in the order book. This allows us to maintain the essence of the strategy without the need for real-time intervals.

### VWAP (Volume Weighted Average Price)

The VWAP strategy is used to guide buy and sell decisions, aiming to buy at lower ask prices and sell at higher bid prices. It dynamically manages market conditions, staying within budget constraints and ensuring efficient order handling. The system is designed to optimise trading performance by executing profitable trades while minimising risks through stop-loss protections.

This logic ensures the trading algorithm remains responsive to market changes while keeping active orders within the allowable limits.


## Order Flow in `VWAPStrategy`

### Order Book Population

The class starts by populating the local bid and ask levels using the `OrderHelper.populateLocalOrderBook` method. It ensures the order book is sorted to maintain time-price priority, which is essential for accurate execution.

### Buy Logic

The algorithm checks if there is remaining budget (`BUY_BUDGET - buyTotal`) and ensures the ask price is below the bid VWAP or within the stop-loss interval before placing a buy order. The buy process is limited by the budget, ensuring the algorithm doesn’t exceed the allowed amount. After each buy, the order book is updated, and the system manages any filled or excess orders through the `OrderManager`.

### Sell Logic

The selling process first ensures that the total quantity sold does not exceed what has been bought. It checks if the bid price is higher than the ask VWAP and within the profit target interval. If so, a sell order is placed. Stop-loss protection is in place if the bid price drops below the VWAP, triggering a sell action if the price is within the stop-loss range. After each sell, the order book is updated, and the `OrderManager` is called to handle any filled or excess orders.

### Order Management

The `OrderManager` ensures that fully filled orders are cancelled and that the number of active orders does not exceed the maximum allowed (`MAX_ACTIVE_ORDERS`).

### Final Steps

After each execution cycle, the strategy logs the updated order book and performs a final profit calculation using the `OrderHelper.calculateProfit` method. The local bid and ask levels are cleared, readying the strategy for the next execution cycle.

# Usage of `OrderHelper` in `VWAPStrategy`

The `OrderHelper` class is essential for various functions within the `VWAPStrategy`. Below is a breakdown of how it is utilised:

## 1. Order Book Management

### Populating the Local Order Book:
The order book is populated using the `populateLocalOrderBook` method, ensuring the local bid and ask levels are updated with the latest market data.

```java
OrderHelper.populateLocalOrderBook(localBidLevels, localAskLevels, state);
```
### Sorting the Order Book:
After populating the order book, sortOrderBook is called to maintain time-price priority.
```java
OrderHelper.sortOrderBook(localBidLevels, localAskLevels);
```



## 2. Price-Calculations:


### Bid VWAP Calculation:

The strategy calculates the Volume Weighted Average Price (VWAP) for the bid side using the calculateBidVWAP method, which informs the buying decisions.
```java
double bidVwap = OrderHelper.calculateBidVWAP(state);
```

## Ask VWAP Calculation:
Similarly, the Ask VWAP is calculated using the `calculateAskVWAP` method to guide selling decisions.

```java
double askVwap = OrderHelper.calculateAskVWAP(state);
```

## 3. Buy Logic Support:

### Stop-Loss Interval Check:
Before placing a buy order, the strategy checks whether the current ask price is within the stop-loss interval using the `isWithinStopLossInterval` method, ensuring buys are only made at reasonable prices.

```java
if (!OrderHelper.isWithinStopLossInterval(bidVwap, askPrice))  
```


### Updating the Ask Levels:

After a buy order is executed, the ask levels in the local order book are updated to reflect the change in available quantities using updateAskLevels.

```java
OrderHelper.updateAskLevels(localAskLevels, askPrice, askQuantity);
```

### Order Book Formatting:
The `formatOrderBook` method is used to log and display the updated state of the order book after a buy action.

``` java
logger.info("[VWAPStrategy] Updated order book after buy:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
```

### Logging Bid/Ask Level Counts:
The strategy logs the number of bid and ask levels in the order book using `logBidAskLevelCounts` to track how the order book evolves after each action.

```java
OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
```

## 4. Sell Logic Support:

### Updating the Bid Levels:
After a sell order is executed, the bid levels in the local order book are updated using `updateBidLevels` to reflect the latest available quantities.

```java
OrderHelper.updateBidLevels(localBidLevels, bidPrice, bidQuantity);
```

### Order Book Formatting:
The updated order book is logged and displayed after a sell action using the `formatOrderBook` method.

```java
logger.info("[VWAPStrategy] Updated order book after sell:\n" + OrderHelper.formatOrderBook(localBidLevels, localAskLevels));
```

### Logging Bid/Ask Level Counts:
Similar to the buy logic, the number of bid and ask levels is logged using `logBidAskLevelCounts` after a sell order is placed.

``` java
OrderHelper.logBidAskLevelCounts(localBidLevels, localAskLevels);
```
## 5. Profit and Stop-Loss Calculations:

### Profit Target Check:
When deciding whether to sell, the strategy checks if the bid price falls within the profit target interval using `isWithinProfitTargetInterval`. This ensures that the strategy captures profits at optimal price points.

``` java

if (OrderHelper.isWithinProfitTargetInterval(bidVwap, bidPrice)) 
```

### Stop-Loss Check:
The strategy also monitors for stop-loss conditions using `isWithinStopLossInterval` on the bid side during sell decisions, allowing it to minimise losses when prices fall below VWAP.

```java
if (OrderHelper.isWithinStopLossInterval(bidVwap, bidPrice)) 
```
### Final Profit Calculation:

Calculating Total Profit:
At the end of the execution, the total profit is calculated using the `calculateProfit` method, which takes into account the total buy and sell amounts.
```java
OrderHelper.calculateProfit(buyTotal, sellTotal);
```
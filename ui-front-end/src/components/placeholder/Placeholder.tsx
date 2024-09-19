import { useState, useEffect } from "react";
import "./Placeholder.css";

// Define the MarketDepthRow type if not imported already
export type MarketDepthRow = {
  level: number;
  bid: number;
  bidQuantity: number;
  offer: number;
  offerQuantity: number;
};

export const Placeholder = ({ data }: { data: MarketDepthRow[] }) => {
  const [displayState, setDisplayState] = useState<"instructions" | "design">(
    "instructions"
  );
  const [previousBidPrices, setPreviousBidPrices] = useState<number[]>([]); // Array to store previous Bid prices
  const [previousAskPrices, setPreviousAskPrices] = useState<number[]>([]); // Array to store previous Ask prices

  // Find the maximum and minimum bid and ask quantities
  const maxBidQuantity = Math.max(...data.map((row) => row.bidQuantity));
  const minBidQuantity = Math.min(...data.map((row) => row.bidQuantity));
  const maxAskQuantity = Math.max(...data.map((row) => row.offerQuantity));
  const minAskQuantity = Math.min(...data.map((row) => row.offerQuantity));

  // Function to calculate the width of the color bar based on the quantity
  const getBarWidth = (quantity: number, maxQuantity: number, minQuantity: number) => {
    const proportion = (quantity - minQuantity) / (maxQuantity - minQuantity); // Scales between 0 and 1
    return 50 + proportion * 50; // Scale between 50% and 100%
  };

  // Update previous bid and ask prices on data change
  useEffect(() => {
    setPreviousBidPrices((prev) => {
      if (prev.length === 0) {
        return data.map((row) => row.bid); // Initial population of previous bid prices
      }
      return prev;
    });

    setPreviousAskPrices((prev) => {
      if (prev.length === 0) {
        return data.map((row) => row.offer); // Initial population of previous ask prices
      }
      return prev;
    });
  }, [data]);

  // Compare current price with previous price to determine the arrow direction
  const getBidPriceArrow = (currentPrice: number, index: number) => {
    const previousPrice = previousBidPrices[index];
    if (currentPrice > previousPrice) {
      return "⬆"; // Up arrow for increase
    } else  {
      return "⬇"; // Down arrow for decrease
    } 
  };

  const getAskPriceArrow = (currentPrice: number, index: number) => {
    const previousPrice = previousAskPrices[index];
    if (currentPrice > previousPrice) {
      return "⬆"; // Up arrow for increase
    } else  {
      return "⬇"; // Down arrow for decrease
    } 
  };

  const toggleDisplayState = () => {
    setDisplayState((prevState) =>
      prevState === "instructions" ? "design" : "instructions"
    );
  };

  const buttonLabel = displayState === "instructions" ? "design" : "instructions";

  return (
    <div className="Placeholder">
      {displayState === "instructions" ? (
        <div className="Placeholder-instructions">
          <p>
            This table will display live market depth data, including bids, asks, and their
            quantities at different price levels. Once the data is ready, it will automatically
            be shown here.
          </p>
        </div>
      ) : (
        <div className="Placeholder-design">
          <table>
            <thead>
              <tr>
                <th></th> {/* Empty cell for Level column */}
                <th colSpan={2} style={{ textAlign: "center" }}>Bid</th> {/* Center-align Bid header */}
                <th colSpan={2} style={{ textAlign: "center" }}>Ask</th> {/* Center-align Ask header */}
              </tr>
              <tr>
                <th></th> {/* Empty header for Level */}
                <th style={{ textAlign: "center" }}>Quantity</th> {/* Center-align Bid Quantity */}
                <th style={{ textAlign: "right" }}>Price</th> {/* Align Bid Price to the right */}
                <th style={{ textAlign: "left" }}>Price</th> {/* Align Ask Price to the left */}
                <th style={{ textAlign: "center" }}>Quantity</th> {/* Center-align Ask Quantity */}
              </tr>
            </thead>
            <tbody>
              {data.map((row: MarketDepthRow, index: number) => (
                <tr key={index}>
                  <td>{row.level}</td> {/* Level data */}
                  <td
                    style={{
                      position: "relative",
                      height: "20px",
                      width: "100px",
                      textAlign: "right", /* Align text to the right for Bid */
                    }}
                  >
                    {/* Blue bar (bid) filling from right to left */}
                    <div
                      style={{
                        position: "absolute",
                        right: 0, /* Start from the right */
                        width: `${getBarWidth(row.bidQuantity, maxBidQuantity, minBidQuantity)}%`,
                        backgroundColor: "rgba(0, 76, 151, 0.85)", /* RAL 5017 */
                        height: "100%",
                      }}
                    ></div>
                    <span style={{ position: "relative", zIndex: 1, color: "white", paddingRight: "2px" }}>
                      {row.bidQuantity}
                    </span> {/* White text aligned right */}
                  </td>
                  <td style={{ textAlign: "right", paddingRight: "8px" }}>
                    {/* Bid Price with dynamic arrow (price aligned right, arrow unchanged) */}
                    <span style={{ paddingRight: "8px", color: "grey" }}>{getBidPriceArrow(row.bid, index)}</span> {row.bid}
                  </td>
                  <td style={{ textAlign: "left", paddingLeft: "8px" }}>
                    {/* Ask Price with dynamic arrow (price aligned left, arrow unchanged) */}
                    {row.offer} <span style={{ paddingLeft: "8px", color: "grey" }}>{getAskPriceArrow(row.offer, index)}</span>
                  </td>
                  <td
                    style={{
                      position: "relative",
                      height: "20px",
                      width: "100px",
                      textAlign: "left", /* Align text to the left for Ask */
                    }}
                  >
                    {/* Red bar (ask) filling from left to right */}
                    <div
                      style={{
                        width: `${getBarWidth(row.offerQuantity, maxAskQuantity, minAskQuantity)}%`,
                        backgroundColor: "rgba(216, 75, 32, 0.85)", /* RAL 2002 */
                        height: "100%",
                      }}
                    ></div>
                    <span style={{ position: "relative", zIndex: 1, color: "white", paddingLeft: "2px" }}>
                      {row.offerQuantity}
                    </span> {/* White text aligned left */}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {/* Add the button for switching */}
      <div className="Placeholder-buttonContainer">
        <button onClick={toggleDisplayState}>
          Click to view {buttonLabel}
        </button>
      </div>
    </div>
  );
};

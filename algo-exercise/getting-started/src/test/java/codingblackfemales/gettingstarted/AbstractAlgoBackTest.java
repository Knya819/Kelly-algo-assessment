package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.api.MarketDataProvider;
import codingblackfemales.marketdata.api.AskBookUpdate;
import codingblackfemales.marketdata.api.BidBookUpdate;
import codingblackfemales.marketdata.api.BookEntry;
import codingblackfemales.marketdata.api.BookUpdate;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import codingblackfemales.orderbook.OrderBook;
import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.consumer.OrderBookInboundOrderConsumer;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import codingblackfemales.marketdata.api.MarketDataEncoder;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Before;
import org.junit.Test;

import messages.marketdata.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {

    protected MarketDataProvider provider;
    protected MarketDataEncoder encoder;
    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        final MarketDataChannel marketDataChannel = new MarketDataChannel(sequencer);
        final OrderChannel orderChannel = new OrderChannel(sequencer);
        final OrderBook book = new OrderBook(marketDataChannel, orderChannel);

        final OrderBookInboundOrderConsumer orderConsumer = new OrderBookInboundOrderConsumer(book);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        container.setLogic(createAlgoLogic());


       
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        // Initialize Market Data Provider to read from your JSON file
        provider = new SimpleFileMarketDataProvider("src/resources/marketdata/marketdatatest.json");
        encoder = new MarketDataEncoder();
        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    public UnsafeBuffer createTick() {
    System.out.println("createTick");
    MarketDataMessage marketDataMessage = provider.poll();
    
    if (marketDataMessage != null) {
        System.out.println(marketDataMessage);
        // Use the retrieved message for encoding
        UnsafeBuffer encoded = encoder.encode(marketDataMessage);
        return encoded;
    }
    return null;
    }


}
    





package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.api.MarketDataEncoder;
import codingblackfemales.marketdata.api.MarketDataProvider;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import codingblackfemales.orderbook.OrderBook;
import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.consumer.OrderBookInboundOrderConsumer;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import org.agrona.concurrent.UnsafeBuffer;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {

    protected AlgoContainer container;
    private final MarketDataEncoder encoder = new MarketDataEncoder();  // Use your encoder
    private MarketDataProvider provider;

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

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        // Initialize Market Data Provider to read from your JSON file
        provider = new SimpleFileMarketDataProvider("src/test/resources/MarketData/marketdatatest.json");

        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    // Method to process market data from the provider
    protected void processMarketData() {
        MarketDataMessage marketDataMessage;
        while ((marketDataMessage = provider.poll()) != null) {
            UnsafeBuffer encoded = encoder.encode(marketDataMessage);
            send(encoded);  // Send encoded message to the container
        }
    }

    // Abstract method that must be implemented by subclasses
    public abstract void send(UnsafeBuffer buffer);
}

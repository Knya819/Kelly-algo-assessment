package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
// import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

import java.nio.ByteBuffer;

public abstract class AbstractAlgoTest extends SequencerTestCase {

    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
     
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        container.setLogic(createAlgoLogic());

        // network.addConsumer(new LoggingConsumer());
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(container);

        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    /**
     * Set the logging level for MarketDataService to control verbosity in tests.
     * @param level Logging level (INFO, DEBUG, etc.)
     */
   

    /**
     * Create a sample market data tick with high volatility bid/ask prices for testing.
     * @return UnsafeBuffer containing encoded market data.
     */
    protected UnsafeBuffer createTick() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Simulate high volatility with large bid/ask spreads
        encoder.bidBookCount(3)  
                .next().price(100L).size(101L)
                .next().price(110L).size(200L)
                .next().price(115L).size(5000L);

        encoder.askBookCount(3)
                .next().price(98L).size(200L)
                .next().price(95L).size(200L)
                .next().price(91L).size(300L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }
}

                 // encoder.askBookCount(3)
        //     .next().price(110L).size(100L)
        //     .next().price(120L).size(200L)
        //     .next().price(130L).size(300L);

        // encoder.bidBookCount(3)
        //     .next().price(98L).size(100L)
        //     .next().price(99L).size(200L)
        //     .next().price(100L).size(5000L);



package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.marketdata.api.BookEntry;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.impl.BookUpdateImpl;
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
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {

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

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    // Add this method to AbstractAlgoBackTest
    protected UnsafeBuffer createTickFromMarketData(MarketDataMessage marketDataMessage) {
        // Assuming the MarketDataMessage contains bid and ask books

        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        if (marketDataMessage instanceof BookUpdateImpl) {
            BookUpdateImpl bookUpdate = (BookUpdateImpl) marketDataMessage;

            // Set venue and instrument ID
            encoder.venue(bookUpdate.venue());
            encoder.instrumentId(bookUpdate.instrumentId());
            encoder.source(Source.STREAM);

            // Encode the bid book
            BookUpdateEncoder.BidBookEncoder bidBookEncoder = encoder.bidBookCount(bookUpdate.bidBook().size());
            for (BookEntry bid : bookUpdate.bidBook()) {
                bidBookEncoder.next().price(bid.price()).size(bid.size());
            }

            // Encode the ask book
            BookUpdateEncoder.AskBookEncoder askBookEncoder = encoder.askBookCount(bookUpdate.askBook().size());
            for (BookEntry ask : bookUpdate.askBook()) {
                askBookEncoder.next().price(ask.price()).size(ask.size());
            }

            // Set the instrument status
            encoder.instrumentStatus(bookUpdate.instrumentStatus());

        } else {
            System.out.println("Unsupported message type: " + marketDataMessage.getClass().getSimpleName());
        }

        return directBuffer;
    }
}
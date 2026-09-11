package mdplayer;

import java.util.concurrent.atomic.AtomicInteger;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import musicDriverInterface.MetaData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.util.event.GenericListener;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AudioGenericListenerTest {

    static class DummyDriver extends BaseDriver {
        public DummyDriver() {
            super(null);
        }

        @Override
        public void init(Common.EnmModel model, int latency, int waitTime, Object... args) {
        }

        @Override
        public void processOneFrame() {
        }

        @Override
        public MetaData retrieveMetaData(byte[] buf, Object... args) {
            return null;
        }
    }

    static class DummyPlugin extends BasePlugin<DummyDriver> {
        DummyDriver driver = new DummyDriver();

        @Override
        public DummyDriver getDriver() {
            return driver;
        }

        @Override
        protected void initChips() {
        }
    }

    @Test
    @DisplayName("listener added dynamically mid-playback receives events from the running driver")
    void testDynamicListenerMidPlayback() {
        Audio audio = Audio.getInstance();
        DummyPlugin plugin = new DummyPlugin();
        audio.plugin = plugin;

        AtomicInteger eventCount = new AtomicInteger();
        GenericListener listener = ev -> {
            if ("master".equals(ev.getName())) {
                eventCount.incrementAndGet();
            }
        };

        // Before adding, firing an event on the driver should not reach listener
        plugin.getDriver().fireEventHappened(audio, "master", new short[10], 0);
        assertEquals(0, eventCount.get());

        // Add listener while plugin/driver is already active
        audio.addGenericListener(listener);

        // Now firing an event on driver should reach listener
        plugin.getDriver().fireEventHappened(audio, "master", new short[10], 0);
        assertEquals(1, eventCount.get());

        // Remove listener
        audio.removeGenericListener(listener);

        // Event after removal should not reach listener
        plugin.getDriver().fireEventHappened(audio, "master", new short[10], 0);
        assertEquals(1, eventCount.get());
    }
}

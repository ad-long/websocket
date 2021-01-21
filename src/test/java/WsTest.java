package huobi;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class WsTest {

    @ParameterizedTest
    @ValueSource(strings = { "FIL" })
    void testNotify(String x) throws URISyntaxException, InterruptedException {
        WsClient ws = new WsClient("x-x-x-x", "x-x-x-4dc5b",
                "api.hbdm.com", "x", true);

        ws.Open("/notification", true);
        Map<String, String> map = new HashMap<String, String>();

        // sub
        map.put("op", "sub");
        map.put("topic", "orders." + x);
        ws.Send(map);

        map.put("topic", "accounts." + x);
        ws.Send(map);

        map.put("topic", "positions." + x);
        ws.Send(map);

        map.put("topic", "matchOrders." + x);
        ws.Send(map);

        map.put("topic", "trigger_order." + x);
        ws.Send(map);

        // sleep
        Thread.sleep(1000 * 60 * 30);
    }

    @ParameterizedTest
    @ValueSource(strings = { "XMR-USDT" })
    void testMarket(String x) throws URISyntaxException, InterruptedException {
        WsClient ws = new WsClient("", "", "api.hbdm.com", "", true);

        ws.Open("/linear-swap-ws", true);
        Map<String, String> map = new HashMap<String, String>();

        // sub
        map.put("sub", "market.XMR-USDT.depth.size_20.high_freq");
        map.put("id", "api");
        map.put("data_type", "incremental");
        ws.Send(map);

        // sleep
        Thread.sleep(1000 * 60 * 30);
    }

}

package huobi;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import huobi.util.ApiSignature;
import huobi.util.ZipUtil;

public class WsClient {

    private final Logger logger = LogManager.getLogger(WsClient.class.getName());

    private String accessKey;
    private String secretKey;
    private String host;
    private String cid;
    private WebSocketClient wsClient;
    private boolean hasAuth;
    private boolean autoReconnect;

    private String path;
    private boolean beAuth;
    private Map<String, String> cmd = new HashMap<String, String>();

    public WsClient(String accessKey, String secretKey, String host, String cid, boolean autoReconnect) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.host = host;
        this.cid = cid;
        this.autoReconnect = autoReconnect;
    }

    public void Open(final String path, boolean beAuth) {
        this.path = path;
        this.beAuth = beAuth;

        String fullPath = "ws://" + host + path;
        logger.info("connect to: " + fullPath);

        try {
            wsClient = new WebSocketClient(new URI(fullPath)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    hasAuth = true;
                    if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
                        hasAuth = false;
                        sendAuth(path);
                    }
                }

                @Override
                public void onMessage(String s) {
                    logger.info("onMessage:%s".formatted(s));
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    try {
                        String message = new String(ZipUtil.decompress(bytes.array()), "UTF-8");

                        JSONObject jdata = JSONObject.parseObject(message);

                        String ping = jdata.getString("ping");
                        if (ping != null && !ping.isEmpty()) {

                            Map<String, String> map = new HashMap<String, String>();
                            map.put("pong", jdata.get("ping").toString());
                            _send(map);
                            return;
                        }

                        String op = jdata.getString("op");
                        if (op != null && !op.isEmpty()) {
                            if (op.equalsIgnoreCase("ping")) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("op", "pong");
                                map.put("ts", jdata.getString("ts"));
                                _send(map);
                                return;
                            }

                            if (op.equalsIgnoreCase("auth")) {
                                Integer errCode = jdata.getInteger("err-code");
                                if (errCode != 0) {
                                    logger.info(message);
                                    return;
                                }
                                hasAuth = true;
                            }
                        }
                        logger.info("recv: " + jdata);

                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.error("onClose code:%d,reason:%s,remote:%s".formatted(code, reason, remote));
                    hasAuth = false;
                    if (autoReconnect) {
                        reconnWs();
                    }
                }

                @Override
                public void onError(Exception e) {
                    logger.error("onError:%s".formatted(e.getMessage()));
                }
            };

            wsClient.connect();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void Send(Map<String, String> map) {
        try {
            while (!hasAuth) {
                Thread.sleep(10);
            }
            logger.info("send: " + _send(map));
            cmd.putAll(map);;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void reconnWs() {
        this.Open(this.path, this.beAuth);
        Send(this.cmd);
    }

    private String _send(Map<String, String> map) {
        String data = JSON.toJSONString(map);
        wsClient.send(data);
        return data;
    }

    private void sendAuth(String path) {
        try {
            Thread.sleep(100);

            Map<String, String> map = new HashMap<String, String>();
            ApiSignature as = new ApiSignature();
            as.createSignature(accessKey, secretKey, "GET", host, path, map);
            map.put("type", "api");
            if (!cid.isEmpty()) {
                map.put("cid", cid);
            }
            map.put("op", "auth");
            logger.info("send: " + _send(map));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}

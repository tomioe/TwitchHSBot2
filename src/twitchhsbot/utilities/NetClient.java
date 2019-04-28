/*
 */

package twitchhsbot.utilities;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import twitchhsbot.TwitchHSBot;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientRequest.RequestType;
import twitchhsbot.structures.NetClientResponse;

public class NetClient {
    
    private static final Logger LOG = TwitchHSBot.LOG;
    private static NetClient inst;
    public static NetClient get() {
        if(inst == null) {
            inst = new NetClient();
        }
        return inst;
    }
    
    
    private final Vertx vertx = VertxFactory.newVertx();
    private final int defaultTimeout = 400; // 400 ms timeout for requests
    
    
    public void getRequest(NetClientRequest ncr, NetClientAccess nca) {
        String address = ncr.getNetAddress();
        String path = ncr.getNetPath();
        int port = ncr.getNetPort();
        HttpClient client = vertx.createHttpClient();
        client.setHost(address)
              .setPort(port)
              .setConnectTimeout(defaultTimeout)
              .exceptionHandler(new CustomExceptionHandler(ncr, nca))
              .getNow(path, new CustomResponseHandler(ncr, nca));
    }
    
    public void postRequest(NetClientRequest ncr, NetClientAccess nca, RequestType reqt) {
        String data = ncr.getNetData();
        String contentType = ncr.getNetContentType();
        String address = ncr.getNetAddress();
        String path = ncr.getNetPath();
        String headers[][] = ncr.getNetHeaders();
        int port = ncr.getNetPort();
        boolean secure = ncr.isSecureConnection();
        
        HttpClient client = vertx.createHttpClient();
        if (secure) {
            client.setSSL(true)
                  .setTrustAll(true);
        }
        client.setHost(address)
                .setPort(port)
                .setConnectTimeout(defaultTimeout)
                .exceptionHandler(new CustomExceptionHandler(ncr, nca));
        HttpClientRequest hcr;
        switch(reqt) {
            default:
            case POST_REQUEST:
                hcr = client.post(path, new CustomResponseHandler(ncr, nca));
                break;
            case PUT_REQUEST:
                hcr = client.put(path, new CustomResponseHandler(ncr, nca));
                break;
        }
        boolean setChunked = true;
        if (headers != null) {    
            for (int i = 0; i < headers.length; i++) {
                String headerKey = headers[i][0];
                if(headerKey.equals("Content-Length")) {
                    setChunked = false;
                }
                hcr.putHeader(headers[i][0], headers[i][1]);
                //System.out.println("Putting header pair '"+headers[i][0] + "': '" + headers[i][1]+"'");
            }
        }
        hcr.setChunked(setChunked);
        if (data != null) {
            hcr.putHeader("Content-Type", contentType);
            hcr.write(data);
//            try {
//                hcr.write(new Buffer(data.getBytes("UTF-8")));
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                
//            }
        }
        hcr.end();
    }
    
    private class CustomResponseHandler implements Handler<HttpClientResponse> {
        private NetClientAccess nca;
        private NetClientRequest ncr;
        private CustomResponseHandler(NetClientRequest ncr, NetClientAccess nca){
            this.nca = nca;
            this.ncr = ncr;
        }
        @Override
        public void handle(HttpClientResponse e) {
            final int responseStatusCode = e.statusCode();
            e.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer e) {
                    NetClientResponse ncresp =
                            new NetClientResponse(true, responseStatusCode, e.toString(), ncr);
                    nca.netResponse(ncresp);
                }
            });
        }
    }
    
    private class CustomExceptionHandler implements Handler<Throwable> {
        private NetClientAccess nca;
        private NetClientRequest ncr;
        private CustomExceptionHandler(NetClientRequest ncr, NetClientAccess nca) {
            this.nca = nca;
            this.ncr = ncr;
        }
        @Override
        public void handle(Throwable e) {
            e.printStackTrace();
            LOG.warning("NetClient threw the following exception: "+e.getClass().getName());
            NetClientResponse ncresp = new NetClientResponse(false, -1, null, ncr);
            nca.netResponse(ncresp);
        }
    }
    

}

# tcptester
A Java framework to work with length prefixed (2I/2E etc) TCP messages. 

Use this library to build your own load test client or simply use TcpClient within another tool like JMeter

* Uses netty as the networking framework
* Uses resilience4j to drive constant throughput
* Uses dropwizard to report metrics


## ThroughputClient
The Throughput client allows to use TcpClient to be used to deliver a fixed (or varying) load on a TCP endpoint
 
### Example Usage 
(Adapted from ThroughputClientTest)
```java
  public static void main(String[] args) {

    // The builder is used by the ThroughputClient to generate messages
    MessageBuilder builder = new MessageBuilder() {

      private final AtomicInteger stan = new AtomicInteger(0);
      private final byte[] TEMPLATE_MSG = ByteBufUtil.decodeHexDump(
          "31343230f02420000000000080000001000000000000000100000000313231323334353637383931303130303430303030303030303030303031393937373935383132323034f8f4f03132333435363738");

      @Override
      public TcpMessage newMessage() {

        //build a new message by changing the stan which acats as the
        System.arraycopy(String.format("%06d", stan.incrementAndGet()).getBytes(), 0, TEMPLATE_MSG,
            60, 6);
        stan.compareAndSet(1000000, 1);
        return new TcpMessage(TEMPLATE_MSG);
      }


    };

    // If you do not care about responses or how requests are matched against response, you can provide
    // a dummy implementation that simply returns an empty string for request and response key
    KeyExtractor keyExtractor = new KeyExtractor() {
      @Override
      public String getRequestKey(TcpMessage requestMsg) {
        return new String(Arrays.copyOfRange(requestMsg.getRequestData(), 60, 60 + 6));
      }

      @Override
      public String getResponseKey(TcpMessage responseMsg) {
        return new String(Arrays.copyOfRange(responseMsg.getResponseData(), 52, 52 + 6));
      }
    };

    //client with 10 tps
    ThroughputClient tpClient = new Builder()
        .setHost("localhost")
        .setPort(6666)
        .setMliType(MLI_TYPE.MLI_2E)
        .setInitialThroughput(10)
        .setKeyExtractor(keyExtractor)
        .setMessageBuilder(builder)
        .build();

    //for demonstration, set tps to 20 after 3 mins and 50 after 6 mins
    new Thread(() -> {
      try {
        Thread.sleep(Duration.ofMinutes(3).toMillis());
        tpClient.setThroughput(20);
        Thread.sleep(Duration.ofMinutes(3).toMillis());
        tpClient.setThroughput(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }).start();

    tpClient.start();
    
  }
```
### Sample Response
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
2020-08-24 18:47:51,076 INFO  main throughput.ThroughputClient - Initializing TcpClient .. Connecting to localhost:6666 using mli-type: MLI_2E
2020-08-24 18:47:51,347 INFO  main client.TcpClient - TcpClient connected to server localhost/127.0.0.1:6666, localAddress: /127.0.0.1:51333
2020-08-24 18:47:51,349 INFO  main throughput.ThroughputClient - Starting ThroughputClient using request rate of 10 reqs/sec
2020-08-24 18:48:21,363 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 310, #responses 310, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 24, #mean(ms) 0.723214, #75th 2.000000, #95th 2.000000
2020-08-24 18:48:51,367 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 610, #responses 610, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 36, #mean(ms) 0.815090, #75th 2.000000, #95th 2.000000
2020-08-24 18:49:21,379 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 910, #responses 910, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 36, #mean(ms) 0.825953, #75th 1.000000, #95th 2.000000
2020-08-24 18:49:51,395 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 1210, #responses 1210, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 36, #mean(ms) 0.858629, #75th 1.000000, #95th 2.000000
2020-08-24 18:50:21,397 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 1510, #responses 1510, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 36, #mean(ms) 0.674490, #75th 1.000000, #95th 2.000000
2020-08-24 18:50:51,128 INFO  Thread-1 throughput.ThroughputClient - changing request rate (tps) to - 20
2020-08-24 18:50:51,401 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 1810, #responses 1810, #timeouts 0 -- Response Times: #min(ms) 0, #max(ms) 36, #mean(ms) 0.507858, #75th 1.000000, #95th 2.000000
2020-08-24 18:51:18,087 INFO  nioEventLoopGroup-2-1 client.TcpClient - Late response: key = 002337
2020-08-24 18:51:18,602 WARN  pool-1-thread-1 client.TcpClient - Request = (002337) timed out.
2020-08-24 18:51:21,415 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 2410, #responses 2409, #timeouts 1 -- Response Times: #min(ms) 0, #max(ms) 4, #mean(ms) 0.367025, #75th 1.000000, #95th 1.000000
2020-08-24 18:51:51,426 INFO  pool-2-thread-1 client.TcpClient - Metrics: #requests 3010, #responses 3009, #timeouts 1 -- Response Times: #min(ms) 0, #max(ms) 4, #mean(ms) 0.423035, #75th 1.000000, #95th 1.000000
```
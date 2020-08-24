# tcptester
A Java framework to work with length prefixed (2I/2E etc) TCP messages. 

* Check Test `TcpClientTest.testClient` for sample usage
* For a typical use case see this - http://raghablog.blogspot.com/2015/04/tcpasync-load-testing-tcp-messages.html

## ThroughputClient
The Throughput client allows to use TcpClient to be used to deliver a fixed (or varying) load on a TCP endpoint
 
### Example Usage 

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

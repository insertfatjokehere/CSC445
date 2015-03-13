# Assignment 1

Measure the latency and throughput of TCP and UDP across at least three pairs of machines using at least two different networks. 
For example, two CS servers (like wolf and pi), or a CS server to a laptop, wired or wireless, or off-campus. 
Create a web page with graphs summarizing your results. Include the following measurements:

 * Measure round-trip latency as a function of message size, by sending and receiving (echoing) messages of size 1, 16, and 256 bytes. Measure RTTs.
 * Measure throughput by sending messages of size 1K, 4K, 16K, 64K, 256K, and 1M bytes in each direction. Measure transfer rates.
 * Measure the interaction between message size and number of messages (using TCP only) by sending 1MByte of data (with a 1-byte acknowledgment in the reverse direction) using different numbers of messages: 256 x 4KByte messages, 512 x 2KByte messages, 1024 x 1KByte messages, and so on.

For timing, use `System.nanoTime`. Read through the [Java networking tutorial.](http://docs.oracle.com/javase/tutorial/networking/index.html) 
Also see [SimpleService.java](http://gee.cs.oswego.edu/dl/csc445/SimpleService.java) and [EchoClient.java](http://gee.cs.oswego.edu/dl/csc445/EchoClient.java) for some stripped-down examples of using server and client sockets. 
When using non-CS machines and networks, minimize unnecessary traffic while developing your programs. Beware of firewalls.

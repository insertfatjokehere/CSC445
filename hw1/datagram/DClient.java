import java.io.*;
import java.net.*;

public class DClient{

  public static void main(String[] args){
    String host = "moxie.oswego.edu";
    int port = 2717;
    int i = 10;
    int k = 32;
    long start, total, milliTime;
    long aver = 0;
    try{
    InetAddress add =  InetAddress.getByName(host);

    DatagramSocket socket = new DatagramSocket();
    byte[] buf = new byte[256];
    for(int j = 0; j < i; j++){
    start = System.nanoTime();
    DatagramPacket sent = new DatagramPacket(buf, buf.length, add, port);
    socket.send(sent);
  
    // get response
    DatagramPacket reply = new DatagramPacket(buf, buf.length);
    socket.receive(reply);
    total = System.nanoTime() - start;
    milliTime = total / 1000000;
    System.out.println("Recieved packet size " + reply.getLength());
    System.out.println("RTT: \n" + total + " nanoseconds\n" + milliTime + " milliseconds");
    aver += total;
    }
    socket.close();
    System.out.println("Average: " + aver / i); 
    System.out.println("Total time: " + aver);
    } catch (Exception e){
      
      e.printStackTrace();
      System.exit(-1);
    
    }

  }

}

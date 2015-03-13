import java.io.*;
import java.net.*;

public class Client {

  public static void main(String[] args){
    String host = "moxie.oswego.edu";
    int port = 2717;
    long startTime, totalTime, milliTime;
    byte[] buff = new byte[256]; 
    long check =0;
    int num = 10;
    
    try {
	        for(int i = 0; i < num; i++){
    Socket clientSock = new Socket(host, port);
/*
    BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
    PrintStream clientOutput = new PrintStream(clientSock.getOutputStream());
*/
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
    DataInputStream in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));


    
    startTime = System.nanoTime();
/*
    clientOutput.print("Message from Client to Sever \n");
     
    String serverOut = clientInput.readLine();

    if (serverOut != null){
       System.out.println("Client recieved message: " + serverOut);
    }
*/     
    out.write(buff, 0, buff.length);

    if (in != null){
 //    in.readLine();
      in.readByte();
    }

    totalTime = (System.nanoTime() - startTime);
    milliTime = totalTime /  1000000;
    System.out.println("The Round Trip Time of the connection is: " + totalTime +" or " + milliTime + " milliseconds " + i);
      out.flush();
    out.flush();
    in.close();
    out.close();
    clientSock.close();
    check += totalTime;
    System.out.println("Total: " + check);
    }


    } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);	
    }
  
  }

}


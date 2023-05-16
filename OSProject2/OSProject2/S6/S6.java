package S6;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.lang.Thread;

//Will be run on dc24 whose ip address is 10.176.69.55
public class S6 {
    private ServerSocket server;
    private Socket client; // to S0
    public BufferedWriter bufferedWriter;
    private String name = "SERVER6";
    private static final String ipOfParent = "10.176.69.54"; // machine that Server2 is running on
    private static final int portOfParent = 5053;

    public S6(String S0address, int S0port, int port) {
        try {
            client = connectToServer(S0address, S0port);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            bufferedWriter.write(name);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            server = new ServerSocket(port);
            startServer();
        } catch (IOException e) {
            System.out.println("ERROR setting up");
            closeEverything();
            e.printStackTrace();

        }
    }

    private Socket connectToServer(String address, int port)
    {
        Socket r = null;
        try {
            r = new Socket(address, port);
            return r;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("ERROR connecting to servers");
        return r;
    }

    private void startServer() {
        int count = 0;// 1 Servers and 5 Cients
        System.out.println("Starting");
        CountDownLatch latch = new CountDownLatch(1);
        Object lock = new Object();
        try {
            if(!server.isClosed()){
                Socket p=connectToServer(ipOfParent, portOfParent);
                S6Helper Parent = new S6Helper(p, bufferedWriter,latch,lock);
                Thread thread = new Thread(Parent);
                thread.start();
                count++;
            }
            while (!server.isClosed()) {
                Socket clients = server.accept();
                S6Helper handler = new S6Helper(clients, bufferedWriter, latch, lock);
                Thread thread = new Thread(handler);
                thread.start();
                count++;
                if (count == 6) {
                    latch.countDown();
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR in accepting connections");
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            if (this.client != null) {
                this.client.close();
            }
            if (this.server != null) {
                this.server.close();
            }
        } catch (IOException e) {
            System.out.println("ERROR in closing " + name);
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws InterruptedException {

        S6 server = new S6("10.176.69.51", 5050, 5056);// Change S0addess to IP of dx20
    }
}

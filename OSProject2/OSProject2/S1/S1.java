package S1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.lang.Thread;

//Will be run on dc21 whose ip address is 10.176.69.52
public class S1 {
    private ServerSocket server;
    private Socket client; // to S0
    public BufferedWriter bufferedWriter;
    private String name = "SERVER1";

    public S1(String S0address, int S0port, int port) {
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
        int count = 0;// 2 Servers and 5 Cients
        System.out.println("Starting");
        CountDownLatch latch = new CountDownLatch(1);
        Object lock = new Object();
        try {
            while (!server.isClosed()) {
                Socket clients = server.accept();
                S1Helper handler = new S1Helper(clients, bufferedWriter, latch, lock);
                Thread thread = new Thread(handler);
                thread.start();
                count++;
                if (count == 7) {
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

        S1 server = new S1("10.176.69.51", 5050, 5051);// Change S0addess to IP of dx20
    }
}

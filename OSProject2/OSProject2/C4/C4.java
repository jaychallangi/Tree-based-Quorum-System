package C4;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class C4 {
    public BufferedWriter bufferedWriter;
    private String name = "CLIENT4";
    private Socket S0 = null;
    private static final String aS1 = "10.176.69.52"; // machine that Server1 is running on
    private static final int pS1 = 5051;
    private static final String aS2 = "10.176.69.53"; // machine that Server2 is running on
    private static final int pS2 = 5052;
    private static final String aS3 = "10.176.69.54"; // machine that Server3 is running on
    private static final int pS3 = 5053;
    private static final String aS4 = "10.176.69.55"; // machine that Server4 is running on
    private static final int pS4 = 5054;
    private static final String aS5 = "10.176.69.56"; // machine that Server5 is running on
    private static final int pS5 = 5055;
    private static final String aS6 = "10.176.69.57"; // machine that Server6 is running on
    private static final int pS6 = 5056;
    private static final String aS7 = "10.176.69.58"; // machine that Server7 is running on
    private static final int pS7 = 5057;

    public C4(String S0address, int S0port) {
        try {
            S0 = connectToServer(S0address, S0port);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(S0.getOutputStream()));
            bufferedWriter.write(name);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            startClient();
        } catch (IOException e) {
            System.out.println("ERROR setting up " + name);
            closeClient();
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

    private void startClient()// Server Function
    {
        Object lock = new Object();
        CountDownLatch latch = new CountDownLatch(1);
        // Server1
        Socket server = connectToServer(aS1, pS1);
        C4Helper SH = new C4Helper(server, bufferedWriter, latch, lock);
        Thread thread = new Thread(SH);
        thread.start();
        // Server2
        server = connectToServer(aS2, pS2);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();
        // Server3
        server = connectToServer(aS3, pS3);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();
        // Server4
        server = connectToServer(aS4, pS4);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();
        // Server5
        server = connectToServer(aS5, pS5);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();
        // Server6
        server = connectToServer(aS6, pS6);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();
        // Server7
        server = connectToServer(aS7, pS7);
        SH = new C4Helper(server, bufferedWriter, latch, lock);
        thread = new Thread(SH);
        thread.start();

        latch.countDown();

    }

    private void closeClient()// Client Function
    {
        try {
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            if (S0 != null) {
                S0.close();
            }
        } catch (IOException e) {
            System.out.println("ERROR ending connection to S0 for " + name);
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {

        C4 server = new C4("10.176.69.51", 5050);// Change S0addess to IP of dx20
    }
}
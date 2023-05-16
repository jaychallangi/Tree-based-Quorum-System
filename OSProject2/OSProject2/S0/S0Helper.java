package S0;

import java.net.Socket;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class S0Helper implements Runnable {

    public static ArrayList<S0Helper> threads = new ArrayList<S0Helper>();
    private Socket socket;
    private String name;
    private BufferedReader bufferedReader;
    private int sent = 0;
    private int received = 0;

    public S0Helper(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.name = bufferedReader.readLine();
            threads.add(this);
        } catch (IOException e) {
            System.out.println("ERROR adding client connection");
            closeEverything();
            e.printStackTrace();
        }
    }

    public void run() {
        String incoming = "";
        try {
            while (this.socket.isConnected()) {
                incoming = bufferedReader.readLine();
                //System.out.println(this.name + ": " + incoming);
                if (incoming.contains("COMPLETE")) {
                    System.out.println(this.name + "'s Completion:");
                    for (int i = 0; i < threads.size(); i++) {
                        System.out.println(threads.get(i).name +
                                " sent " + threads.get(i).sent +
                                " messages and received " + threads.get(i).received +
                                " messages.");
                    }
                    System.out.println("\n");
                } else if (incoming.contains("ENTERING")) {
                    // ENTERING CLIENT starttime#timespent
                    received++; //Client only recieves a message when it recieves grant, right after grant is entering
                    String[] temp = incoming.split("#", 3);
                    System.out.println(temp[0]);
                    System.out.println(this.name +
                            " exchanged " + temp[1] +
                            " messages and spent " + temp[2] +
                            " milliseconds trying to enter the critical section.");
                } else if (incoming.contains("SENT")) {
                    this.sent += 1;
                } else if (incoming.contains("RECEIVED")) {
                    this.received += 1;
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void closeEverything() {
        threads.remove(this);
        try {
            if (this.bufferedReader != null) {
                this.bufferedReader.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            System.out.println("ERROR with ending connections");
            e.printStackTrace();
        }
    }

}

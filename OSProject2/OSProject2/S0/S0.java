package S0;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

//Will be run on dc20 whose ip address is 10.176.69.51
public class S0 // Just a Data Collection Server
{
    private ServerSocket server;

    public S0(int port) {
        try {
            server = new ServerSocket(port);
            startServer();
        } catch (IOException e) {
            System.out.println("ERROR setting up socket");
            closeServer();
            e.printStackTrace();
        }
    }

    private void startServer() {
        System.out.println("Starting");
        try {
            while (!server.isClosed()) {
                Socket clients = server.accept();
                S0Helper handler = new S0Helper(clients);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("ERROR in accepting connections");
            closeServer();
            e.printStackTrace();
        }
    }

    private void closeServer() {
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            System.out.println("ERROR in closing server");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {

        S0 server = new S0(5050);
    }
}
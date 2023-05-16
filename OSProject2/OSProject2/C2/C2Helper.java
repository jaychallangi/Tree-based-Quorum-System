package C2;

import java.net.Socket;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.time.Instant;
import java.lang.Math;
import java.lang.Thread;
import java.util.concurrent.CountDownLatch;

public class C2Helper implements Runnable {

    public static ArrayList<C2Helper> servers = new ArrayList<C2Helper>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    public static BufferedWriter toS0;
    private String name;
    public static final String clientName = "CLIENT2";
    public static boolean inCritical = false;
    public static Object lock;
    public CountDownLatch latch;
    public static int nCompleted = 0;
    private int nMessages = 0;

    public C2Helper(Socket connection, BufferedWriter S0, CountDownLatch xlatch, Object xlock) {
        try {
            if (toS0 == null && lock == null) {
                toS0 = S0;
                lock = xlock;
            }
            this.latch = xlatch;
            this.socket=connection;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writeToConnection(clientName); // Share name to connection
            this.name = bufferedReader.readLine();
            if (this.name.contains("SERVER")) {
                servers.add(this);
                System.out.println("Connected to " + this.name);
            }
        } catch (IOException e) {
            System.out.println("ERROR adding connection");
            closeEverything();
            e.printStackTrace();
        }

    }

    public void run() {
        try {
            this.latch.await();
        } catch (InterruptedException e) {
        }
        String incoming = "";
        String outgoing = "";
        String log = "";
        Instant sentRequest;
        Instant recievedGrant;
        while (this.socket.isConnected()) {
            try {
                if (inCritical) {
                    synchronized (lock) {
                        System.out.println(this.name + ": Locked");
                        lock.wait();
                    }
                }
                if (nCompleted == 20)
                    closeEverything();

                int waitTime = (int) Math.floor(Math.random() * (10 - 5 + 1) + 5);
                Thread.sleep(waitTime);

                if (inCritical) {
                    synchronized (lock) {
                        System.out.println(this.name + ": Locked");
                        lock.wait();
                    }
                }
                if (nCompleted == 20)
                    closeEverything();

                sentRequest = Instant.now();
                outgoing = "REQUEST #" + sentRequest.toString();
                writeToConnection(outgoing);

                nMessages++;
                log = "SENT " + outgoing + " to " + this.name;
                writeToS0(log);

                if (inCritical) {
                    synchronized (lock) {
                        System.out.println(this.name + ": Locked");
                        lock.wait();
                    }
                }
                if (nCompleted == 20)
                    closeEverything();

                incoming = bufferedReader.readLine();
                nMessages++;
                if (incoming.contains("GRANT")) {
                    synchronized (lock) {
                        int messages=0;
                        for(C2Helper x:servers)
                        {
                            messages+=x.nMessages;
                        }
                        System.out.println(this.name + ": In Critical");
                        inCritical=true;
                        recievedGrant = Instant.now();
                        long milli = recievedGrant.toEpochMilli() - sentRequest.toEpochMilli();
                        outgoing = "ENTERING " + clientName + " " + recievedGrant.toString() + "#" + messages
                                + "#" + milli;
                        writeToS0(outgoing);
                        this.nMessages=0;
                        Thread.sleep(3);
                        nCompleted++;
                        for(C2Helper x:servers)
                        {
                            outgoing="RELEASE";
                            log="SENT "+outgoing+ " to "+x.name;
                            x.writeToConnection(outgoing);
                            writeToS0(log);
                        }
                        synchronized (lock) {
                            inCritical = false;
                            System.out.println(this.name + ": Unlocking all other threads");
                            lock.notifyAll();
                        }if (nCompleted == 20) {
                            outgoing = "COMPLETE";
                            writeToS0(outgoing);
                            closeEverything();
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("ERROR occured with handling " + name);
                closeEverything();
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println(this.name + " is Waiting");
            }
        }

    }

    public static void writeToS0(String message) {
        try {
            synchronized (lock) {
                System.out.println(message+".");
                System.out.flush();
                toS0.write(message);
                toS0.newLine();
                toS0.flush();
            }
        } catch (IOException e) {
            System.out.println("ERROR wrtiting to S0");
        }
    }

    public void writeToConnection(String message) {
        try {
            this.bufferedWriter.write(message);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            System.out.println("ERROR wrtiting to connection");
        }
    }

    public void closeEverything() {
        servers.remove(this);
        try {
            if (this.bufferedReader != null) {
                this.bufferedReader.close();
            }
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            if (this.socket != null) {
                socket.close();
            }
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.out.println("ERROR with ending connection");
            e.printStackTrace();
        }
    }
}

package snd.sketchpilot.core.bridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Client transport used by the companion app and the injected panel. */
public final class SocketBridgeTransport implements BridgeTransport {

    private final String host;
    private final int port;

    public SocketBridgeTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isAvailable() {
        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByName(host), port);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    public BridgeMessage call(BridgeMessage message) throws BridgeException {
        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        try {
            socket = new Socket(InetAddress.getByName(host), port);
            socket.setSoTimeout(8000);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer.println(message.toJsonString());
            writer.flush();
            String line = reader.readLine();
            if (line == null) {
                throw new BridgeException("no response from bridge");
            }
            return BridgeMessage.fromJson(line);
        } catch (BridgeException e) {
            throw e;
        } catch (Throwable t) {
            throw new BridgeException("bridge call failed", t);
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }
}

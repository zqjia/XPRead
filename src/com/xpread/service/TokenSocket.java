
package com.xpread.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * <p>
 * Title: ucweb
 * </p>
 * <p>
 * Description:
 * </p>
 * TokenSocket is use to keep alive the connection with two devices <br>
 * the HTTP server can use TokenSocket to send message to client<br>
 * must take the initiative to close the TokenSocket when two devices disconnect
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 * <p>
 * Company: ucweb.com
 * </p>
 * 
 * @author wujm@ucweb.com
 * @version 1.0
 */
public class TokenSocket {
    private int BUFFER_SIZE = 8096;

    private String mSocketName;

    private Socket mSocket;

    private PushbackInputStream mInputStream;

    public Socket getSocket() {
        return mSocket;
    }

    public TokenSocket(Socket socket, String socketName, PushbackInputStream inputStream)
            throws SocketException {
        this.mSocket = socket;
        this.mSocketName = socketName;
        if (this.mSocket != null)
            mSocket.setSoTimeout(0);
        this.mInputStream = inputStream;
    }

    /**
     * 不设置inputStream，自动初始化为socket.getInputStream
     * 
     * @param socket
     * @param socketName
     */
    public TokenSocket(Socket socket, String socketName) {
        this.mSocket = socket;
        this.mSocketName = socketName;
    }

    public final PushbackInputStream getInputStream() throws IOException {
        if (mInputStream != null) {
            return mInputStream;
        }
        return new PushbackInputStream(mSocket.getInputStream(), BUFFER_SIZE);
    }

    public final OutputStream getOutputStream() throws IOException {
        if (mSocket != null)
            return mSocket.getOutputStream();
        return null;
    }

    /**
     * @return the name of socket
     */
    public final String getSocketName() {
        return mSocketName;
    }

    /**
     * close token socket
     */
    public final void close() {
        if (mSocket != null)
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

}

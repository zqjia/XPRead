
package com.xpread.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.xpread.util.LogUtil;

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
public class TokenSession {

    private MyLog myLog = new MyLog(TokenSession.class.getSimpleName());

    public static final int BUFFSIZE = 1024;

    private TokenSocket mTokenSocket;

    private LinkedBlockingQueue<byte[]> mTokenMessageQueue = new LinkedBlockingQueue<byte[]>(50);

    /**
     * 在Token连接建立之前，临时缓存一些需要发送的信息，但是最多能缓存两条
     */
    private static ConcurrentLinkedQueue<byte[]> mCacheMessage = new ConcurrentLinkedQueue<byte[]>();

    private HashMap<String, Integer> mFilesSizeMap = new HashMap<String, Integer>();

    private HashMap<String, Integer> mFilesStateMap = new HashMap<String, Integer>();

    private HashMap<String, String> mFilesNameMap = new HashMap<String, String>();

    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    private HashMap<String, String> mParams = new HashMap<String, String>();

    private ReceiveTokenServe mReceiveTokenServe;

    public static synchronized void addMessageBeforeConnected(byte[] messageBytes) {
        mCacheMessage.add(messageBytes);
    }

    public void dealTokenInputStream() throws IOException {
        while (!Thread.interrupted()) {
            parseReceiveToken();
        }
    }

    public void dealTokenOutputSteam() throws IOException, InterruptedException {
        OutputStream outputStream = mTokenSocket.getOutputStream();
        while (mCacheMessage.size() > 0 && !Thread.interrupted()) {
            byte[] messageBytes = mCacheMessage.remove();
            if (messageBytes == null) {
                continue;
            }
            myLog.e("TOKEN WRITE MESSAGE TO " + mTokenSocket.getSocketName() + "\n"
                    + new String(messageBytes));
            outputStream.write(messageBytes);
        }
        while (!Thread.interrupted()) {
            byte[] messageBytes = mTokenMessageQueue.take();
            myLog.e("TOKEN WRITE MESSAGE TO " + mTokenSocket.getSocketName() + "\n"
                    + new String(messageBytes));
            outputStream.write(messageBytes);

            if (mReceiveTokenServe != null) {
                mReceiveTokenServe.justSendOrReceiveMessage();
                if (mTokenMessageQueue.size() == 0) {
                    byte[] ackDisBytes = TokenCommand.createACKDisConnecetMessage();
                    byte[] disBytes = TokenCommand.createDisConnecetMessage();
                    if (messageBytes.length == ackDisBytes.length
                            || messageBytes.length == disBytes.length) {
                        String ackDis = new String(ackDisBytes);
                        String s = new String(messageBytes);
                        if (s.equals(ackDis)) {
                            mReceiveTokenServe
                                    .finishWriteDisConnectionMessage(TokenCommand.MESSAGE_TYPE_ACK_DISCONNECT);
                            continue;
                        }
                        String dis = new String(disBytes);
                        if (s.equals(dis)) {
                            mReceiveTokenServe
                                    .finishWriteDisConnectionMessage(TokenCommand.MESSAGE_TYPE_DISCONNECT);
                        }
                    }
                }
            }
        }
    }

    private void parseReceiveToken() throws IOException {

        PushbackInputStream inputStream = mTokenSocket.getInputStream();
        byte[] buf = new byte[BUFFSIZE];
        int read = -1;
        mHeaders.clear();
        mFilesSizeMap.clear();
        mFilesStateMap.clear();
        mFilesNameMap.clear();
        mParams.clear();
        int splitbyte = 0;
        int rlen = 0;
        int needReadLength = 0;
        try {
            if (needReadLength == 0) {
                read = inputStream.read(buf, 0, BUFFSIZE);
            } else {
                read = inputStream.read(buf, 0, needReadLength);
            }
        } catch (IOException e) {
            myLog.e("TOKEN -> peer close output stream and current read != -1 ,socket error");
            throw e;
        }
        if (read == -1) {
            myLog.e("TOKEN -> peer close output stream and current read = -1 ");
            throw new SocketException("socket inputstream close read = -1 ");
        }
        while (read > 0) {
            rlen += read;
            splitbyte = findTokenMessageHeadEnd(buf, rlen);
            if (splitbyte > 0)
                break;
            read = inputStream.read(buf, rlen, BUFFSIZE - rlen);
        }

        if (splitbyte < rlen) {

            inputStream.unread(buf, splitbyte, rlen - splitbyte);
        }

        BufferedReader tokenHeadin = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(buf, 0, rlen)));
        decodeTokenHeader(tokenHeadin, mHeaders);
        // 确保每次都读完--------------------------------------------------------
        needReadLength = Integer.parseInt(mHeaders.get("content-length"));
        needReadLength = rlen - splitbyte - needReadLength;
        decodeTokenBody(inputStream, mHeaders, mFilesSizeMap, mParams, mFilesStateMap,
                mFilesNameMap);
        myLog.e("TOKEN RECEVICE MESSAGE : " + "\nhead -->" + mHeaders.toString() + "\nparams --->"
                + mParams.toString() + " \nfileSizes ---> " + mFilesSizeMap.toString()
                + "\nFileState ---> " + mFilesStateMap.toString() + "\nfileNames ---> "
                + mFilesNameMap.toString());
        if (mReceiveTokenServe != null) {
            mReceiveTokenServe.serveToken(mHeaders, mParams, mFilesSizeMap, mFilesStateMap,
                    mFilesNameMap);
            mReceiveTokenServe.justSendOrReceiveMessage();
        } else {
            throw new IOException("token server is no implments");
        }
        // }
    }

    private int findTokenMessageHeadEnd(byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 3 < rlen) {
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r'
                    && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }
            splitbyte++;
        }
        return 0;
    }

    private void decodeTokenHeader(BufferedReader in, Map<String, String> headers)
            throws IOException {
        try {
            String line = in.readLine();
            if (line == null) {
                return;
            }
            while (line != null && line.trim().length() > 0) {
                int p = line.indexOf(":");
                if (p > 0)
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.US),
                            line.substring(p + 1).trim());
                line = in.readLine();
            }
        } catch (IOException e) {
            LogUtil.e("TOKEN -> Token header prase error");
            // 将异常往上抛出
            throw e;
        }
    }

    private void decodeTokenBody(PushbackInputStream inputStream, Map<String, String> headers,
            Map<String, Integer> fileSizeMaps, Map<String, String> params,
            Map<String, Integer> fileStateMaps, Map<String, String> fileNameMaps)
            throws IOException {
        if (!headers.containsKey("message-type")) {
            throw new SocketException("TOKEN ->  miss message-type");
        }

        if (!headers.containsKey("content-length")) {
            throw new SocketException("TOKEN ->  miss content-length");
        }

        int messageType = Integer.parseInt(headers.get("message-type"));
        int contentLength = Integer.parseInt(headers.get("content-length"));
        // FIXME global variable 2048
        if (contentLength > 2048) {
            throw new SocketException("TOKEN ->  content to long");
        }
        // 1 is files send
        switch (messageType) {
            case TokenCommand.MESSAGE_TYPE_CONNECT_ESTABLE: {
                break;
            }
            case TokenCommand.MESSAGE_TYPE_SEND_FILES: {
                byte[] buf = new byte[contentLength];
                inputStream.read(buf, 0, contentLength);
                BufferedReader contentIn = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(buf, 0, contentLength)));
                String line = contentIn.readLine();
                while (line != null && line.trim().length() > 0) {
                    // int p = line.indexOf(":");
                    // if (p > 0) {
                    // int len = Integer.parseInt(line.substring(p + 1).trim());
                    // fileSizeMaps.put(line.substring(0, p), len);
                    // }
                    // line = contentIn.readLine();

                    StringTokenizer st = new StringTokenizer(line, ":");
                    String file = st.nextToken();
                    int fileSize = Integer.parseInt(st.nextToken());
                    String fileName = st.nextToken();
                    fileSizeMaps.put(file, fileSize);
                    fileNameMaps.put(file, fileName);
                    line = contentIn.readLine();
                }
            }
                break;
            case TokenCommand.MESSAGE_TYPE_USER_INFORMAITION: {
                byte[] buf = new byte[contentLength];
                inputStream.read(buf, 0, contentLength);
                BufferedReader contentIn = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(buf, 0, contentLength)));
                String line = contentIn.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(":");
                    if (p > 0)
                        params.put(line.substring(0, p), line.substring(p + 1).trim());
                    line = contentIn.readLine();
                }
            }
                break;
            case TokenCommand.MESSAGE_TYPE_READY_FOR_RECEIVE_FILE: {
                byte[] buf = new byte[contentLength];
                inputStream.read(buf, 0, contentLength);
                BufferedReader contentIn = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(buf, 0, contentLength)));
                String line = contentIn.readLine();
                while (line != null && line.trim().length() > 0) {
                    StringTokenizer st = new StringTokenizer(line, ":");
                    String file = st.nextToken();
                    int fileSize = Integer.parseInt(st.nextToken());
                    int fileState = Integer.parseInt(st.nextToken());
                    fileSizeMaps.put(file, fileSize);
                    fileStateMaps.put(file, fileState);
                    line = contentIn.readLine();
                }
            }
                break;
            case TokenCommand.MESSAGE_TYPE_CANCEL_FILE: {
                byte[] buf = new byte[contentLength];
                inputStream.read(buf, 0, contentLength);
                BufferedReader contentIn = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(buf, 0, contentLength)));
                String line = contentIn.readLine();
                while (line != null && line.trim().length() > 0) {
                    StringTokenizer st = new StringTokenizer(line, ":");
                    String file = st.nextToken();
                    int fileSize = Integer.parseInt(st.nextToken());
                    int fileState = Integer.parseInt(st.nextToken());
                    fileSizeMaps.put(file, fileSize);
                    fileStateMaps.put(file, fileState);
                    line = contentIn.readLine();
                }
            }
                break;
            case TokenCommand.MESSAGE_TYPE_DISCONNECT: {
                break;
            }
            case TokenCommand.MESSAGE_TYPE_ACK_DISCONNECT: {
                break;
            }
        }
    }

    public TokenSession(TokenSocket tokenSocket) {
        this.mTokenSocket = tokenSocket;
    }

    /**
     * 对Token的内容进行处理
     */
    public interface ReceiveTokenServe {
        public void serveToken(HashMap<String, String> headers, HashMap<String, String> params,
                HashMap<String, Integer> fileSizeMap, HashMap<String, Integer> fileStateMap,
                HashMap<String, String> fileNameMap);

        public void finishWriteDisConnectionMessage(int tokenCammand);

        public void justSendOrReceiveMessage();
    }

    public boolean sendMessageFromToken(byte[] messageBytes) throws Exception {
        if (messageBytes == null) {
            myLog.e("消息为空，无法发送");
        }
        // FIXME 消息为空的时候抛出nullpoint异常
        return mTokenMessageQueue.offer(messageBytes);
    }

    public boolean clearAllMessageAndSendThisMessage(byte[] messageBytes) {
        if (messageBytes == null) {
            myLog.e("消息为空，无法发送");
        }
        if (mTokenMessageQueue.size() > 0) {
            mTokenMessageQueue.clear();
        }
        return mTokenMessageQueue.offer(messageBytes);
    }

    /**
     * @param mReceiveTokenServe the mReceiveTokenServe to set
     */
    public void setReceiveTokenServe(ReceiveTokenServe mReceiveTokenServe) {
        this.mReceiveTokenServe = mReceiveTokenServe;
    }

    public final void clear() {
        mTokenMessageQueue.clear();
    }

}

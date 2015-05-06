
package com.xpread.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.xpread.service.HTTPResponse.ResponseException;

public class HTTPSession implements IHTTPSession {

    private MyLog myLog = new MyLog(HTTPSession.class.getSimpleName());

    public static final int BUFSIZE = 8192;

    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     * This is required as the Keep-Alive HTTP connections would otherwise block
     * the socket reading thread forever (or as long the browser is open).
     */
    public static final int SOCKET_READ_TIMEOUT = 5000;

    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";

    /**
     * Common mime type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";

    /**
     * Pseudo-Parameter to use to store the actual query string in the
     * parameters map for later re-processing.
     */
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";

    private final OutputStream outputStream;

    private PushbackInputStream inputStream;

    private int splitbyte;

    private int rlen;

    private String uri;

    private HTTPMethod method;

    private Map<String, String> parms;

    private Map<String, String> headers;

    private CookieHandler cookies;

    private Socket socket;

    private String queryParameterString;

    private ReceviceTokenRequest mReceviceTokenRequest;

    private boolean canClose = true;

    private int mTempUnreadLength;

    public HTTPSession(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = new PushbackInputStream(socket.getInputStream(), BUFSIZE);
        this.outputStream = socket.getOutputStream();
    }

    public HTTPSession(InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
        this.inputStream = new PushbackInputStream(inputStream, BUFSIZE);
        this.outputStream = outputStream;
        String remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1"
                : inetAddress.getHostAddress().toString();
        headers = new HashMap<String, String>();

        headers.put("remote-addr", remoteIp);
        headers.put("http-client-ip", remoteIp);
    }

    @Override
    public void execute() throws java.lang.Exception {
        try {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header
            // at once!
            byte[] buf = new byte[BUFSIZE];
            splitbyte = 0;
            rlen = 0;
            mTempUnreadLength = 0;
            {
                int read = -1;
                try {
                    read = inputStream.read(buf, 0, BUFSIZE);
                    myLog.e("***********************execute  read " + read);
                } catch (IOException e) {
                    myLog.e("http socket intput stream error");
                    throw e;
                }
                if (read == -1) {
                    myLog.e("http socket intput stream read = -1 ");
                    throw new SocketException("http socket close before parse http head");
                }
                while (read > 0) {
                    rlen += read;
                    splitbyte = findHeaderEnd(buf, rlen);
                    if (splitbyte > 0)
                        break;
                    read = inputStream.read(buf, rlen, BUFSIZE - rlen);
                }
            }
//            Log.e("***********************", "execute  unread " + (rlen - splitbyte));

            if (splitbyte < rlen) {
                mTempUnreadLength = rlen - splitbyte;
                inputStream.unread(buf, splitbyte, rlen - splitbyte);
            }

            parms = new HashMap<String, String>();
            if (null == headers) {
                headers = new HashMap<String, String>();
            }

            BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(
                    buf, 0, rlen)));

            Map<String, String> pre = new HashMap<String, String>();
            decodeHeader(hin, pre, parms, headers);

            method = HTTPMethod.lookup(pre.get("method"));
            if (method == null) {
                throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                        "BAD REQUEST: Syntax error.");
            }

            uri = pre.get("uri");

            cookies = new CookieHandler(headers);

            if (parms.containsKey("socket") && parms.containsKey("name")) {
                if (mReceviceTokenRequest != null) {
                    canClose = mReceviceTokenRequest.dealTokenSocket(new TokenSocket(socket, parms
                            .get("name"), inputStream));
                    return;
                } else {
                    throw new IOException("deal token socket method no implements");
                }
            }

            // Ok, now do the serve()
            HTTPResponse r = serve(this);

            if (r == null) {
                throw new ResponseException(HTTPResponse.Status.INTERNAL_ERROR,
                        "SERVER INTERNAL ERROR: Serve() returned a null response.");
            } else {
                cookies.unloadQueue(r);
                r.setRequestMethod(method);
                r.send(outputStream);
            }
        } catch (SocketException e) {
            // throw it out to close socket object (finalAccept)
            throw e;
        } catch (SocketTimeoutException ste) {
            throw ste;
        } catch (IOException ioe) {
            myLog.logException(ioe);
            HTTPResponse r = new HTTPResponse(HTTPResponse.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            r.send(outputStream);
            // safeClose(outputStream);
        } catch (ResponseException re) {
            HTTPResponse r = new HTTPResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            r.send(outputStream);
        } finally {
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void parseBody(Map<String, String> files) throws java.lang.Exception {

        if (HTTPMethod.POST.equals(method)) {
            String contentType = "";
            String contentTypeHeader = headers.get("content-type");

            StringTokenizer st = null;
            if (contentTypeHeader != null) {
                st = new StringTokenizer(contentTypeHeader, ",; ");
                if (st.hasMoreTokens()) {
                    contentType = st.nextToken();
                }
            }
            if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                if (!st.hasMoreTokens()) {
                    throw new ResponseException(
                            HTTPResponse.Status.BAD_REQUEST,
                            "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
                }

                String boundaryStartString = "boundary=";
                int boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString)
                        + boundaryStartString.length();
                String boundary = contentTypeHeader.substring(boundaryContentStart,
                        contentTypeHeader.length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                decodeMultipartData(boundary, inputStream, parms, files);
            } else {
                throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                        "BAD REQUEST: when post file the contentType missing ");

            }
        } else if (HTTPMethod.PUT.equals(method)) {
            throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                    "BAD REQUEST: the server cannot deal the put method");
        }
    }

    /**
     * Decodes the sent headers and loads the data into Key/value pairs
     */
    private void decodeHeader(BufferedReader in, Map<String, String> pre,
            Map<String, String> parms, Map<String, String> headers) throws ResponseException {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                        "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
            }

            pre.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                        "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else {
                uri = decodePercent(uri);
            }

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line
                                .substring(p + 1).trim());
                    line = in.readLine();
                }
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            throw new ResponseException(HTTPResponse.Status.INTERNAL_ERROR,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
        }
    }

    /**
     * Decodes the Multipart Body data and put it into Key/Value pairs.
     * 
     * @throws java.lang.Exception
     */
    private void decodeMultipartData(String boundary, PushbackInputStream inputStream,
            Map<String, String> parms, Map<String, String> files) throws java.lang.Exception {
        int fileHeadAndEndByte = 0;
        try {
            // 处理post body
            byte[] buf = new byte[BUFSIZE];
            int fileHeadSplitByte = 0;
            int readLength = 0;
            int read = -1;
            myLog.e(" decodeMultipartData stop ");
            if (mTempUnreadLength > 0) {
                read = inputStream.read(buf, 0, mTempUnreadLength);
            } else {
                read = inputStream.read(buf, 0, BUFSIZE);
            }
            if (read == -1) {
                throw new SocketException(
                        "http upload file before parse file head input stream was close and read -1  ");
            }
            while (read > 0) {
                readLength += read;
                fileHeadSplitByte = findHeaderEnd(buf, readLength);
                if (fileHeadSplitByte > 0)
                    break;
                read = inputStream.read(buf, readLength, BUFSIZE - readLength);
            }

            if (fileHeadSplitByte < readLength) {
                inputStream.unread(buf, fileHeadSplitByte, readLength - fileHeadSplitByte);
            }
            BufferedReader fileHeadin = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(buf, 0, rlen)));
            fileHeadAndEndByte += fileHeadSplitByte;
            int endLen = ("\r\n" + "--" + boundary + "--" + "\r\n").getBytes().length;
            fileHeadAndEndByte += endLen;
            String mpline = fileHeadin.readLine();
            // while (mpline != null) {
            if (!mpline.contains(boundary)) {
                throw new ResponseException(
                        HTTPResponse.Status.BAD_REQUEST,
                        "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
            }
            Map<String, String> item = new HashMap<String, String>();
            mpline = fileHeadin.readLine();
            while (mpline != null && mpline.trim().length() > 0) {
                int p = mpline.indexOf(':');
                if (p != -1) {
                    item.put(mpline.substring(0, p).trim().toLowerCase(Locale.US), mpline
                            .substring(p + 1).trim());
                }
                mpline = fileHeadin.readLine();
            }
            if (mpline != null) {
                String contentDisposition = item.get("content-disposition");
                if (contentDisposition == null) {
                    throw new ResponseException(
                            HTTPResponse.Status.BAD_REQUEST,
                            "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
                }
                StringTokenizer st = new StringTokenizer(contentDisposition, ";");
                Map<String, String> disposition = new HashMap<String, String>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    int p = token.indexOf('=');
                    if (p != -1) {
                        disposition.put(token.substring(0, p).trim().toLowerCase(Locale.US), token
                                .substring(p + 1).trim());
                    }
                }
                String pname = disposition.get("name");
                pname = pname.substring(1, pname.length() - 1);

                String value = "";
                if (item.get("content-type") == null) {
                    while (mpline != null && !mpline.contains(boundary)) {
                        mpline = fileHeadin.readLine();
                        if (mpline != null) {
                            int d = mpline.indexOf(boundary);
                            if (d == -1) {
                                value += mpline;
                            } else {
                                value += mpline.substring(0, d - 2);
                            }
                        }
                    }
                } else {
                    long size;
                    if (headers.containsKey("content-length")) {
                        size = Integer.parseInt(headers.get("content-length"));
                    } else {
                        throw new ResponseException(HTTPResponse.Status.BAD_REQUEST,
                                "content-length is empty");
                    }
                    // 一次post请求只能上传一个文件
                    value = disposition.get("filename");
                    value = value.substring(1, value.length() - 1);
                    String path = null;
                    if (size != 0) {
                        path = saveFile(inputStream, URLDecoder.decode(value, "utf-8"),
                                (int)(size - fileHeadAndEndByte));
                        files.put(pname, path);
                    }
                    if (path == null) {
                        return;
                    }
                    int length = inputStream.read(buf, 0, endLen);
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new ByteArrayInputStream(buf, 0, length)));
                    do {
                        mpline = in.readLine();
                    } while (mpline != null && !mpline.contains(boundary));
                    // }
                    parms.put(pname, value);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new ResponseException(HTTPResponse.Status.INTERNAL_ERROR,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
        } catch (ResponseException re) {
            HTTPResponse r = new HTTPResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            r.send(outputStream);
        } finally {
        }
    }

    // private void setSocket(Socket socket) {
    // this.socket = socket;
    // }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     */
    private int findHeaderEnd(final byte[] buf, int rlen) {
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

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
     * NOTE: this doesn't support multiple identical keys due to the simplicity
     * of Map.
     */
    private void decodeParms(String parms, Map<String, String> p) {
        if (parms == null) {
            queryParameterString = "";
            return;
        }

        queryParameterString = parms;
        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
                p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
            } else {
                p.put(decodePercent(e).trim(), "");
            }
        }
    }

    @Override
    public final Map<String, String> getParms() {
        return parms;
    }

    public String getQueryParameterString() {
        return queryParameterString;
    }

    @Override
    public final Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public final String getUri() {
        return uri;
    }

    @Override
    public final HTTPMethod getMethod() {
        return method;
    }

    @Override
    public final InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public CookieHandler getCookies() {
        return cookies;
    }

    /**
     * Decode percent encoded <code>String</code> values.
     * 
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes
     *         "foo bar"
     */
    protected String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return decoded;
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     * 
     * @param uri Percent-decoded URI without parameters, for example
     *            "/index.cgi"
     * @param method "GET", "POST" etc.
     * @param parms Parsed, percent decoded parameters from URI and, in case of
     *            POST, data.
     * @param headers Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    @Deprecated
    protected HTTPResponse serve(String uri, HTTPMethod method, Map<String, String> headers,
            Map<String, String> parms, Map<String, String> files) {
        return new HTTPResponse(HTTPResponse.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     * 
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     * @throws java.lang.Exception
     */
    protected HTTPResponse serve(IHTTPSession session) throws java.lang.Exception {
        Map<String, String> files = new HashMap<String, String>();
        HTTPMethod method = session.getMethod();
        if (HTTPMethod.PUT.equals(method) || HTTPMethod.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return new HTTPResponse(HTTPResponse.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new HTTPResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        Map<String, String> parms = session.getParms();
        parms.put(QUERY_STRING_PARAMETER, session.getQueryParameterString());
        return serve(session.getUri(), method, session.getHeaders(), parms, files);
    }

    /**
     * Provides rudimentary support for cookies. Doesn't support 'path',
     * 'secure' nor 'httpOnly'. Feel free to improve it and/or add unsupported
     * features.
     * 
     * @author LordFokas
     */
    public class CookieHandler implements Iterable<String> {
        private HashMap<String, String> cookies = new HashMap<String, String>();

        private ArrayList<Cookie> queue = new ArrayList<Cookie>();

        public CookieHandler(Map<String, String> httpHeaders) {
            String raw = httpHeaders.get("cookie");
            if (raw != null) {
                String[] tokens = raw.split(";");
                for (String token : tokens) {
                    String[] data = token.trim().split("=");
                    if (data.length == 2) {
                        cookies.put(data[0], data[1]);
                    }
                }
            }
        }

        @Override
        public Iterator<String> iterator() {
            return cookies.keySet().iterator();
        }

        /**
         * Read a cookie from the HTTP Headers.
         * 
         * @param name The cookie's name.
         * @return The cookie's value if it exists, null otherwise.
         */
        public String read(String name) {
            return cookies.get(name);
        }

        /**
         * Sets a cookie.
         * 
         * @param name The cookie's name.
         * @param value The cookie's value.
         * @param expires How many days until the cookie expires.
         */
        public void set(String name, String value, int expires) {
            queue.add(new Cookie(name, value, Cookie.getHTTPTime(expires)));
        }

        public void set(Cookie cookie) {
            queue.add(cookie);
        }

        /**
         * Set a cookie with an expiration date from a month ago, effectively
         * deleting it on the client side.
         * 
         * @param name The cookie name.
         */
        public void delete(String name) {
            set(name, "-delete-", -30);
        }

        /**
         * Internally used by the webserver to add all queued cookies into the
         * Response's HTTP Headers.
         * 
         * @param response The Response object to which headers the queued
         *            cookies will be added.
         */
        public void unloadQueue(HTTPResponse response) {
            for (Cookie cookie : queue) {
                response.addHeader("Set-Cookie", cookie.getHTTPHeader());
            }
        }

    }

    public interface ReceviceTokenRequest {
        /**
         * @param socket
         * @return false 表示socket不由调用函数处理，实现需自行关闭socket <br>
         *         true 表示socket由调用函数处理，实现无需对socket处理
         */
        boolean dealTokenSocket(TokenSocket socket);
    }

    /**
     * Override this to save file.
     * 
     * @param inputStream
     * @param fileName
     * @param size the size of file
     * @return the path of the file save in loacl
     * @throws IOException
     * @throws java.lang.Exception
     * @throws java.lang.Exception
     */
    protected String saveFile(InputStream inputStream, String fileName, int size)
            throws IOException, java.lang.Exception {
        throw new IOException("no implements save file method");
    }

    public static class Cookie {
        private String n, v, e;

        public Cookie(String name, String value, String expires) {
            n = name;
            v = value;
            e = expires;
        }

        public Cookie(String name, String value) {
            this(name, value, 30);
        }

        public Cookie(String name, String value, int numDays) {
            n = name;
            v = value;
            e = getHTTPTime(numDays);
        }

        public String getHTTPHeader() {
            String fmt = "%s=%s; expires=%s";
            return String.format(fmt, n, v, e);
        }

        public static String getHTTPTime(int days) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                    Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            calendar.add(Calendar.DAY_OF_MONTH, days);
            return dateFormat.format(calendar.getTime());
        }
    }

    /**
     * @return the canClose
     */
    public boolean isCanClose() {
        return canClose;
    }

    /**
     * @param mReceviceTokenRequest the mReceviceTokenRequest to set
     */
    public void setReceviceTokenRequest(ReceviceTokenRequest mReceviceTokenRequest) {
        this.mReceviceTokenRequest = mReceviceTokenRequest;
    }

}

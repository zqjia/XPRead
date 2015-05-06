
package com.xpread.service;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Map;


import com.xpread.service.HTTPResponse.Status;
import com.xpread.transfer.exception.FileNotReadyException;
import com.xpread.transfer.exception.FileTransferCancelException;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;

public class HTTPFileSession extends HTTPSession {

    private MyLog myLog = new MyLog(HTTPFileSession.class.getSimpleName());

    private HTTPServerCommand mCommand;

    public HTTPFileSession(Socket socket, HTTPServerCommand httpServerCommand) throws IOException {
        super(socket);
        this.mCommand = httpServerCommand;
    }

    @SuppressWarnings("deprecation")
    @Override
    public HTTPResponse serve(String uri, HTTPMethod method, Map<String, String> headers,
            Map<String, String> parms, Map<String, String> files) {

        if (parms.containsKey("file") && method.equals(HTTPMethod.GET)) {
            String filePath = parms.get("file");
            try {
                filePath = URLDecoder.decode(filePath, "utf-8");
            } catch (UnsupportedEncodingException e) {
                myLog.logException(e);
                return new HTTPResponse(Status.NOT_FOUND, MIME_HTML, filePath
                        + " user utf-8 to encode");
            }
//            Log.e("***********************", "file = " + filePath);
            File file = new File(filePath);
            if (!file.exists()) {
//                Log.e("***********************", "file = " + filePath);
                // TODO
                // 发送文件不存在
                // --------------------------------------------------
                return new HTTPResponse(Status.NOT_FOUND, MIME_HTML, filePath + " not found");
            }

            HTTPResponse r = new HTTPResponseFileDownload(Status.OK, "application/octet-stream",
                    null, file, filePath, mCommand);
            // must set the content length attribute
            // the client receive file by content-length
            r.addHeader("Content-Length", "" + file.length());
            return r;
        }

        if (parms.containsKey("file") && method.equals(HTTPMethod.POST)) {
            myLog.e(parms.get("file") + "*****post***** return ok response");
            return new HTTPResponse(Status.OK, MIME_HTML, "");
        }

        if (method.equals(HTTPMethod.PUT) && files != null) {
            return new HTTPResponse("");
        }
        // bad request
        // we only deal the upload post and download get
        return super.serve(uri, method, headers, parms, files);
    }

    @Override
    public String saveFile(InputStream inputStream, String filePath, int size) throws IOException,
            Exception {
        myLog.e("SAVE_FILE -> Server begin save file ---> fileName = " + filePath + "  fileSize = "
                + size + " ------------" + Thread.currentThread());
        DataOutputStream outputFile = null;
        String fileLocalPath = "";

        // 实验室数据
        long dataReceiveReadNetTime = 0L;
        long dataReceiveReadNetSize = 0L;
        long dataReceiveWriteSDTime = 0L;
        long dataReceiveWriteSDSize = 0L;
        // --------------------------------
        try {
            File file = mCommand.getReceiveFileByFileName(filePath);
            if (file == null) {
                // FIXME 抛出自定义异常
                myLog.e("SAVE_FILE init file error --> file == null");
                throw new FileNotReadyException("SAVE_FILE init file error --> file == null");

            }
            fileLocalPath = file.getAbsolutePath();
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                    Const.FILE_TRANSFER_DOING));
            // -------------------------------------------------------------------
            outputFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            int readLength = 0;
            byte[] buffer = new byte[Const.BUFFER_SIZE];

            long currentTime = 0;
            long beforeTime = System.currentTimeMillis();
            int refreshLength = 0;
            int fileSize = size;

            while (readLength >= 0 && size > 0 && !Thread.interrupted()) {
                // FIXME 通过自定义异常取消文件传送
                if (!mCommand.isNeedReceiveThisFile(filePath)) {
                    throw new FileTransferCancelException(filePath);
                }
                // 实验室数据
                long dbrNet = System.currentTimeMillis();
                // --------------
                readLength = inputStream.read(buffer, 0, (int)Math.min(size, 100 * 1024));

                // 实验室数据
                if (readLength > 0) {
                    long darNet = System.currentTimeMillis();
                    dataReceiveReadNetTime += (darNet - dbrNet);
                    dataReceiveReadNetSize += readLength;
                }
                // -----------------------
                size -= readLength;
                if (readLength > 0) {

                    // 实验室数据
                    long dbwSD = System.currentTimeMillis();
                    // -------------------------------------
                    outputFile.write(buffer, 0, readLength);
                    // 实验室数据
                    long dawSD = System.currentTimeMillis();
                    dataReceiveWriteSDSize += readLength;
                    dataReceiveWriteSDTime += (dawSD - dbwSD);
                    // ------------------------------------
                }

                refreshLength += readLength;
                currentTime = System.currentTimeMillis();
                int timeLength = (int)(currentTime - beforeTime);
                if (timeLength > 1000) {
                    beforeTime = currentTime;
                    // ----------------------------------------------------------------------------------
                    ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getTransferingMessage(
                            fileLocalPath, refreshLength, timeLength, fileSize - size, fileSize));
                    // ----------------------------------------------------------------------------------
                    myLog.e("SERVER file http upload" + "------->" + filePath + " upload ---- > "
                            + refreshLength);
                    refreshLength = 0;
                }
            }
            if (size == 0) {
                myLog.e("SAVE_FILE -> Server finish save file " + file.getAbsolutePath());
                // -------------------------------------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                        Const.FILE_TRANSFER_COMPLETE));
                // -------------------------------------------------------------------
            } else {
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                        Const.FILE_TRANSFER_FAILURE));
            }
            outputFile.flush();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            myLog.e("SAVE_FILE -> Server save file not found");
            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_207);
            // ----------------------
            // 本地异常 不上报
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                    Const.FILE_TRANSFER_FAILURE));
            // -------------------------------------------------------------------
            throw e;
        } catch (Exception e) {
            myLog.e("SAVE_FILE -> Server save file error -->" + filePath);
            myLog.logException(e);
            // 异常上报
            if (e instanceof FileTransferCancelException) {
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                        Const.FILE_TRANSFER_CANCEL));
                //实验室数据
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_203);
                //---------------------------------------------------
            } else {
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(fileLocalPath, size,
                        Const.FILE_TRANSFER_FAILURE));
                //实验室数据
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_205);
                // -------------------------------------------------------------------
            }
            throw e;
        } finally {
            // 实验室数据
            LaboratoryData.gNetWorkReadSize += dataReceiveReadNetSize;
            LaboratoryData.gNetWorkReadTime += dataReceiveReadNetTime;
            LaboratoryData.gSDWriteSize += dataReceiveWriteSDSize;
            LaboratoryData.gSDWriteTime += dataReceiveWriteSDTime;
            LaboratoryData.gReceiveFileTotalSize += dataReceiveReadNetSize;
            LaboratoryData.gReceiveFileTotalTime += (dataReceiveWriteSDTime + dataReceiveReadNetTime);
            // ----------------------------
            // 关闭在这一块使用的file资源
            LogUtil.e("SAVE_FILE -> Server save file release file resource ");
            mCommand.removeReceiveFileByFileName(filePath);
            if (outputFile != null) {
                outputFile.close();
            }
        }

    }
}

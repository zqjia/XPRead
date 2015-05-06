
package com.xpread.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.xpread.transfer.exception.FileTransferCancelException;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;

public class HTTPResponseFileDownload extends HTTPResponse {
    private MyLog myLog = new MyLog(HTTPResponseFileDownload.class.getSimpleName());

    private File mFile;

    private String filePath;

    private HTTPServerCommand mHttpServerCommand;

    public HTTPResponseFileDownload(IStatus status, String mimeType, InputStream data, File file,
            String filePath, HTTPServerCommand httpServerCommand) {
        super(status, mimeType, data);
        this.mFile = file;
        this.filePath = filePath;
        this.mHttpServerCommand = httpServerCommand;
    }

    @Override
    protected void dealOuputStream(OutputStream outputStream, PrintWriter pw) throws Exception {

        int readLength = 0;
        int size = (int)mFile.length();
        super.sendContentLengthHeaderIfNotAlreadyPresent(pw, size);
        DataInputStream mDataInputStream = null;

        int fileSize = size;
        int refreshLength = 0;
        long currentTime = 0;
        long beforeTime = System.currentTimeMillis();

        // 实验室数据
        long dataSendReadSDTime = 0L;
        long dataSendReadSDSize = 0L;
        long dataSendWriteNetSize = 0L;
        long dataSendWriteNetTime = 0L;
        // --------------------------------------
        try {
            mDataInputStream = new DataInputStream(new FileInputStream(mFile));
            myLog.e("SERVER DOWNLOAD -> begin send file to client  ... filepath = " + filePath
                    + " size = " + fileSize);
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_DOING));
            // -------------------------------------------------------------------
            byte[] buffer = new byte[Const.BUFFER_SIZE];
            while (readLength >= 0 && size > 0) {

                if (!mHttpServerCommand.isNeedSendThisFile(filePath)) {
                    myLog.e(filePath + " is canceled by server");
                    throw new FileTransferCancelException(filePath);
                }

                // 实验室数据
                long dbrSD = System.currentTimeMillis();
                // ----------------------------
                readLength = mDataInputStream.read(buffer, 0, (int)Math.min(size, 100 * 1024));
                // 实验室数据
                if (readLength > 0) {
                    long darSD = System.currentTimeMillis();
                    dataSendReadSDTime += (darSD - dbrSD);
                    dataSendReadSDSize += readLength;
                }
                // ----------------------
                size -= readLength;
                if (readLength > 0) {
                    // 实验室数据
                    long dbwNet = System.currentTimeMillis();
                    // ---------------------------
                    outputStream.write(buffer, 0, readLength);
                    // 实验室数据
                    long dawNet = System.currentTimeMillis();
                    dataSendWriteNetTime += (dawNet - dbwNet);
                    dataSendWriteNetSize += readLength;
                    // ------------------------
                }

                refreshLength += readLength;
                // 更新速度
                currentTime = System.currentTimeMillis();
                int timeLength = (int)(currentTime - beforeTime);
                if (timeLength > 1000) {
                    beforeTime = currentTime;
                    // ----------------------------------------------------------------------------------
                    ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getTransferingMessage(filePath,
                            refreshLength, timeLength, fileSize - size, fileSize));
                    // ----------------------------------------------------------------------------------
                    refreshLength = 0;
                }
            }
            myLog.e("SERVER DOWNLOAD -> download success  ... filepath " + filePath);
            // -------------------------------------------------------------------

            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_COMPLETE));
            // -------------------------------------------------------------------
            outputStream.flush();
        } catch (Exception e) {
            myLog.logException(e);
            if (!(e instanceof FileTransferCancelException)) {
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                        Const.FILE_TRANSFER_FAILURE));
                // TODO
                // 发送文件失败
                // --------------------------------------------------
            } else {
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                        Const.FILE_TRANSFER_CANCEL));
                // TODO
                // 发送文件不取消
                // --------------------------------------------------
            }
            throw e;
        } finally {
            // 实验室数据
            LaboratoryData.gNetWorkWriteSize += dataSendWriteNetSize;
            LaboratoryData.gNetWorkWriteTime += dataSendWriteNetTime;
            LaboratoryData.gSDReadSize += dataSendReadSDSize;
            LaboratoryData.gSDReadTime += dataSendReadSDTime;
            LaboratoryData.gSendFileTotalSize += dataSendWriteNetSize;
            LaboratoryData.gSendFileTotalTime += (dataSendWriteNetTime + dataSendReadSDTime);
            // ------------------------
            mHttpServerCommand.removeSendFileByFilePath(filePath);
            if (mDataInputStream != null) {
                mDataInputStream.close();
            }
        }
    }
}

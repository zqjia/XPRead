
package com.xpread.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Bundle;
import android.os.Message;

import com.xpread.transfer.exception.FileNotReadyException;
import com.xpread.transfer.exception.FileTransferCancelException;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;

public class HTTPClientCommand {

    private MyLog myLog = new MyLog(HTTPClientCommand.class.getSimpleName());

    // 需要发送的文件
    private final ConcurrentLinkedQueue<String> mNeedSendFiles = new ConcurrentLinkedQueue<String>();

    // 正在发送的文件
    private final ConcurrentHashMap<String, String> mSendingFilesMap = new ConcurrentHashMap<String, String>();

    // 正在接收的文件
    private final ConcurrentHashMap<String, File> mReceiveFiles = new ConcurrentHashMap<String, File>();

    private ExcuteHttpConnection mExcuteHttpConnection;

    /**
     * 得到文件发送命令
     * 
     * @return
     */
    public byte[] getNeedSendFileTokenCommand() {
        if (mNeedSendFiles.size() == 0) {
            myLog.e(" mNeedSendFiles.size() == 0 ");
            return null;
        }
        String[] filePaths = null;
        synchronized (HTTPClientCommand.class) {
            filePaths = new String[mNeedSendFiles.size()];
            int i = 0;
            while (mNeedSendFiles.size() > 0) {
                filePaths[i] = mNeedSendFiles.remove();
                i++;
            }
        }
        int[] sizes = new int[filePaths.length];
        String[] fileNames = new String[filePaths.length];
        for (int j = 0; j < filePaths.length; j++) {
            if (mSendingFilesMap.containsKey(filePaths[j])) {
                sizes[j] = FileUtil.getFileSizeByName(filePaths[j]);
                fileNames[j] = mSendingFilesMap.get(filePaths[j]);
            } else {
                myLog.e("error-->when create send file token message , mSendingFilesMap not contain "
                        + filePaths[j]);
            }
        }
        return TokenCommand.createFileSendMessage(filePaths, sizes, fileNames, false);

    }

    /**
     * 处理文件发送命令
     * 
     * @param msg
     */
    public void dealFilesResponse(Message msg) {
        int messageType = (Integer)msg.obj;
        switch (messageType) {
            case TokenCommand.MESSAGE_TYPE_READY_FOR_RECEIVE_FILE: {
                Bundle bundle = msg.getData();
                if (bundle == null) {
                    return;
                }
                // -----------------------------------------------------------------------------
                // FIXME 测试代码
                List<String> filePath = bundle.getStringArrayList("path");
                List<Integer> fileState = bundle.getIntegerArrayList("state");
                Map<String, Integer> fileStateMap = new HashMap<String, Integer>();
                for (int i = 0; i < filePath.size(); i++) {
                    fileStateMap.put(filePath.get(i), fileState.get(i));
                }
                // --------------------------------------------------------------------------------

                if (mExcuteHttpConnection != null) {
                    for (String file : fileStateMap.keySet()) {
                        if (fileStateMap.get(file) == Const.FILE_TRANSFER_PREPARED
                                && mSendingFilesMap.containsKey(file)) {
                            final String tempFile = file;
                            mExcuteHttpConnection.excuteHttpPostConnection(tempFile);
                        } else if (fileStateMap.get(file) != Const.FILE_TRANSFER_PREPARED) {
                            // FIXME 如果接受方不需要接收，更新UI
                            myLog.e("peer not need receive " + file);
                            // ----------------------------------------------------------------------
                            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(file, 0,
                                    fileStateMap.get(file)));
                            // ----------------------------------------------------------------------
                            mSendingFilesMap.remove(file);
                        } else if (!mSendingFilesMap.containsKey(file)) {
                            myLog.e("why not send this file ????? mSendingFiles not contain  ---- "
                                    + mSendingFilesMap.toString() + "--------" + file);
                            mSendingFilesMap.remove(file);
                        }
                    }
                }

            }
                break;
            case TokenCommand.MESSAGE_TYPE_SEND_FILES: {
                Bundle bundle = msg.getData();
                if (bundle == null) {
                    myLog.e("why not receive this file ????? bundle = null ");
                    return;
                }

                List<String> filePath = bundle.getStringArrayList("path");
                if (filePath.size() == 0) {
                    myLog.e("why not receive this file ????? filePath.size = 0 ");
                    return;
                }
                if (mExcuteHttpConnection != null) {
                    for (int i = 0; i < filePath.size(); i++) {
                        final String file = filePath.get(i);
                        if (mReceiveFiles.containsKey(file)) {
                            mExcuteHttpConnection.excuteHttpGetConnection(file,
                                    mReceiveFiles.get(file));
                        } else {
                            myLog.e("why not receive this file ????? mReceiveFiles -->"
                                    + mReceiveFiles.toString() + " -----not contain" + file);
                        }
                    }
                }

            }
                break;

        }
    }

    // FIXME connect由谁控制
    /**
     * upload a file through post method
     * 
     * @param requestUrl the server url
     * @param file the file you want to upload
     * @return
     * @throws Exception
     */
    public String postUseUrlConnection(HttpURLConnection con, String filePath) throws Exception {

        // 数据统计临时变量---------------------------------------------
        // long dataBeforeReadSDTime = 0L;
        // long dataAfterReadSDTime = 0L;
        long dataTotalReadSDTime = 0L;
        long dataTotalReadSDSize = 0L;

        // long dataBeforeWriteNetTime = 0L;
        // long dataAfterWriteNetTime = 0L;
        long dataTotalWriteNetTime = 0L;
        long dataTotalWriteNetSize = 0L;
        // --------------------------------------------------------
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int size = 0;
        StringBuffer sb = new StringBuffer();

        FileInputStream fis = null;
        DataOutputStream ds = null;
        DataInputStream di = null;

        try {
            File file = new File(filePath);
            fis = new FileInputStream(file);
            StringBuilder fileHeadSB = new StringBuilder();
            fileHeadSB.append(twoHyphens + boundary + end);
            fileHeadSB.append("Content-Disposition: form-data; ");
            fileHeadSB.append("name=\"file\";filename=\"" + URLEncoder.encode(filePath, "utf-8")
                    + "\"" + end);
            fileHeadSB.append("Content-type: application/octet-stream");
            fileHeadSB.append(end + end);
            String fileEnd = end + twoHyphens + boundary + twoHyphens + end;
            size = (int)file.length() + fileHeadSB.toString().getBytes().length
                    + fileEnd.getBytes().length;
            // set the fixed length to let the con know the length of your
            // content
            con.setFixedLengthStreamingMode(size);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            // set post head
            con.setRequestMethod("POST");
            con.setRequestProperty("Connection", "Close");
            System.setProperty("http.keepAlive", "false");
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            // don't use zip format , the actual length may be different from
            // the content-length
            con.setRequestProperty("Accept-Encoding", "identity");

            try {
                ds = new DataOutputStream(new BufferedOutputStream(con.getOutputStream()));
            } catch (IOException e) {
                // 实验室数据
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_101);
                // -----------------------------
                throw e;
            }
            ds.writeBytes(fileHeadSB.toString());
            ds.flush();
            di = new DataInputStream(new BufferedInputStream(fis));
            // TODO
            // 文件上传发送方buffer大小--------------------------------------------------------------
            // -------------------------------------------------------------------------------------
            byte[] buffer = new byte[Const.BUFFER_SIZE];
            int length = 0;
            // TODO
            // 文件上传发送方文件大小--------------------------------------------------------------
            // -------------------------------------------------------------------------------------
            size = (int)file.length();
            int fileSize = size;

            int refreshLength = 0;
            myLog.e("CLIENT file http begin upload" + "------->" + filePath + "   size = " + size
                    + " thread = " + Thread.currentThread().getName());

            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_DOING));

            long beforeTime = System.currentTimeMillis();
            long currentTime = 0l;
            while (!Thread.interrupted() && length != -1) {

                if (!mSendingFilesMap.containsKey(filePath)) {
                    myLog.e("cancel " + filePath);
                    throw new FileTransferCancelException(filePath);
                }

                /*
                 * TODO
                 * 文件上传发送方读取文件速度--------------------------------------------
                 */
                // 实验室数据
                long dbrSD = System.currentTimeMillis();
                // end----------------------------
                length = di.read(buffer);
                // 实验室数据-------------------------
                if (length != -1) {
                    long darSD = System.currentTimeMillis();
                    dataTotalReadSDSize += length;
                    dataTotalReadSDTime += (darSD - dbrSD);
                }
                // end ----------------------------

                if (length > 0) {
                    /*
                     * TODO
                     * 文件上传发送方写入网络速度--------------------------------------------
                     */
                    // 实验室数据
                    long dbwNET = System.currentTimeMillis();
                    // end---------------------------------
                    size -= length;
                    ds.write(buffer, 0, length);
                    // 实验室数据
                    long dawNET = System.currentTimeMillis();
                    dataTotalWriteNetSize += length;
                    dataTotalWriteNetTime += (dawNET - dbwNET);
                    // end---------------------------------
                }

                refreshLength += length;
                // 更新速度
                currentTime = System.currentTimeMillis();
                int timeLength = (int)(currentTime - beforeTime);
                if (timeLength > 1000) {
                    beforeTime = currentTime;
                    // ----------------------------------------------------------------------------------
                    ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getTransferingMessage(filePath,
                            refreshLength, timeLength, fileSize - size, fileSize));
                    refreshLength = 0;
                }

            }
            ds.writeBytes(fileEnd);
            ds.flush();
            myLog.e("CLIENT finish upload file ----> " + filePath);
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, length,
                    Const.FILE_TRANSFER_COMPLETE));
            // TODO
            // 文件成功率--------------------------------------------------------------
            // LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_SUCCESS_FILES);
            // LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_SUCCESS_FILE);
            // -------------------------------------------------------------------------------------
            safeClose(ds);
            // 让TCP socket能正常回收
            try {
                if (con.getResponseCode() == 200) {
                    // do nothing
                    myLog.e("read respone == 200 " + filePath);
                } else {
                    myLog.e("read respone != 200 " + filePath);
                }
            } catch (IOException e) {
                // TODO
                // 实验室数据 ----------
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_103);
                // --------------------------------------
                myLog.e(filePath + " -- IOException ");
                myLog.logException(e);

                InputStream es = null;
                try {
                    es = ((HttpURLConnection)con).getErrorStream();
                    // read the response body
                    if (es != null) {
                        while ((es.read(buffer)) > 0) {
                        }
                    }
                    // close the errorstream

                } catch (Exception ex) {
                    // 这个异常没有必要处理，因为文件已经发送成功了。
                    myLog.e(filePath + " -- timeout ");
                    myLog.logException(ex);
                } finally {
                    safeClose(es);
                }
            }
            return sb.toString();
        } catch (Exception e) {

            myLog.e("Client ERROR --> fileNams = " + filePath + " file reset size = " + size);

            if (!(e instanceof FileTransferCancelException)) {
                // TODO
                // 实验室市局文件传送错误
                if (e instanceof FileNotFoundException) {
                    LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_104);
                    // -------------------------------------
                } else {
                    LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_107);
                }
                // LaboratoryData
                // .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_FAIL_FILES);
                // LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_FAIL_FILE);
                // -------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                        Const.FILE_TRANSFER_FAILURE));
            } else {
                // TODO
                // 文件传送错误（105）
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_105);
                // LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_FAIL_FILE);
                // LaboratoryData
                // .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_FAIL_FILES);
                // -------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                        Const.FILE_TRANSFER_CANCEL));

            }
            // -------------------------------------------------------------------
            throw e;
        } finally {
            // TODO
            // 实验室数据传送过程中的速度（包含失败文件）---------------------------------------------------------------
            LaboratoryData.gSDReadSize += dataTotalReadSDSize;
            LaboratoryData.gSDReadTime += dataTotalReadSDTime;
            LaboratoryData.gNetWorkWriteSize += dataTotalWriteNetSize;
            LaboratoryData.gNetWorkWriteTime += dataTotalWriteNetTime;

            LaboratoryData.gSendFileTotalSize += dataTotalWriteNetSize;
            LaboratoryData.gSendFileTotalTime += (dataTotalWriteNetTime + dataTotalReadSDTime);
            // LaboratoryData.gTotalSize += dataTotalReadSDSize;
            // LaboratoryData.gTotalTime += (dataTotalWriteNetTime +
            // dataTotalReadSDTime);
            // -------------------------------------------------------------------------------------
            mSendingFilesMap.remove(filePath);
            // end
            safeClose(ds);

            safeClose(di);
            safeClose(fis);
        }
    }

    /**
     * download a file through get method
     * 
     * @param requestUrl the server url
     * @param file the file you want to save the download file
     * @return
     * @throws Exception
     */
    public void downloadFile(HttpURLConnection con, String requestFileName, File file)
            throws Exception {
        if (!mReceiveFiles.containsKey(requestFileName)) {
            throw new FileNotReadyException("request file " + requestFileName + " not ready ");
        }
        try {
            con.setDoInput(true);
            // must set the do output false , otherwise the method will be set
            // get
            con.setDoOutput(false);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            System.setProperty("http.keepAlive", "false");
            String s = con.getHeaderField("content-length");
            if (con.getResponseCode() != 200) {
                // TODO
                // 实验室数据文件接收（205）
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_204);
                // ------------------------
                return;
            }
            InputStream inputStream = con.getInputStream();
            if (s == null || s.length() == 0) {
                // listener the error response
                return;
            }
            int size = Integer.parseInt(s);
            saveFile(inputStream, requestFileName, file, size);
        } catch (Exception e) {
            throw e;
        } finally {
            mReceiveFiles.remove(requestFileName);
        }
    }

    private void saveFile(InputStream inputStream, String requestFileName, File file, int size)
            throws Exception {
        DataOutputStream outputFile = null;
        String filePath = "";

        long beforeTime = 0;
        long currentTime = 0;
        int refreshLength = 0;
        int fileSize = size;

        // 实验室数据
        long dataReadNetTime = 0L;
        long dataReadNetSize = 0L;
        long dataWriteSDTime = 0L;
        long dataWriteSDSize = 0L;
        // ---------------------------------------
        try {
            outputFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            int readLength = 0;
            byte[] buffer = new byte[Const.BUFFER_SIZE];
            filePath = file.getAbsolutePath();
            myLog.e("Client begin save FILE ----> " + filePath);
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_DOING));
            // -------------------------------------------------------------------
            while (!Thread.interrupted() && readLength >= 0 && size > 0) {
                if (!mReceiveFiles.containsKey(requestFileName)) {
//                    Log.e(HTTPClientCommand.class.getSimpleName(), " mReceiveFiles - > "
//                            + mReceiveFiles.toString() + " filePath - > " + requestFileName);
                    throw new FileTransferCancelException(filePath);
                }
                // 实验室数据
                long drbNET = System.currentTimeMillis();
                // -----------------------------------------------------------------------
                readLength = inputStream.read(buffer, 0, (int)Math.min(size, 100 * 1024));
                // 实验室数据
                if (readLength > 0) {
                    long draNET = System.currentTimeMillis();
                    dataReadNetSize += readLength;
                    dataReadNetTime += (draNET - drbNET);
                }
                // -----------------------------------------------------------------------
                size -= readLength;
                if (readLength > 0) {
                    // 实验室数据
                    long dwbSD = System.currentTimeMillis();
                    outputFile.write(buffer, 0, readLength);
                    long dwaSD = System.currentTimeMillis();
                    dataWriteSDSize += readLength;
                    dataWriteSDTime += (dwaSD - dwbSD);
                }

                refreshLength += readLength;
                currentTime = System.currentTimeMillis();
                long timeLength = currentTime - beforeTime;
                if (timeLength > 1000) {
                    beforeTime = currentTime;
                    // ----------------------------------------------------------------------------------
                    ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getTransferingMessage(filePath,
                            refreshLength, (int)timeLength, fileSize - size, fileSize));
                    // ----------------------------------------------------------------------------------
                    LogUtil.e("CLIENT file http saving" + "------->" + filePath + " save ---- > "
                            + refreshLength);
                    refreshLength = 0;
                }
            }
            if (size == 0) {
                myLog.e("Client  finish save FILE ----> " + filePath + " -----> success");
                // -------------------------------------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                        Const.FILE_TRANSFER_COMPLETE));
                // TODO
                // 文件完成接收
                // ----------------------------------------
                // -------------------------------------------------------------------
            }
            outputFile.flush();
        } catch (FileNotFoundException e) {
            myLog.e("Client  save FILE ----> " + filePath + " -----> error");
            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_207);
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_FAILURE));
            throw e;
        } catch (IOException e) {
            myLog.e("Client  save FILE ----> " + filePath + " -----> error");
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_FAILURE));
            // TODO
            // 其他错误(205)
            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_205);
            // -------------------------------------------------------------------
            throw e;
        } catch (FileTransferCancelException e) {
            // -------------------------------------------------------------------
            ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(filePath, size,
                    Const.FILE_TRANSFER_CANCEL));

            // TODO
            // 接收文件取消错误(203)
            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_203);
            // -------------------------------------------------------------------
            throw e;
        } finally {
            // 实验室数据
            LaboratoryData.gNetWorkReadSize += dataReadNetSize;
            LaboratoryData.gNetWorkReadTime += dataReadNetTime;
            LaboratoryData.gSDWriteSize += dataWriteSDSize;
            LaboratoryData.gSDWriteTime += dataWriteSDTime;
            LaboratoryData.gReceiveFileTotalSize += dataReadNetSize;
            LaboratoryData.gReceiveFileTotalTime += (dataReadNetTime + dataWriteSDTime);
            // ------------------------------------
            mReceiveFiles.remove(filePath);
            if (outputFile != null) {
                outputFile.close();
            }
        }

    }

    protected static final void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    public interface ExcuteHttpConnection {
        public void excuteHttpPostConnection(String filePath);

        public void excuteHttpGetConnection(String requestFilePath, File localSaveFile);
    }

    /**
     * @param mExcuteHttpConnection the mExcuteHttpConnection to set
     */
    public final void setExcuteHttpConnection(ExcuteHttpConnection mExcuteHttpConnection) {
        this.mExcuteHttpConnection = mExcuteHttpConnection;
    }

    public final void unRegistExcuteHttpConnection(ExcuteHttpConnection excuteHttpConnection) {
        if (mExcuteHttpConnection == excuteHttpConnection) {
            mExcuteHttpConnection = null;
        }
    }

    public byte[] getRecevieFilesTokenResponse(Message msg) {
        Bundle bundle = msg.getData();
        if (bundle == null) {
            return null;
        }
        // -------------------------------------------------------------------------
        // FIXME 测试代码
        ArrayList<String> filePath = bundle.getStringArrayList("path");
        ArrayList<String> fileName = bundle.getStringArrayList("name");
        ArrayList<Integer> fileSize = bundle.getIntegerArrayList("size");
        Map<String, Integer> fileSizeMap = new HashMap<String, Integer>();
        Map<String, Integer> fileStateMap = new HashMap<String, Integer>();

        ArrayList<String> needRecevieFiles = new ArrayList<String>();
        // UI更新
        ArrayList<String> loaclUIPath = new ArrayList<String>();
        ArrayList<Integer> localUIState = new ArrayList<Integer>();

        // 对接收文件过滤，只接收需要接收的文件
        for (int i = 0; i < filePath.size(); i++) {
            fileSizeMap.put(filePath.get(i), fileSize.get(i));
            if (FileUtil.isFileExist(fileName.get(i), fileSize.get(i))) {
                fileStateMap.put(filePath.get(i), Const.FILE_TRANSFER_COMPLETE);
                // ---------------------------------------------------------
                // 如果本地文件已经存在
                loaclUIPath.add(FileUtil.getPathByName(fileName.get(i), fileSize.get(i)));
                localUIState.add(Const.FILE_TRANSFER_COMPLETE);
                // TODO
                // 接收文件成功
                // ------------------------------------------------------------
                // -----------------------------------------------------------------
            } else {
                if (!mReceiveFiles.containsKey(filePath.get(i))) {
                    // 用于发送文件get请求列表
                    needRecevieFiles.add(filePath.get(i));
                    mReceiveFiles.put(filePath.get(i), FileUtil.getFileByName(fileName.get(i)));
                    fileStateMap.put(filePath.get(i), Const.FILE_TRANSFER_PREPARED);
                } else {
                    // 如果文件不需要再次接收
                    fileStateMap.put(filePath.get(i), Const.FILE_TRANSFER_DOING);
                }
                loaclUIPath.add(mReceiveFiles.get(filePath.get(i)).getAbsolutePath());
                localUIState.add(Const.FILE_TRANSFER_PREPARED);
            }
        }

        bundle.putStringArrayList("path", needRecevieFiles);
        // ----------------------------------------------------------------------------
        ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getReceiveFilesInformation(loaclUIPath,
                fileSize, localUIState));
        // -----------------------------------------------------------------------------
        return TokenCommand.createFileResponseMessage(fileSizeMap, fileStateMap, false);
    }

    public final void addNeedSendFiles(List<String> filePaths, List<String> fileNames) {
        if (filePaths == null || fileNames == null) {
            return;
        }
        if (filePaths.size() == 0 || fileNames.size() == 0 || fileNames.size() != filePaths.size()) {
            return;
        }
        for (int i = 0; i < filePaths.size(); i++) {
            String file = filePaths.get(i);
            String name = fileNames.get(i);
            // 需要发和正在发的文件链表都没有，则添加
            if (!mNeedSendFiles.contains(file) && !mSendingFilesMap.containsKey(file)) {
                mNeedSendFiles.add(file);
                mSendingFilesMap.put(file, name);
            } else {
                myLog.e(file + " is sending ,can not send again");
                // TODO
                // 文件传送错误，文件正在传送（107）
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_106);
                // ------------------------------
            }
        }
        myLog.e("addNeedSendFiles -- > " + mNeedSendFiles.toString() + mSendingFilesMap.toString());
    }

    public final int getNeedSendFilesSize() {
        return mNeedSendFiles.size();
    }

    public final boolean isFileNeedSend() {
        if (mNeedSendFiles.size() > 0) {
            return true;
        }
        return false;
    }

    public final boolean cancelFile(String filePath) {
        if (mSendingFilesMap.containsKey(filePath)) {
            mSendingFilesMap.remove(filePath);
            return true;
        } else {
            String cancelKey = null;
            for (String key : mReceiveFiles.keySet()) {
                if (mReceiveFiles.get(key).getAbsolutePath().equals(filePath)) {
                    cancelKey = key;
                    break;
                }
            }
            if (cancelKey != null) {
                mReceiveFiles.remove(cancelKey);
                return true;
            } else {
                myLog.e(filePath + " can not be cancel because not contain in receive file list");
            }
            return false;
        }

    }

    public String getChanelFilePath(String filePath) {
        if (mSendingFilesMap.containsKey(filePath)) {
            return filePath;
        }
        File f = new File(filePath);
        if (mReceiveFiles.contains(f)) {
            for (String fp : mReceiveFiles.keySet()) {
                if (f.equals(mReceiveFiles.get(fp))) {
                    return fp;
                }
            }
        }
        myLog.e("发送取消请求，但没有发现需要的文件" + filePath);
        return null;
    }

    public String getLocalFilePath(String filePath) {
        if (mSendingFilesMap.containsKey(filePath)) {
            return filePath;
        }

        if (mReceiveFiles.containsKey(filePath)) {
            return mReceiveFiles.get(filePath).getAbsolutePath();
        }
        myLog.e("接收到取消请求后没有发现需要的文件" + filePath);
        return null;
    }

    /**
     * 获得遗留没有完成传送的文件
     * 
     * @return
     */
    public ArrayList<String> getReserveFiles() {
        ArrayList<String> list = new ArrayList<String>();
        for (String file : mSendingFilesMap.keySet()) {
            if (file != null) {
                list.add(file);
            }
        }
        for (String file : mReceiveFiles.keySet()) {
            String f = mReceiveFiles.get(file).getAbsolutePath();
            list.add(f);
        }
        return list;
    }

    public void clear() {
        mSendingFilesMap.clear();
        mReceiveFiles.clear();
        mNeedSendFiles.clear();
    }

}

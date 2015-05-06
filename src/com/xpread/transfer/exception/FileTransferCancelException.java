
package com.xpread.transfer.exception;

/**
 * 取消文件传送异常
 */
public class FileTransferCancelException extends Exception {

    private static final long serialVersionUID = 1L;

    public FileTransferCancelException(String message) {
        super(message);
    }
}

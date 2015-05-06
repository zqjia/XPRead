
package com.xpread.transfer.exception;

/**
 * 在消息传递中文件信息没有本地准备好
 */
public class FileNotReadyException extends Exception {
    private static final long serialVersionUID = 1L;

    public FileNotReadyException(String message) {
        super(message);
    }
}

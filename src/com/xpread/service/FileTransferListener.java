
package com.xpread.service;

import java.util.List;

public interface FileTransferListener {
    /**
     * refresh file transfer state by fileUrl
     * 
     * @param fileUrl the loacl path of the file
     * @param state </br>10 File prepared </br>11 File is transmitted </br>12
     *            File transfer complete </br>13 File transfer failure
     * @param fileSize the size of the file
     */
    public void fileStateChangeListener(String filePath, int state, int fileSize);

    // /**
    // * refresh file transfer progress by fileUrl , the shortest time interval
    // is
    // * half second
    // *
    // * @param fileUrl the loacl path of the file
    // * @param progress file transfer progress
    // * @param max file transfer max progress
    // */
    // public void fileProgressChangeListener(String filePath, int progress, int
    // fileSize);

    // public void filesReceiveListener(String[] filePath, int[] state);

    /**
     * refresh file transfer speed by fileUrl , the shortest time interval is
     * half second
     * 
     * @param filePath
     * @param speed Speed is a unit of byte every second
     */
    public void fileTranferingListener(String filePath, int speed, int progress, int fileSize);

    public void fileReceiveListener(List<String> files, List<Integer> fileSizes,
            List<Integer> fileStatus);
}

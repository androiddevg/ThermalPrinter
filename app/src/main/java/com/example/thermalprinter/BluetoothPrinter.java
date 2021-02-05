package com.example.thermalprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class BluetoothPrinter {
    private static BluetoothPrinter mBluetoothPrintDriver;
    private final String TAG = "BluetoothPrinter";
    private final boolean D = true;
    private final String NAME = "BluetoothPrinter";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState = 0;

    private BluetoothPrinter() {
    }

    public static BluetoothPrinter getInstance() {
        if (mBluetoothPrintDriver == null) {
            mBluetoothPrintDriver = new BluetoothPrinter();
        }

        return mBluetoothPrintDriver;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    private void sendMessageToMainThread(int flag) {
        if (mHandler != null) {
            Message message = mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            message.setData(data);
            mHandler.sendMessage(message);
        }

    }

    private void sendMessageToMainThread(int flag, int state) {
        if (mHandler != null) {
            Message message = mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            data.putInt("state", state);
            message.setData(data);
            mHandler.sendMessage(message);
        }

    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        switch (mState) {
            case 0:
                sendMessageToMainThread(1, 16);
            case 1:
            case 2:
            default:
                break;
            case 3:
                sendMessageToMainThread(1, 17);
        }

    }

    public synchronized void start() {
        Log.d(TAG, "start");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

        setState(1);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        if (mState == 2 && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(2);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        sendMessageToMainThread(4);
        setState(3);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(0);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != 3) {
                return;
            }

            r = mConnectedThread;
        }

        r.write(out);
    }

    public void write2(byte[] out) throws IOException {
        ConnectedThread r;
        synchronized (this) {
            if (mState != 3) {
                return;
            }

            r = mConnectedThread;
        }

        for (int i = 0; i < out.length; ++i) {
            r.mmOutStream.write(out[i]);
        }

    }

    public void BT_Write(String dataString) {
        byte[] data = null;
        if (mState == 3) {
            ConnectedThread r = mConnectedThread;

            try {
                data = dataString.getBytes("GBK");
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            r.write(data);
        }
    }


    public void BT_Write(byte[] out) {
        if (mState == 3) {
            ConnectedThread r = mConnectedThread;
            r.write(out);
        }
    }

    public void BT_Write(byte[] out, int dataLen) {
        if (mState == 3) {
            ConnectedThread r = mConnectedThread;
            r.write(out, dataLen);
        }
    }

    public boolean IsNoConnection() {
        return mState != 3;
    }

    public boolean InitPrinter() {
        byte[] combyte = new byte[]{27, 64};
        if (mState != 3) {
            return false;
        } else {
            BT_Write(combyte);
            return true;
        }
    }

    public void WakeUpPritner() {
        byte[] b = new byte[3];

        try {
            BT_Write(b);
            Thread.sleep(100L);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public void SetDefaultSetting() {
        BT_Write(new byte[]{27, 33, 0});
    }

    public void Begin() {
        WakeUpPritner();
        InitPrinter();
    }

    public void LF() {
        byte[] cmd = new byte[]{13};
        BT_Write(cmd);
    }

    public void CR() {
        byte[] cmd = new byte[]{10};
        BT_Write(cmd);
    }

    public void SelftestPrint() {
        byte[] cmd = new byte[]{18, 84};
        BT_Write(cmd, 2);
    }

    public void Beep(byte times, byte time) {
        byte[] cmd = new byte[]{27, 66, times, time};
        BT_Write(cmd, 4);
    }

    public void StatusInquiry() {
        byte[] cmd = new byte[]{16, 4, -2};
        BT_Write(cmd, 3);
        byte[] cmd1 = new byte[]{16, 4, -1};
        BT_Write(cmd1, 3);
    }

    public void SetRightSpacing(byte Distance) {
        byte[] cmd = new byte[]{27, 32, Distance};
        BT_Write(cmd);
    }

    public void SetAbsolutePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 36, nL, nH};
        BT_Write(cmd);
    }

    public void SetRelativePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 92, nL, nH};
        BT_Write(cmd);
    }

    public void SetDefaultLineSpacing() {
        byte[] cmd = new byte[]{27, 50};
        BT_Write(cmd);
    }

    public void SetLineSpacing(byte LineSpacing) {
        byte[] cmd = new byte[]{27, 51, LineSpacing};
        BT_Write(cmd);
    }

    public void SetLeftStartSpacing(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 76, nL, nH};
        BT_Write(cmd);
    }

    public void SetAreaWidth(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 87, nL, nH};
        BT_Write(cmd);
    }

    public void SetCharacterPrintMode(byte CharacterPrintMode) {
        byte[] cmd = new byte[]{27, 33, CharacterPrintMode};
        BT_Write(cmd);
    }

    public void SetUnderline(byte UnderlineEn) {
        byte[] cmd = new byte[]{27, 45, UnderlineEn};
        BT_Write(cmd);
    }

    public void SetBold(byte BoldEn) {
        byte[] cmd = new byte[]{27, 69, BoldEn};
        BT_Write(cmd);
    }

    public void SetCharacterFont(byte Font) {
        byte[] cmd = new byte[]{27, 77, Font};
        BT_Write(cmd);
    }

    public void SetRotate(byte RotateEn) {
        byte[] cmd = new byte[]{27, 86, RotateEn};
        BT_Write(cmd);
    }

    public void SetAlignMode(byte AlignMode) {
        byte[] cmd = new byte[]{27, 97, AlignMode};
        BT_Write(cmd);
    }

    public void SetInvertPrint(byte InvertModeEn) {
        byte[] cmd = new byte[]{27, 123, InvertModeEn};
        BT_Write(cmd);
    }

    public void SetFontEnlarge(byte FontEnlarge) {
        byte[] cmd = new byte[]{29, 33, FontEnlarge};
        BT_Write(cmd);
    }

    public void SetBlackReversePrint(byte BlackReverseEn) {
        byte[] cmd = new byte[]{29, 66, BlackReverseEn};
        BT_Write(cmd);
    }

    public void SetChineseCharacterMode(byte ChineseCharacterMode) {
        byte[] cmd = new byte[]{28, 33, ChineseCharacterMode};
        BT_Write(cmd);
    }

    public void SelChineseCodepage() {
        byte[] cmd = new byte[]{28, 38};
        BT_Write(cmd);
    }

    public void CancelChineseCodepage() {
        byte[] cmd = new byte[]{28, 46};
        BT_Write(cmd);
    }

    public void SetChineseUnderline(byte ChineseUnderlineEn) {
        byte[] cmd = new byte[]{28, 45, ChineseUnderlineEn};
        BT_Write(cmd);
    }


    public void CutPaper() {
        byte[] cmd = new byte[]{27, 105};
        BT_Write(cmd);
    }

    public void PartialCutPaper() {
        byte[] cmd = new byte[]{27, 109};
        BT_Write(cmd);
    }

    public void FeedAndCutPaper(byte CutMode) {
        byte[] cmd = new byte[]{29, 86, CutMode};
        BT_Write(cmd);
    }


    public void printParameterSet(byte[] buf) {
        BT_Write(buf);
    }

    public void printByteData(byte[] buf) {
        BT_Write(buf);
        BT_Write(new byte[]{10});
    }

    public void printImage(Bitmap bitmap) {
        Bitmap newBm = BitmapUtils.decodeSampledBitmapFromBitmap(bitmap, 384);
        byte xL = (byte) (((newBm.getWidth() - 1) / 8 + 1) % 256);
        byte xH = (byte) (((newBm.getWidth() - 1) / 8 + 1) / 256);
        byte yL = (byte) (newBm.getHeight() % 256);
        byte yH = (byte) (newBm.getHeight() / 256);
        Log.d(TAG, "xL = " + xL);
        Log.d(TAG, "xH = " + xH);
        Log.d(TAG, "yL = " + yL);
        Log.d(TAG, "yH = " + yH);
        byte[] pixels = BitmapUtils.convert(newBm);
        BT_Write(new byte[]{29, 118, 48, 0, xL, xH, yL, yH});
        BT_Write(pixels);
        BT_Write(new byte[]{10});
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(TAG, MY_UUID);
            } catch (IOException var4) {
                Log.e(TAG, "listen() failed", var4);
            }

            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            while (mState != 3) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException var6) {
                    Log.e(TAG, "accept() failed", var6);
                    break;
                }

                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                            case 0:
                            case 3:
                                try {
                                    socket.close();
                                } catch (IOException var4) {
                                    Log.e(TAG, "Could not close unwanted socket", var4);
                                }
                                break;
                            case 1:
                            case 2:
                                connected(socket, socket.getRemoteDevice());
                        }
                    }
                }
            }

            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);

            try {
                mmServerSocket.close();
            } catch (IOException var2) {
                Log.e(TAG, "close() of server failed", var2);
            }

        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException var5) {
                Log.e(TAG, "create() failed", var5);
            }

            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException var5) {
                sendMessageToMainThread(2);

                try {
                    mmSocket.close();
                } catch (IOException var3) {
                    Log.e(TAG, "unable to close() socket during connection failure", var3);
                }
                try {
                    start();
                } catch (IllegalThreadStateException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            synchronized (this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException var2) {
                Log.e(TAG, "close() of connect socket failed", var2);
            }

        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException var6) {
                Log.e(TAG, "temp sockets not created", var6);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            while (true) {
                try {
                    while (true) {
                        if (mmInStream.available() != 0) {
                            for (int i = 0; i < 3; ++i) {
                                buffer[i] = (byte) mmInStream.read();
                            }
                        }
                    }
                } catch (IOException var3) {
                    Log.e(TAG, "disconnected", var3);
                    return;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException var3) {
                Log.e(TAG, "Exception during write", var3);
            }

        }

        public void write(byte[] buffer, int dataLen) {
            try {
                for (int i = 0; i < dataLen; ++i) {
                    mmOutStream.write(buffer[i]);
                }
            } catch (IOException var4) {
                Log.e(TAG, "Exception during write", var4);
            }

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException var2) {
                Log.e(TAG, "close() of connect socket failed", var2);
            }

        }
    }
}

package com.gmail.at.moicjarod;

import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.RuntimeErrorAlert;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.SdkLevel;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.os.StrictMode;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;

@DesignerComponent(version = 4,
  description = "Non-visible component that provides client socket connectivity.",
  category = ComponentCategory.EXTENSION,
  nonVisible = true,
  iconName = "http://jr.letertre.free.fr/Projets/AIClientSocket/clientsocket.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public class ClientSocketAI2Ext extends AndroidNonvisibleComponent implements Component {
    private static final String LOG_TAG = "ClientSocketAI2Ext";
    private final Activity activity;
    private Socket clientSocket = null;
    private String serverAddress = "";
    private String serverPort = "";
    private boolean connectionState = false;
    private boolean hexaStringMode = false;
    private boolean debugMessages = true;
    InputStream inputStream = null;

    public ClientSocketAI2Ext(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The address of the server the client will connect to.")
    public String ServerAddress() {
        return serverAddress;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    public void ServerAddress(String address) {
        serverAddress = address;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The port of the server the client will connect to.")
    public String ServerPort() {
        return serverPort;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    public void ServerPort(String port) {
        serverPort = port;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The state of the connection - true = connected, false = disconnected")
    public boolean ConnectionState() {
        return connectionState;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\\n\\r\" sequence")
    public String SeqNewLineAndRet() {
        return "\n\r";
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\\r\\n\" sequence")
    public String SeqRetAndNewLine() {
        return "\r\n";
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\\r\" sequence")
    public String SeqRet() {
        return "\r";
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\\n\" sequence")
    public String SeqNewLine() {
        return "\n";
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The mode of sending and receiving data.")
    public boolean HexaStringMode() {
        return hexaStringMode;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
    @SimpleProperty
    public void HexaStringMode(boolean mode) {
        hexaStringMode = mode;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The display of debug messages.")
    public boolean DebugMessages() {
        return debugMessages;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue="True")
    @SimpleProperty
    public void DebugMessages(boolean displayDebugMessages) {
        debugMessages = displayDebugMessages;
    }

    @SimpleFunction(description = "Tries to connect to the server and launches the thread for receiving data (blocking until connected or failed)")
    public void Connect() {
        if (connectionState) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionError("Socket allaqachon ulangan, qayta ulanishdan oldin ulanishni uzish kerak!");
                }
            });
            return;
        }
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(serverAddress, Integer.parseInt(serverPort)), 5000);
            connectionState = true;

            AsynchUtil.runAsynchronously(new Runnable() {
                @Override
                public void run() {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;

                    try {
                        inputStream = clientSocket.getInputStream();
                        while (true) {
                            bytesRead = inputStream.read(buffer);
                            if (bytesRead == -1) break;

                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                            final String dataReceived;
                            if (hexaStringMode == false) {
                                dataReceived = byteArrayOutputStream.toString("UTF-8");
                            } else {
                                int i;
                                char hexaSymbol1, hexaSymbol2;
                                String tempData = "";
                                byte[] byteArray = byteArrayOutputStream.toByteArray();
                                for (i = 0; i < byteArrayOutputStream.size(); i++) {
                                    if (((byteArray[i] & 0xF0) >> 4) < 0xA)
                                        hexaSymbol1 = (char)(((byteArray[i] & 0xF0) >> 4) + 0x30);
                                    else
                                        hexaSymbol1 = (char)(((byteArray[i] & 0xF0) >> 4) + 0x37);
                                    if ((byteArray[i] & 0x0F) < 0xA)
                                        hexaSymbol2 = (char)((byteArray[i] & 0x0F) + 0x30);
                                    else
                                        hexaSymbol2 = (char)((byteArray[i] & 0x0F) + 0x37);
                                    tempData = tempData + hexaSymbol1 + hexaSymbol2;
                                }
                                dataReceived = tempData;
                            }
                            byteArrayOutputStream.reset();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DataReceived(dataReceived);
                                }
                            });
                        }
                        if (connectionState) {
                            Disconnect();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    RemoteConnectionClosed();
                                }
                            });
                        }
                    } catch (SocketException e) {
                        final String errorMessage = "Ulanish xatosi: " + e.getMessage();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ConnectionError(errorMessage);
                            }
                        });
                    } catch (IOException e) {
                        final String errorMessage = "Ulanish xatosi: " + e.getMessage();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ConnectionError(errorMessage);
                            }
                        });
                    } catch (Exception e) {
                        final String errorMessage = "Xatolik: " + e.getMessage();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ConnectionError(errorMessage);
                            }
                        });
                    }
                }
            });
        } catch (SocketException e) {
            final String errorMessage = "Ulanish xatosi: " + e.getMessage();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionError(errorMessage);
                }
            });
        } catch (Exception e) {
            final String errorMessage = "Xatolik: " + e.getMessage();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionError(errorMessage);
                }
            });
        }
    }

    @SimpleFunction(description = "Send data to the server")
    public void SendData(final String data) {
        if (!connectionState) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionError("Socket ulanmagan, ma'lumot yuborish mumkin emas!");
                }
            });
            return;
        }
        final byte[] dataToSend;
        byte[] dataCopy = data.getBytes();
        if (!hexaStringMode) {
            dataToSend = dataCopy;
        } else {
            int i;
            for (i = 0; i < data.length(); i++) {
                if (((dataCopy[i] < 0x30) || (dataCopy[i] > 0x39)) && ((dataCopy[i] < 0x41) || (dataCopy[i] > 0x46)) && ((dataCopy[i] < 0x61) || (dataCopy[i] > 0x66))) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectionError("HexaStringMode faqat hexadecimal belgilarni qo'llaydi!");
                        }
                    });
                    return;
                }
            }
            if ((data.length() % 2) == 1) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionError("HexaStringMode uchun ma'lumot uzunligi juft bo'lishi kerak!");
                    }
                });
                return;
            }
            dataToSend = new byte[data.length() / 2 + 1];
            for (i = 0; i < data.length(); i = i + 2) {
                byte[] temp1 = new byte[2];
                temp1[0] = dataCopy[i];
                temp1[1] = dataCopy[i + 1];
                String temp2 = new String(temp1);
                dataToSend[i / 2] = (byte) Integer.parseInt(temp2, 16);
            }
            dataToSend[i / 2] = (byte) 0x00;
        }

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out = clientSocket.getOutputStream();
                    out.write(dataToSend);
                } catch (SocketException e) {
                    final String errorMessage = "Ma'lumot yuborishda xatolik: " + e.getMessage();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectionError(errorMessage);
                        }
                    });
                } catch (Exception e) {
                    final String errorMessage = "Ma'lumot yuborishda xatolik: " + e.getMessage();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectionError(errorMessage);
                        }
                    });
                }
            }
        });
    }

    @SimpleFunction(description = "Disconnect to the server")
    public void Disconnect() {
        if (connectionState) {
            connectionState = false;
            try {
                clientSocket.close();
            } catch (SocketException e) {
                if (e.getMessage().indexOf("ENOTCONN") == -1) {
                    final String errorMessage = "Ulanishni uzishda xatolik: " + e.getMessage();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectionError(errorMessage);
                        }
                    });
                }
            } catch (IOException e) {
                final String errorMessage = "Ulanishni uzishda xatolik: " + e.getMessage();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionError(errorMessage);
                    }
                });
            } catch (Exception e) {
                final String errorMessage = "Ulanishni uzishda xatolik: " + e.getMessage();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionError(errorMessage);
                    }
                });
            } finally {
                clientSocket = null;
            }
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionError("Socket ulanmagan, ulanishni uzish mumkin emas!");
                }
            });
        }
    }

    @SimpleEvent(description = "Event indicating that a message has been received")
    public void DataReceived(String data) {
        EventDispatcher.dispatchEvent(this, "DataReceived", data);
    }

    @SimpleEvent(description = "Event indicating that the remote socket closed the connection")
    public void RemoteConnectionClosed() {
        EventDispatcher.dispatchEvent(this, "RemoteConnectionClosed");
    }

    @SimpleEvent(description = "Event indicating a connection error")
    public void ConnectionError(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ConnectionError", errorMessage);
    }
}
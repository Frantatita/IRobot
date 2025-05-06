package org.openbot.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
import android.icu.text.SymbolTable;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.ficat.easyble.BleDevice;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import org.openbot.env.GameController;
import org.openbot.env.SensorReading;
import org.openbot.main.CommonRecyclerViewAdapter;
import org.openbot.main.ScanDeviceAdapter;
import org.openbot.utils.Enums;

import timber.log.Timber;

public class Vehicle {

  private final Noise noise = new Noise(1000, 2000, 5000);
  private boolean noiseEnabled = false;

  private int indicator = 0;
  private int speedMultiplier = 192; // 128,192,255
  private Control control = new Control(0, 0);

  private final SensorReading batteryVoltage = new SensorReading();
  private final SensorReading leftWheelRpm = new SensorReading();
  private final SensorReading rightWheelRpm = new SensorReading();
  private final SensorReading sonarReading = new SensorReading();

  private float minMotorVoltage = 2.5f;
  private float lowBatteryVoltage = 9.0f;
  private float maxBatteryVoltage = 12.6f;

  private UsbConnection usbConnection;
  protected boolean usbConnected;
  private final Context context;
  private final int baudRate;

  private String vehicleType = "";
  private boolean hasVoltageDivider = false;
  private boolean hasIndicators = false;
  private boolean hasSonar = false;
  private boolean hasBumpSensor = false;
  private boolean hasWheelOdometryFront = false;
  private boolean hasWheelOdometryBack = false;
  private boolean hasLedsFront = false;
  private boolean hasLedsBack = false;
  private boolean hasLedsStatus = false;
  private boolean isReady = false;

  private Timer serialMessageTimer;

  private BluetoothManager bluetoothManager;
  SharedPreferences sharedPreferences;
  public String connectionType;



  public float getMinMotorVoltage() {
    return minMotorVoltage;
  }

  public void setMinMotorVoltage(float minMotorVoltage) {
    this.minMotorVoltage = minMotorVoltage;
  }

  public float getLowBatteryVoltage() {
    return lowBatteryVoltage;
  }

  public void setLowBatteryVoltage(float lowBatteryVoltage) {
    this.lowBatteryVoltage = lowBatteryVoltage;
  }

  public float getMaxBatteryVoltage() {
    return maxBatteryVoltage;
  }

  public void setMaxBatteryVoltage(float maxBatteryVoltage) {
    this.maxBatteryVoltage = maxBatteryVoltage;
  }

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean ready) {
    isReady = ready;
  }

  public boolean isHasVoltageDivider() {
    return hasVoltageDivider;
  }

  public void setHasVoltageDivider(boolean hasVoltageDivider) {
    this.hasVoltageDivider = hasVoltageDivider;
  }

  public boolean isHasIndicators() {
    return hasIndicators;
  }

  public void setHasIndicators(boolean hasIndicators) {
    this.hasIndicators = hasIndicators;
  }

  public boolean isHasSonar() {
    return hasSonar;
  }

  public void setHasSonar(boolean hasSonar) {
    this.hasSonar = hasSonar;
  }

  public boolean isHasBumpSensor() {
    return hasBumpSensor;
  }

  public void setHasBumpSensor(boolean hasBumpSensor) {
    this.hasBumpSensor = hasBumpSensor;
  }

  public boolean isHasWheelOdometryFront() {
    return hasWheelOdometryFront;
  }

  public void setHasWheelOdometryFront(boolean hasWheelOdometryFront) {
    this.hasWheelOdometryFront = hasWheelOdometryFront;
  }

  public boolean isHasWheelOdometryBack() {
    return hasWheelOdometryBack;
  }

  public void setHasWheelOdometryBack(boolean hasWheelOdometryBack) {
    this.hasWheelOdometryBack = hasWheelOdometryBack;
  }

  public boolean isHasLedsFront() {
    return hasLedsFront;
  }

  public void setHasLedsFront(boolean hasLedsFront) {
    this.hasLedsFront = hasLedsFront;
  }

  public boolean isHasLedsBack() {
    return hasLedsBack;
  }

  public void setHasLedsBack(boolean hasLedsBack) {
    this.hasLedsBack = hasLedsBack;
  }

  public boolean isHasLedsStatus() {
    return hasLedsStatus;
  }

  public void setHasLedsStatus(boolean hasLedsStatus) {
    this.hasLedsStatus = hasLedsStatus;
  }

  public String getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(String vehicleType) {
    this.vehicleType = vehicleType;
  }

  //Envía el comando "f\n" para solicitar la configuración del vehículo.
  public void requestVehicleConfig() {
    //sendStringToDevice(String.format(Locale.US, "f\n"));
  }

  //Procesa una cadena de texto que contiene informacion del vehiculo, en este caso el BODY (cuerpo)
  public void processVehicleConfig(String message) {
    setVehicleType(message.split(":")[0]);

    if (message.contains(":v:")) {
      setHasVoltageDivider(true);
      setVoltageFrequency(250);
    }
    if (message.contains(":i:")) {
      setHasIndicators(true);
    }
    if (message.contains(":s:")) {
      setHasSonar(true);
      setSonarFrequency(100);
    }
    if (message.contains(":b:")) {
      setHasBumpSensor(true);
    }
    if (message.contains(":wf:")) {
      setHasWheelOdometryFront(true);
      setWheelOdometryFrequency(500);
    }
    if (message.contains(":wb:")) {
      setHasWheelOdometryBack(true);
      setWheelOdometryFrequency(500);
    }
    if (message.contains(":lf:")) {
      setHasLedsFront(true);
    }
    if (message.contains(":lb:")) {
      setHasLedsBack(true);
    }
    if (message.contains(":ls:")) {
      setHasLedsStatus(true);
    }
  }

  protected Enums.DriveMode driveMode = Enums.DriveMode.GAME;
  private final GameController gameController;
  private Timer heartbeatTimer;

  public Vehicle(Context context, int baudRate) {
    this.context = context;
    this.baudRate = baudRate;
    gameController = new GameController(driveMode);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    connectionType = getConnectionPreferences("connection_type", "USB");
  }

  public float getBatteryVoltage() {
    return batteryVoltage.getReading();
  }

  public int getBatteryPercentage() {
    return (int)
        ((batteryVoltage.getReading() - lowBatteryVoltage)
            * 100
            / (maxBatteryVoltage - lowBatteryVoltage));
  }

  public void setBatteryVoltage(float batteryVoltage) {
    this.batteryVoltage.setReading(batteryVoltage);
  }

  public float getLeftWheelRpm() {
    return leftWheelRpm.getReading();
  }

  public void setLeftWheelRpm(float leftWheelRpm) {
    this.leftWheelRpm.setReading(leftWheelRpm);
  }

  public float getRightWheelRpm() {
    return rightWheelRpm.getReading();
  }

  public void setRightWheelRpm(float rightWheelRpm) {
    this.rightWheelRpm.setReading(rightWheelRpm);
  }

  public float getRotation() {
    float rotation = (getLeftSpeed() - getRightSpeed()) * 180 / (getLeftSpeed() + getRightSpeed());
    if (Float.isNaN(rotation) || Float.isInfinite(rotation)) rotation = 0f;
    return rotation;
  }

  public int getSpeedPercent() {
    float throttle = (getLeftSpeed() + getRightSpeed()) / 2;
    return Math.abs((int) (throttle * 100 / 255)); // 255 is the max speed
  }

  public String getDriveGear() {
    float throttle = (getLeftSpeed() + getRightSpeed()) / 2;
    if (throttle > 0) return "D";
    if (throttle < 0) return "R";
    return "P";
  }

  public float getSonarReading() {
    return sonarReading.getReading();
  }

  public void setSonarReading(float sonarReading) {
    this.sonarReading.setReading(sonarReading);
  }

  public Control getControl() {
    return control;
  }

  public void setControl(Control control) {
    this.control = control;
    //sendControl();
  }

  public void setControl(float left, float right) {
    this.control = new Control(left, right);
    //sendControl();
  }

  private Timer noiseTimer;

  public void toggleNoise() {
    if (noiseEnabled) stopNoise();
    else startNoise();
  }

  public boolean isNoiseEnabled() {
    return noiseEnabled;
  }

  public void setDriveMode(Enums.DriveMode driveMode) {
    this.driveMode = driveMode;
    gameController.setDriveMode(driveMode);
  }

  public Enums.DriveMode getDriveMode() {
    return driveMode;
  }

  public GameController getGameController() {
    return gameController;
  }

  private class NoiseTask extends TimerTask {
    @Override
    public void run() {
      noise.update();
      //sendControl();
    }
  }

  public void startNoise() {
    noiseTimer = new Timer();
    NoiseTask noiseTask = new NoiseTask();
    noiseTimer.schedule(noiseTask, 0, 50);
    noiseEnabled = true;
    //sendControl();
  }

  public void stopNoise() {
    noiseEnabled = false;
    noiseTimer.cancel();
    //sendControl();
  }

  public int getSpeedMultiplier() {
    return speedMultiplier;
  }

  public void setSpeedMultiplier(int speedMultiplier) {
    this.speedMultiplier = speedMultiplier;
  }

  public int getIndicator() {
    return indicator;
  }


  //
  public void setIndicator(int indicator) {
    this.indicator = indicator;
    switch (indicator) {
      case -1:
        //sendStringToDevice(String.format(Locale.US, "i1,0\n"));
        break;
      case 0:
        //sendStringToDevice(String.format(Locale.US, "i0,0\n"));
        break;
      case 1:
        //sendStringToDevice(String.format(Locale.US, "i0,1\n"));
        break;
    }
  }

  public UsbConnection getUsbConnection() {
    return usbConnection;
  }

  //Inicia una conexcion USB e inicia un temporizador
  public void connectUsb() {

    if (usbConnection == null) usbConnection = new UsbConnection(context, baudRate);
    usbConnected = usbConnection.startUsbConnection();
    if (usbConnected) {
      if (heartbeatTimer == null) {
        startHeartbeat();
      }
    }

    /*
    if (usbConnection == null) usbConnection = new UsbConnection(context, baudRate);
    usbConnected = usbConnection.startUsbConnection();
    if (usbConnected) {
      sendMessageFrank("c100,100"); // Envía un mensaje inicial al conectar USB
    } */
  }

  //Desconecta el usb
  public void disconnectUsb() {
    if (usbConnection != null) {
      stopBot();
      stopHeartbeat();
      usbConnection.stopUsbConnection();
      usbConnection = null;
      usbConnected = false;
    }
  }

  public boolean isUsbConnected() {
    return usbConnected;
  }

  /*
  //Envia el mensaje, ya sea si esta conectado por USB o por Bluetooth
  private void sendStringToDevice(String message) {
    //System.out.println("MENSAJE ENVIADO AL ROBOT: " + message);
    if (getConnectionType().equals("USB") && usbConnection != null) {
      usbConnection.send(message); //METODO QUE ENVIA EL MENSAJE AL ROBOT
      //System.out.println("Este mensaje se envia: " + message);
      //Timber.i("MENSAJE ENVIADO POR CONEXION SERIAL ES: " + message);
    } else if (getConnectionType().equals("Bluetooth")
        && bluetoothManager != null
        && bluetoothManager.isBleConnected()) {
      sendStringToBle(message);
    }
  }*/


  //Manda valores en entero
  private void sendStringToDevice(int message) {
    //System.out.println("MENSAJE ENVIADO AL ROBOT: " + message);
    if (getConnectionType().equals("USB") && usbConnection != null) {
      usbConnection.send(message); //METODO QUE ENVIA EL MENSAJE AL ROBOT
      //System.out.println("Este mensaje se envia: " + message);
      //Timber.i("MENSAJE ENVIADO POR CONEXION SERIAL ES: " + message);
    } else if (getConnectionType().equals("Bluetooth")
            && bluetoothManager != null
            && bluetoothManager.isBleConnected()) {
    }
  }

/*
  private void sendBytesToDevice(byte[] message) {
    if (getConnectionType().equals("USB") && usbConnection != null) {
      usbConnection.send(message); // Método que acepte byte[]
    } else if(getConnectionType().equals("Bluetooth")
            && bluetoothManager != null
            && bluetoothManager.isBleConnected()) {
    }
  }
*/


  public float getLeftSpeed() {
    return control.getLeft() * speedMultiplier;
  }

  public float getRightSpeed() {
    return control.getRight() * speedMultiplier;
  }

  public void sendLightIntensity(float frontPercent, float backPercent) {
    int front = (int) (frontPercent * 255.f);
    int back = (int) (backPercent * 255.f);
    //sendStringToDevice(String.format(Locale.US, "l%d,%d\n", front, back));
    //System.out.println(String.format(Locale.US, "l%d,%d\n", front, back));
  }
/*
  //Funcion qye manda al robot las coordenadas localizadas que son el centroide del objeto rastreado
  public void sendCordinateRobot(int coordX, int coordY){
    String cordenadas = coordX  + "," + coordY;
    sendStringToDevice(cordenadas);
  }
 */


  public void sendCordinateRobot(int coordX){
    int cordenadas = coordX;
    //int [] coordenadas = {coordX, coordY};
    sendStringToDevice(cordenadas);
  }

/*
  //Metodo para mandar por un arreglo
  public void sendCoordinatesToRobot(int coordX, int coordY) {
    ByteBuffer buffer = ByteBuffer.allocate(8); // 4 bytes por coordenada
    buffer.putInt(coordX);
    buffer.putInt(coordY);
    byte[] byteArray = buffer.array();
    sendBytesToDevice(byteArray);
  }
*/


  public void sendConteoPrueba() {
    for (int i = 1; i <= 180; i++) { // Ciclo de 1 a 180
      String conteo = String.valueOf(i); // Convertir el número a String
      //sendStringToDevice(conteo); // Enviar el conteo al dispositivo
      try {
        Thread.sleep(1000); // Esperar 1 segundo (1000 milisegundos)
      } catch (InterruptedException e) {
        e.printStackTrace(); // Manejo de excepción si se interrumpe el ciclo
      }
    }
  }

/*
  // Método para recibir la posición central del objeto rastreado y enviar las coordenadas al robot
  public void receiveCenterOfTrackedObject(Point centerPoint) {
    if (centerPoint != null) {
      int coordX = centerPoint.x;
      int coordY = centerPoint.y;
      sendCordinateRobot(coordX, coordY);
    }
  }
  */

  public void receiveCenterOfTrackedObject(Point centerPoint) {
    if (centerPoint != null) {
      int coordX = centerPoint.x;
      //int coordY = centerPoint.y;
      sendCordinateRobot(coordX);
      //sendCoordinatesToRobot(coordX, coordY);
    }
  }




  //Obtiene las velocidades de las ruedas, las ajusta y envia un comando de control
  public void sendControl() {
    /*
    int left = (int) (getLeftSpeed());
    int right = (int) (getRightSpeed());
    if (noiseEnabled && noise.getDirection() < 0)
      left =
          (int)
              ((control.getLeft() - noise.getValue())
                  * speedMultiplier); // dado que el valor del ruido no tiene el componente speedMultiplier,
    // raw control value is used
    if (noiseEnabled && noise.getDirection() > 0)
      right = (int) ((control.getRight() - noise.getValue()) * speedMultiplier);

    sendStringToDevice(String.format(Locale.US, "c%d,%d\n", left, right));
  */

    int left = (int) (getLeftSpeed());
    int right = (int) (getRightSpeed());
    if (noiseEnabled && noise.getDirection() < 0) {
      left = (int) ((control.getLeft() - noise.getValue()) * speedMultiplier);
    }
    if (noiseEnabled && noise.getDirection() > 0) {
      right = (int) ((control.getRight() - noise.getValue()) * speedMultiplier);
    }
    //sendStringToDevice(String.format(Locale.US, "c%d,%d\n", left, right));
  }

  //MODIFICAR ESTA LINEA PARA QUE MANDE ALGUNA OPCION.
  protected void sendMessageFrank(String message) {
      //sendStringToDevice("f");
  }

  protected void sendHeartbeat(int timeout_ms) {
    //sendStringToDevice(String.format(Locale.getDefault(), "h%d\n", timeout_ms));
    //sendStringToDevice(String.format("p"));
  }
  protected void setSonarFrequency(int interval_ms) {
    //sendStringToDevice(String.format(Locale.getDefault(), "s%d\n", interval_ms));
  }

  protected void setVoltageFrequency(int interval_ms) {
    //sendStringToDevice(String.format(Locale.getDefault(), "v%d\n", interval_ms));
  }

  protected void setWheelOdometryFrequency(int interval_ms) {
    //sendStringToDevice(String.format(Locale.getDefault(), "w%d\n", interval_ms));
  }

  private class HeartBeatTask extends TimerTask {

    @Override
    public void run() {
       //sendHeartbeat(750);
       // sendMessageFrank("f");
    }
  }

  //Inicia-crea un temporizador
  public void startHeartbeat() {
    heartbeatTimer = new Timer();
    HeartBeatTask heartBeatTask = new HeartBeatTask();
    heartbeatTimer.schedule(heartBeatTask, 250, 250); // 250ms delay and 250ms interval
  }

  //Para el temporizador
  public void stopHeartbeat() {
    if(heartbeatTimer != null) {
      heartbeatTimer.cancel();
      heartbeatTimer.purge(); // remove all pending tasks from the queue
      heartbeatTimer = null;
    }
  }

  //Para el robot
  public void stopBot() {
    Control control = new Control(0, 0);
    setControl(control);
  }

  public ScanDeviceAdapter getBleAdapter() {
    return bluetoothManager.adapter;
  }

  public void setBleAdapter(
      ScanDeviceAdapter adapter,
      @NonNull CommonRecyclerViewAdapter.OnItemClickListener onItemClickListener) {
    bluetoothManager.adapter = adapter;
    bluetoothManager.adapter.setOnItemClickListener(onItemClickListener);
  }

  public void startScan() {
    bluetoothManager.startScan();
  }

  public void stopScan() {
    bluetoothManager.stopScan();
  }

  public List<BleDevice> getDeviceList() {
    return bluetoothManager.deviceList;
  }

  public void setBleDevice(BleDevice device) {
    bluetoothManager.bleDevice = device;
  }

  public BleDevice getBleDevice() {
    return bluetoothManager.bleDevice;
  }

  public void toggleConnection(int position, BleDevice device) {
    bluetoothManager.toggleConnection(position, device);
  }

  public void initBle() {
    bluetoothManager = new BluetoothManager(context);
  }

  private void sendStringToBle(String message) {
    bluetoothManager.write(message);
  }

  public boolean bleConnected() {
    return bluetoothManager.isBleConnected();
  }

  private void setConnectionPreferences(String name, String value) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(name, value);
    editor.apply();
  }

  private String getConnectionPreferences(String name, String defaultValue) {
    try {
      if (sharedPreferences != null) {
        return sharedPreferences.getString(name, defaultValue);
      } else return defaultValue;
    } catch (ClassCastException e) {
      return defaultValue;
    }
  }

  public String getConnectionType() {
    return getConnectionPreferences("connection_type", "USB");
  }
}

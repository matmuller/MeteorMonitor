package com.example.mat.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.Bundle;

import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.channels.FileChannel;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

public class mainactivity extends Activity implements SurfaceHolder.Callback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }
    public static double calculateLST(double longi) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.ZONE_OFFSET, TimeZone.getTimeZone("GMT").getRawOffset());

        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0 + cal.get(Calendar.SECOND)/3600.0;

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        double DD = day + (hour/24.);

        if (month<3) {
            year = year - 1;
            month = month + 12;
        }

        double jd = Math.floor(365.25*year) + Math.floor(30.6001*month) + Math.floor(DD) + 1720996.5 - Math.floor(year/100)+Math.floor(Math.floor(year/100)/4);
        double delta = jd - 2451545.0;
        double T = delta/36525.0;
        double T0 = 280.46061837 + 360.98564736629*(jd-2451545.0) + 0.000387933*T*T - T*T*T/38710000.0;
        //6.6460656 + (2400.051336*T) + (0.000025862*T*T);
        double GST = (T0/24 - Math.floor(T0/24))*24;
        double LST = GST + (DD-Math.floor(DD))*24*1.002737908 + (Math.abs(longi/15.));

        if (LST<0) {
            LST=LST+24;
        }
        if (LST>24) {
            LST=LST-24;
        }

        return LST;
    }
    public void GenerateReport(Calendar c){
        try {
            FileWriter fileWriter = new FileWriter(directory + String.format("%s.%s.%s_%s:%s:%s_report.txt",(c.get(Calendar.YEAR)), ((c.get(Calendar.MONTH))+1), (c.get(Calendar.DAY_OF_MONTH)), (c.get(Calendar.HOUR)), (c.get(Calendar.MINUTE)), (c.get(Calendar.SECOND))), true);
            out = new BufferedWriter(fileWriter);
            out.append(String.format("---Statistics:---\r\n"));
            out.append(String.format("AVG: %f, Diff: %f errAVG: %f \r\n STDEV: %f, Diff: %f \r\n\n", AVG, DiffAVG, errAVG, STDev, DiffSTDev));
            out.append(String.format("---Central Pixel Coordinates:---\r\n"));
            out.append(String.format("Horizontal Coordinates:\r\n"));
            out.append(String.format("Azimuth: %f Altitude: %f\r\n\n",azimuth,altur));
            out.append(String.format("Equatorial Coordinates:\r\n"));
            out.append(String.format("Right Ascention: %f Declination: %f\r\n\n",Ra,Dec));
            out.append(String.format("---Pixel(x,y) to Coordinates equations:---\r\n"));
            out.append(String.format("Horizontal Coordinates:\r\n"));
            double kx = thetaH*Math.cos(CamRoll);
            double ky = thetaV*Math.sin(CamRoll);
            out.append(String.format("Azimuth = %f + %f*(x/%d- 1/2) - %f*(y/%d - 1/2)\r\n\n", azimuth, kx, camxres, ky, camyres));
            ky = -thetaV*Math.cos(CamRoll);
            kx = -thetaH*Math.sin(CamRoll);
            out.append(String.format("Altitude* = %f + %f*(x/%d- 1/2) + %f*(y/%d - 1/2)\r\n", altur, kx, camxres, ky, camyres));
            out.append(String.format("*For Altitude > 90º, consider [real]Altitude = 180º - [calculated]Altitude, as it have crossed the 90º pole.\r\n"));
//out.append(String.format("Equatorial Coordinates:\r\n"));
            //out.append(String.format("Right Ascention: %f Declination: %f\r\n\n",Ra,Dec));
            //out.flush();
            out.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void GenerateReportLens(Calendar c){
        try {
            FileWriter fileWriter = new FileWriter(directory + String.format("%s.%s.%s_%s:%s:%s_report.txt",(c.get(Calendar.YEAR)), ((c.get(Calendar.MONTH))+1), (c.get(Calendar.DAY_OF_MONTH)), (c.get(Calendar.HOUR)), (c.get(Calendar.MINUTE)), (c.get(Calendar.SECOND))), true);
            out = new BufferedWriter(fileWriter);
            out.append(String.format("---Statistics:---\r\n"));
            out.append(String.format("AVG: %f, Diff: %f errAVG: %f \r\n STDEV: %f, Diff: %f \r\n\n", AVG, DiffAVG, errAVG, STDev, DiffSTDev));
            out.append(String.format("---Central Pixel Coordinates:---\r\n"));
            out.append(String.format("Horizontal Coordinates:\r\n"));
            out.append(String.format("Azimuth: %f Altitude: %f\r\n\n",azimuth,altur));
            out.append(String.format("Equatorial Coordinates:\r\n"));
            out.append(String.format("Right Ascention: %f Declination: %f\r\n\n",Ra,Dec));
            out.append(String.format("---Pixel(x,y) to Coordinates equations:---\r\n"));
            out.append(String.format("Horizontal Coordinates:\r\n"));
            double Xc = (circlecenterx-rectview.left)*camxres/(rectview.right-rectview.left);
            double Yc = (circlecentery-rectview.top)*camyres/(rectview.bottom-rectview.top);
            double kx = -thetaH*Math.cos(CamRoll)*(rectview.right-rectview.left)/(2*circlesize*camxres);
            double ky = -thetaV*Math.sin(CamRoll)*(rectview.bottom-rectview.top)/(2*circlesize*camyres);
            out.append(String.format("Azimuth = %f + %f*(x - %f) - %f*(y - %f)\r\n\n", azimuth, kx, Xc, ky, Yc));
            ky = thetaV*Math.cos(CamRoll)*(rectview.bottom-rectview.top)/(circlesize*camyres);
            kx = thetaH*Math.sin(CamRoll)*(rectview.right-rectview.left)/(circlesize*camxres);
            out.append(String.format("Altitude* = %f + %f*(y - %f) + %f*(x - %f)\r\n", altur, kx, Yc, ky, Xc));
            out.append(String.format("*For Altitude > 90º, consider [real]Altitude = 180º - [calculated]Altitude.\r\n"));
//out.append(String.format("Equatorial Coordinates:\r\n"));
            //out.append(String.format("Right Ascention: %f Declination: %f\r\n\n",Ra,Dec));
            //out.flush();
            out.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MySensor mRenderer;
    public SensorManager mSensorManager;
    public TextView GPSt, DAVG, DSTD;
    public double azimuth;
    public double altur;
    public double Dec;
    public double Ra;
    public double CamRoll;
    public boolean capturing = false;
    public Rect rectview = new Rect();
    public float geodec;

    protected Location mLastLocation;
    protected LocationManager loc;
    protected boolean requestinglocup;
    protected GoogleApiClient mGoogleApiClient;
    protected static final String TAG = "MainActivity";
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    class MySensor implements SensorEventListener {
        private double HA;
        private double LSidT;
        private int RaH;
        private int RaM;
        private double RaS;
        private double lati;
        private double longi;
        public TextView X = (TextView)findViewById(R.id.textView);
        public TextView Y = (TextView) findViewById(R.id.textView3);
        public TextView L = (TextView)findViewById(R.id.textView2);
        public TextView K = (TextView) findViewById(R.id.textView5);
        private android.hardware.Sensor mRotationVectorSensor;
        private final float[] mRotationMatrix = new float[16];
        private final float[] nRotationMatrix = new float[16];
        public float[] xyz=new float[4];
        public MySensor() {
            // find the rotation-vector sensor
                mRotationVectorSensor = mSensorManager.getDefaultSensor(
                        android.hardware.Sensor.TYPE_ROTATION_VECTOR);
            if (mRotationVectorSensor == null){
                mRotationVectorSensor = mSensorManager.getDefaultSensor(
                        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
            }
        }
        public void start() {
            // enable our sensor when the activity is resumed, ask for
            // 200 ms updates.
            mSensorManager.registerListener(this, mRotationVectorSensor, mSensorManager.SENSOR_DELAY_NORMAL);
        }
        public void stop() {
            // make sure to turn our sensor off when the activity is paused
            mSensorManager.unregisterListener(this);
        }
        public void updatecoords() {
            azimuth = 180 + Math.toDegrees(xyz[0]) + geodec;
            if (azimuth < 0){
                azimuth = 360 + azimuth;
            }
            altur = Math.toDegrees(xyz[1]);
            CamRoll = xyz[2] + Math.PI;

            if (mLastLocation != null) {

                lati = mLastLocation.getLatitude();
                longi = mLastLocation.getLongitude();
                Dec = 360/(2*Math.PI)*Math.asin((Math.sin(Math.toRadians(altur)) * Math.sin(Math.toRadians(lati))) + (Math.cos(Math.toRadians(altur)) * Math.cos(Math.toRadians(lati)) * Math.cos(Math.toRadians(azimuth))));
                HA = 360/(2*Math.PI)*Math.acos((Math.sin(Math.toRadians(altur)) - (Math.sin(Math.toRadians(lati))*Math.sin(Math.toRadians(Dec))))/(Math.cos(Math.toRadians(lati))*Math.cos(Math.toRadians(Dec))));

                if (Math.sin(Math.toRadians(azimuth)) > 0) {
                    HA = (360-HA)/15.;
                }
                else {
                    HA = HA/15.;
                }
                LSidT = calculateLST(longi);
                Ra = LSidT - HA;
                if (Ra < 0) {
                    Ra = Ra + 24;
                }

                RaH = (int)Math.floor(Ra);
                RaM = (int)Math.floor((Ra - Math.floor(Ra))*60);
                RaS = ((Ra - Math.floor(Ra))*60 - RaM)*60;

                L.setText(String.format("Right Ascension = %dh%dm%fs", RaH,RaM,RaS));
                K.setText(String.format("Declination = %f", Dec));
            }
            X.setText(String.format("Azimuth = %f", azimuth));
            Y.setText(String.format("Altitude = %f", altur));
        }
        public void onSensorChanged(SensorEvent event) {
            // we received a sensor event. it is a good practice to check
            // that we received the proper event
            if (!capturing) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, nRotationMatrix);
                    SensorManager.getOrientation(nRotationMatrix, xyz);
                    updatecoords();
                }
            }
        }
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }
    }

    protected void UpdateGPS(){
        GPSt.setText(String.format("Latitude: %f\nLongitude: %f", mLastLocation.getLatitude(), mLastLocation.getLongitude()));
    }
    final LocationListener Loklistener= new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }
        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }
        @Override
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            UpdateGPS();
            geodec = new GeomagneticField((float)mLastLocation.getLatitude(), (float)mLastLocation.getLongitude(), (float)mLastLocation.getAltitude(), mLastLocation.getTime()).getDeclination();
        }
    };

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                UpdateGPS();
            }
            else {
                Toast.makeText(this, "No location detected", Toast.LENGTH_LONG).show();
            }
            loc.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, Loklistener);
            requestinglocup = true;
        }
        catch (SecurityException e){}
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    ScaleGestureDetector SGD;
    public float circlescale;
    public float circlesize = 75;
    public float circlecenterx;
    public float circlecentery;
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            circlescale = detector.getScaleFactor();
            circlesize = circlesize*circlescale*circlescale;
            return true;
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            if (surfaceHolderdraw.getSurface().isValid()) {
                SGD.onTouchEvent(event);
                circlecenterx = event.getRawX()-10;
                circlecentery = event.getRawY()-140;
                Canvas canvas = surfaceHolderdraw.lockCanvas();
                canvas.drawColor(0,android.graphics.PorterDuff.Mode.CLEAR);
                canvas.drawCircle(circlecenterx,circlecentery,circlesize,paint);
                surfaceHolderdraw.unlockCanvasAndPost(canvas);
            }
        }
        return false;
    }

    Camera camera;
    SurfaceView surfaceView;
    SurfaceView surfaceView2;
    SurfaceHolder surfaceHolder;
    SurfaceHolder surfaceHolderdraw;
    Camera.PictureCallback jpegCallback;
    public Camera.Parameters param;
    public ArrayList<String> options = new ArrayList<String>();
    public ArrayList<String> optionsvalues = new ArrayList<String>();

    public int[] histograma = new int[256];
    public double[] diffhistograma = new double[256];
    public double thetaV;
    public double thetaH;

    public double AVGsum = 0;
    public double AVG;
    public double AVGofAVG;
    public double STDofAVG;
    public double[] LastAVGes = new double[25];
    public double AVGofAVGsum;
    public double DiffAVG = 0;
    public double STDevsum = 0;
    public double STDevsum2 = 0;
    public double STDev;
    public double LastSTDev = 0;
    public double DiffSTDev = 0;
    public double errAVG;
    public double deltat;

    public boolean eventmode = false;
    public int eventflag = 0;
    public int piccount = 0;
    public double targetdiff;
    public double targetdiffstd;
    public int shootingfrequency;
    public int camxres, camyres;
    public BitmapFactory.Options bmOptions = new BitmapFactory.Options();

    public String directory = Environment.getExternalStorageDirectory() + "/LUMENS/";
    public File docsFolder = new File(directory);

    public String fileName = "1.bmp";
    public File output = new File(directory,fileName);
    public String fileName2 = "2.bmp";
    public File output2 = new File(directory,fileName2);
    public String fileName3 = "3.bmp";
    public File output3 = new File(directory, fileName3);
    public String fileName4 = "4.bmp";
    public File output4 = new File(directory, fileName4);
    public String fileName5 = "5.bmp";
    public File output5 = new File(directory, fileName5);
    public BufferedWriter out;

    public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public Runnable tpic = new Thread(new Runnable() {
        @Override
        public void run() {
            if (param.isAutoWhiteBalanceLockSupported()){
                param.setAutoWhiteBalanceLock(true);}
            camera.setParameters(param);
            camera.takePicture(null, null, jpegCallback);
            if (param.isAutoWhiteBalanceLockSupported()){
                param.setAutoWhiteBalanceLock(false);}
            camera.setParameters(param);;  }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        if (!docsFolder.exists()){
            docsFolder.mkdir();
        }
        GPSt = (TextView)findViewById(R.id.textView4);
        DAVG = (TextView)findViewById(R.id.textView6);
        DSTD = (TextView) findViewById(R.id.textView7);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mRenderer = new MySensor();
        loc = (LocationManager)getSystemService(LOCATION_SERVICE);
        buildGoogleApiClient();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        SGD = new ScaleGestureDetector(this,new ScaleListener());

        final Spinner mspin=(Spinner) findViewById(R.id.spinner);
        final Integer[] items = new Integer[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,20};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, items);
        mspin.setAdapter(adapter);
        final Spinner mspin2=(Spinner) findViewById(R.id.spinner2);
        final Integer[] items2 = new Integer[]{32000,16000,8000,4000,2000,1500,1000,900,800,750,700,650,600,550,500,450,400,350,300,250,200,175,150,125,100,75,50};
        ArrayAdapter<Integer> adapter2 = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, items2);
        mspin2.setAdapter(adapter2);
        final Spinner mspin3=(Spinner) findViewById(R.id.spinner3);
        final Integer[] items3 = new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,20};
        ArrayAdapter<Integer> adapter3 = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, items3);
        mspin3.setAdapter(adapter3);

        final CheckBox LensCheck = (CheckBox) findViewById(R.id.checkBox2);
        final CheckBox STDCheck = (CheckBox) findViewById(R.id.checkBox);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView2 = (SurfaceView) findViewById(R.id.surfaceView2);
        surfaceView2.setZOrderOnTop(true);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolderdraw = surfaceView2.getHolder();
        surfaceHolderdraw.setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    capturing = true;
                    targetdiff = items[mspin.getSelectedItemPosition()];
                    shootingfrequency = items2[mspin2.getSelectedItemPosition()];
                    targetdiffstd = items3[mspin3.getSelectedItemPosition()];
                    if (!LensCheck.isChecked()){
                        thetaV = param.getVerticalViewAngle();
                        thetaH = param.getHorizontalViewAngle();
                    }
                    Calendar c = Calendar.getInstance();
                    File subFolder = new File(directory);
                    if (!subFolder.exists()){
                        subFolder.mkdirs();
                    }
                    //Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    //cal.set(Calendar.ZONE_OFFSET, TimeZone.getTimeZone("GMT").getRawOffset());
                    scheduler = Executors.newScheduledThreadPool(1);
                    scheduler.scheduleAtFixedRate(tpic, 0, shootingfrequency, TimeUnit.MILLISECONDS);
                    //showrect();
                } else {
                    scheduler.shutdownNow();
                    capturing = false;
                    piccount = 0;
                    for (int i = 0; i < 25; i++) {
                        LastAVGes[i] = 0;
                    }
                    Toast.makeText(getApplicationContext(), String.format("App takes %f ms to process each picture.",deltat), Toast.LENGTH_LONG).show();
                }
            }
        });

        jpegCallback = new PictureCallback() {

            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {

                camxres = camera.getParameters().getPictureSize().width;
                camyres = camera.getParameters().getPictureSize().height;
                AVGsum = 0;
                STDevsum = 0;
                eventflag = 0;
                if (piccount < 25) {
                    piccount += 1;
                }
                eventmode = true;
                new Thread(new Runnable() {
                    public void run() {
                        FileOutputStream outStream = null;

                        if (output5.exists()) {
                            output5.delete();                }
                        File from4      = new File(directory, fileName4);
                        from4.renameTo(output5);
                        if (output4.exists()) {
                            output4.delete();                }
                        File from3      = new File(directory, fileName3);
                        from3.renameTo(output4);
                        if (output3.exists()) {
                            output3.delete();                }
                        File from2      = new File(directory, fileName2);
                        from2.renameTo(output3);
                        if (output2.exists()) {
                            output2.delete();                }
                        File from      = new File(directory, fileName);
                        from.renameTo(output2);
                        if (output.exists()) {
                            output.delete();                 }

                        long tzero = System.currentTimeMillis();
                        try {outStream = new FileOutputStream(directory + String.format("%s", fileName));
                            outStream.write(data);
                            outStream.close();
                            }
                        catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        catch (IOException e) {
                        e.printStackTrace();
                        }
                        finally {
                        }


                        Bitmap bitmap = BitmapFactory.decodeFile(output.getAbsolutePath(), bmOptions);
                        bitmap = Bitmap.createScaledBitmap(bitmap, camxres, camyres, true);
                        int N = camxres*camyres;

                        for (int i = 0; i < 256; i++) {
                            histograma[i]=0;
                        }
                        for (int i = 0; i < camxres; i++) {
                            for (int j = 0; j < camyres; j++) {
                                int p = bitmap.getPixel(i, j);
                                int s = (Color.red(p) + Color.green(p) + Color.blue(p))/3;
                                histograma[s]+=1;
                            }
                        }
                        long tum = System.currentTimeMillis();
                        deltat = (double)(tum - tzero);

                        for (int i = 0; i < 256; i++) {
                            diffhistograma[i]=histograma[i];
                            AVGsum += i*histograma[i];
                        }
                        AVG = AVGsum/N;

                        for (int i = 1; i < 256; i++) {
                            STDevsum += i*(histograma[i]-AVG)*(histograma[i]-AVG);
                        }
                        STDev = STDevsum/N;

                        DiffAVG = AVG-AVGofAVG;
                        DiffSTDev = (STDev - LastSTDev) / LastSTDev;

                        if (piccount == 25){
                            if (DiffAVG > targetdiff*errAVG){
                                eventflag += 1;
                            }
                            if (STDCheck.isChecked()){
                                if (Math.abs(DiffSTDev) > targetdiffstd*STDofAVG){
                                    eventflag += 1;
                                }
                            }
                            if (eventflag > 0){
                                eventmode = true;
                            }
                        }
                        if (eventmode==true) {
                            eventmode = false;
                            eventflag = 0;
                            Calendar c = Calendar.getInstance();
                            if (LensCheck.isChecked()){
                                GenerateReportLens(c);
                            }
                            else {
                                GenerateReport(c);
                            }
                            try{
                                File eventfile = new File(directory + String.format("%s.%s.%s_%s:%s:%s_event.bmp",(c.get(Calendar.YEAR)), ((c.get(Calendar.MONTH))+1), (c.get(Calendar.DAY_OF_MONTH)), (c.get(Calendar.HOUR)), (c.get(Calendar.MINUTE)), (c.get(Calendar.SECOND))));
                                copy(output, eventfile);
                            }
                            catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            finally {
                            }
                        }
                        else {
                            LastSTDev = STDev;
                            if (piccount < 25){
                                LastAVGes[piccount-1] = AVG;
                            }
                            else {
                                for (int i = 0; i < 24; i++) {
                                    LastAVGes[i]=LastAVGes[i+1];
                                }
                                LastAVGes[24] = AVG;
                            }
                            AVGofAVGsum = 0;
                            for (int i = 0; i < piccount; i++) {
                                AVGofAVGsum += LastAVGes[i];
                            }
                            AVGofAVG = AVGofAVGsum/piccount;

                            STDevsum2 = 0;
                            for (int i = 0; i < piccount; i++) {
                                STDevsum2 += (LastAVGes[i]-AVGofAVG)*(LastAVGes[i]-AVGofAVG);
                            }
                            STDofAVG = Math.sqrt(STDevsum2/piccount);
                            errAVG = STDofAVG/Math.sqrt(piccount);
                        }
                        bitmap.recycle();
                    }}).start();

                DAVG.setText(String.format("DiffAVG: %f", DiffAVG));
                DSTD.setText(String.format("errAVG: %f", errAVG));
                // Toast.makeText(getApplicationContext(), String.format("Picture Saved \r\n DiffAVG: %f \r\n DiffSTD: %f", DiffAVG, DiffSTDev), Toast.LENGTH_LONG).show();
                refreshCamera();
            }
        };
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
        }
        catch (Exception e) {
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }
        catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mRenderer.start();
        mGoogleApiClient.connect();
        if (!requestinglocup){
        try {
            loc.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, Loklistener);
            requestinglocup = true;
        }
        catch (SecurityException e) {}
        }
    }
    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mRenderer.stop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        try {
            loc.removeUpdates(Loklistener);
            requestinglocup = false;
        }
        catch (SecurityException e) {}
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
     //    Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        int maxec = param.getMaxExposureCompensation();
        int minec = param.getMinExposureCompensation();
        float ecstep = param.getExposureCompensationStep();

        final int[] isapsh = {0,0,0};
        final SubMenu sub = menu.addSubMenu(0,0,0,R.string.ISO);
        final SubMenu sub2 = menu.addSubMenu(0,1,1,R.string.Aperture);
        final SubMenu sub3 = menu.addSubMenu(0,2,2,"Set Lens X Angular Opening");
        final SubMenu sub4 = menu.addSubMenu(0,3,3,"Set Lens Y Angular Opening");
        final SubMenu sub5 = menu.addSubMenu(0,4,4,R.string.Exposure);
        final SubMenu sub6 = menu.addSubMenu(0,5,5,"Set Destination Folder");

     /***   sub4.add(4, 40, Menu.NONE, "200ms");
        sub4.add(4, 41, Menu.NONE, "350ms");
        sub4.add(4, 42, Menu.NONE, "500ms");
        sub4.add(4, 43, Menu.NONE, "750ms");
        sub4.add(4, 44, Menu.NONE, "1s");
        sub4.add(4, 45, Menu.NONE, "2s");
        sub4.add(4, 46, Menu.NONE, "5s");***/

        int k=0;
        for (int i = minec; i <= maxec; i++) {
            float expo = i * ecstep;
            sub5.add(0, 50+k, Menu.NONE, String.format("%f", expo));
            k = k + 1;
        }

        final String[] supportedopt = camera.getParameters().flatten().split(";");

        for (int i = 0; i < supportedopt.length; i++) {
            String supopt = supportedopt[i].split("=")[0];
            String supoptv = supportedopt[i].split("=")[1];
            if (supopt.contains("video")){}
            else{
                if (supopt.contains("preview")){}
                else {
                    if (supopt.contains("values")) {
                    if (supopt.contains("iso")) {
                        isapsh[0] = 1;
                    }
                    //if (supopt.contains("shutter")){isapsh[2]=1;}
                    //if (supoptv.split(",").length==1){}
                    options.add(supopt.replace("-values", ""));
                    optionsvalues.add(supoptv);
                    }
                }
            }
        }             //if (supopt.contains("aperture")){isapsh[1]=1;}

        for (int i = 0; i < options.size(); i++) {
            SubMenu subi = menu.addSubMenu(0, i + 6, i + 6, options.get(i).replace("-", " "));
            String[] optval = optionsvalues.get(i).split(",");
            for (int j = 0; j < optval.length; j++) {
                subi.add(0, 100*(i+1)+j,Menu.NONE, String.format(optval[j]));
        }}

        final ProgressDialog progress;
        progress=new ProgressDialog(this);
        progress.setMessage("Updating configuration options... Please wait");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setProgress(0);
        progress.show();

        Thread t = new Thread(new Runnable() {
            public void run() {

                if (isapsh[0]==0){
            try {
                param.set("iso", 100);
                camera.setParameters(param);
                if (param.get("iso").equals("100")){
                    sub.add(1,10,Menu.NONE,"100");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 200);
                camera.setParameters(param);
                if (param.get("iso").equals("200")){
                    sub.add(1,11,Menu.NONE,"200");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 400);
                camera.setParameters(param);
                if (param.get("iso").equals("400")){
                    sub.add(1,12,Menu.NONE,"400");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 800);
                camera.setParameters(param);
                if (param.get("iso").equals("800")){
                    sub.add(1,13,Menu.NONE,"800");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 1600);
                camera.setParameters(param);
                if (param.get("iso").equals("1600")){
                    sub.add(1,14,Menu.NONE,"1600");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 3200);
                camera.setParameters(param);
                if (param.get("iso").equals("3200")){
                    sub.add(1,15,Menu.NONE,"3200");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 6400);
                camera.setParameters(param);
                if (param.get("iso").equals("6400")){
                    sub.add(1,16,Menu.NONE,"6400");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", 12800);
                camera.setParameters(param);
                if (param.get("iso").equals("12800")){
                    sub.add(1,17,Menu.NONE,"12800");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("iso", "auto");
                camera.setParameters(param);
                if (param.get("iso").equals("auto")){
                    sub.add(1,18,Menu.NONE,"auto");
                }
            }
            catch (Exception e) {
            }}

                if (isapsh[1]==0){

            try {
                param.set("aperture", 28);
                camera.setParameters(param);
                if (param.get("aperture").equals("28")){
                    sub2.add(2,20,Menu.NONE,"28");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 32);
                camera.setParameters(param);
                if (param.get("aperture").equals("32")){
                    sub2.add(2,21,Menu.NONE,"32");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 35);
                camera.setParameters(param);
                if (param.get("aperture").equals("35")){
                    sub2.add(2,22,Menu.NONE,"35");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 40);
                camera.setParameters(param);
                if (param.get("aperture").equals("40")){
                    sub2.add(2,23,Menu.NONE,"40");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 45);
                camera.setParameters(param);
                if (param.get("aperture").equals("45")){
                    sub2.add(2,24,Menu.NONE,"45");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 50);
                camera.setParameters(param);
                if (param.get("aperture").equals("50")){
                    sub2.add(2,25,Menu.NONE,"50");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 56);
                camera.setParameters(param);
                if (param.get("aperture").equals("56")){
                    sub2.add(2,26,Menu.NONE,"56");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 63);
                camera.setParameters(param);
                if (param.get("aperture").equals("63")){
                    sub2.add(2,27,Menu.NONE,"63");
                }
            }
            catch (Exception e) {
            }
            try {
                param.set("aperture", 71);
                camera.setParameters(param);
                if (param.get("aperture").equals("71")){
                    sub2.add(2,28,Menu.NONE,"71");
                }
            }
            catch (Exception e) {
            }
            //for (int i = 1; i < 500; i++){
            //        try {
            //            param.set("shutter-speed", i);
            //            camera.setParameters(param);
            //            if (param.get("shutter-speed").equals(String.format("%d",i))){
            //                sub3.add(0, 10000+i, Menu.NONE, String.format("%d",i));
            //            }
            //        }
            //        catch (Exception e) {
            //        }}
                }
                progress.dismiss();
            }});

        t.start();
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int indice = item.getItemId();
        int resto = indice % 100;
        indice = ((indice/100)-1);

        //if (item.getItemId()>9999) {
        //    param.set("shutter-speed", (item.getItemId()-10000));
        //    camera.setParameters(param);
        //    return true;
       // }
            if (item.getItemId()>499){
            try {

                param.set(options.get(indice), optionsvalues.get(indice).split(",")[resto]);
                camera.setParameters(param);
                return true;
            }
            catch (Exception e) {
            }}

        if (item.getItemId()>49){
            try {
                param.setAutoExposureLock(false);
                int o = item.getItemId() - 50 - param.getMaxExposureCompensation();
                param.setExposureCompensation(o);
                param.setAutoExposureLock(true);
                camera.setParameters(param);
                return true;
            }
            catch (Exception e) {
            }}

        else{
        switch (item.getItemId()) {
            case 0:
                return true;
            case 2:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Set Camera 'Horizontal' Angular Opening - numbers only");
// Set up the input
                final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                builder.setView(input);
// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        thetaH = Double.parseDouble(input.getText().toString());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            case 3:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle("Set Camera 'Vertical' Angular Opening - numbers only");
// Set up the input
                final EditText input2 = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                builder2.setView(input2);
// Set up the buttons
                builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        thetaV = Double.parseDouble(input2.getText().toString());
                    }
                });
                builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder2.show();
                return true;
            case 5:
                /////////////////////////////////////////////////////////////////////////////////////////////////
                //Create FileOpenDialog and register a callback
                /////////////////////////////////////////////////////////////////////////////////////////////////
                SimpleFileDialog FolderChooseDialog =  new SimpleFileDialog(mainactivity.this, "FolderChoose",
                        new SimpleFileDialog.SimpleFileDialogListener()
                        {
                            @Override
                            public void onChosenDir(String chosenDir)
                            {
                                // The code in this function will be executed when the dialog OK button is pushed
                                directory = chosenDir;
                                Toast.makeText(mainactivity.this, "Chosen Directory: " +
                                        directory, Toast.LENGTH_LONG).show();
                            }
                        });
                FolderChooseDialog.chooseFile_or_Dir();
                return true;
            case 10:
                param.set("iso", 100);
                camera.setParameters(param);
                return true;
            case 11:
                param.set("iso", 200);
                camera.setParameters(param);
                return true;
            case 12:
                param.set("iso", 400);
                camera.setParameters(param);
                return true;
            case 13:
                param.set("iso", 800);
                camera.setParameters(param);
                return true;
            case 14:
                param.set("iso", 1600);
                camera.setParameters(param);
                return true;
            case 15:
                param.set("iso", 3200);
                camera.setParameters(param);
                return true;
            case 16:
                param.set("iso", 6400);
                camera.setParameters(param);
                return true;
            case 17:
                param.set("iso", 12800);
                camera.setParameters(param);
                return true;
            case 18:
                param.set("iso", "auto");
                camera.setParameters(param);
                return true;
            case 20: //can be 28 32 35 40 45 50 56 63 71 80 on default zoom
                param.set("aperture", 28);
                camera.setParameters(param);
                return true;
            case 21:
                param.set("aperture", 32);
                camera.setParameters(param);
                return true;
            case 22:
                param.set("aperture", 35);
                camera.setParameters(param);
                return true;
            case 23:
                param.set("aperture", 40);
                camera.setParameters(param);
                return true;
            case 24:
                param.set("aperture", 45);
                camera.setParameters(param);
                return true;
            case 25:
                param.set("aperture", 50);
                camera.setParameters(param);
                return true;
            case 26:
                param.set("aperture", 56);
                camera.setParameters(param);
                return true;
            case 27:
                param.set("aperture", 63);
                camera.setParameters(param);
                return true;
            case 28:
                param.set("aperture", 71);
                camera.setParameters(param);
                return true;
            case 29:
                param.set("aperture", 80);
                camera.setParameters(param);
                return true;
            case 40: shootingfrequency = 200;
                return true;
            case 41: shootingfrequency = 350;
                return true;
            case 42: shootingfrequency = 500;
                return true;
            case 43: shootingfrequency = 750;
                return true;
            case 44: shootingfrequency = 1000;
                return true;
            case 45: shootingfrequency = 2000;
                return true;
            case 46: shootingfrequency = 5000;
                return true;

            default:
                return super.onOptionsItemSelected(item);
    }}
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == surfaceHolder){
           try {
               camera = Camera.open();
           }
           catch (RuntimeException e) {
                System.err.println(e);
                return;
           }

            camera.setDisplayOrientation(90);
            param = camera.getParameters();
            camxres = param.getPictureSize().width;
            camyres = param.getPictureSize().height;
            param.setPreviewSize(param.getSupportedPreviewSizes().get(0).width, param.getSupportedPreviewSizes().get(0).height);
            param.set("orientation", "portrait");
            param.set("mode", "m");
            camera.setParameters(param);

            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            }
            catch (Exception e) {
                System.err.println(e);
                return;
            }
            surfaceView.getLocalVisibleRect(rectview);
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder == surfaceHolder){
            refreshCamera();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (holder == surfaceHolder){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}
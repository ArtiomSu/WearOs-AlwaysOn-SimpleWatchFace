package terminal_heat_sink.simplewatchface;

import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private Paint mBackgroundPaint;
        private boolean mAmbient;

        private float mCenterX;
        private float mCenterY;

        private float font_size_time = 100;
        private float font_size_date = 40;
        private float font_size_month = 30;
        private float font_size_year = 20;
        private float font_size_battery = 30;
        private float font_size_uptime = 20;
        private float font_size_network = 25;

        private String uptime_text;

        private boolean isWifiConn = false;
        private boolean isBlueTooth = false;
        private String network_text;

        private Typeface typeface;

        private int mainColor = Color.rgb(255,255,255);

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .setHideStatusBar(true)
                    .build());

            mCalendar = Calendar.getInstance();

            typeface = getResources().getFont(R.font.dsdigi);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            uptime_text = read_from_sys("uptime | cut -d \",\" -f1  | cut -d \" \" -f3-10\n",getApplicationContext()).trim();

            ConnectivityManager connMgr =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            for (Network network : connMgr.getAllNetworks()) {
                NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn |= networkInfo.isConnected();
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
                    isBlueTooth |= networkInfo.isConnected();
                }
            }

            network_text = "Wifi: " + (isWifiConn ? "on" : "off") + " Blue: " + (isBlueTooth ? "on" : "off");
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            updateTimer();
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX = width / 2f;
            mCenterY = height / 2f;
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            canvas.drawColor(Color.BLACK);
            int minutes = mCalendar.get(Calendar.MINUTE);
            int hours = mCalendar.get(Calendar.HOUR_OF_DAY);
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);
            int month = mCalendar.get(Calendar.MONTH)+1;

            String hours_s = ((hours<10)?"0"+hours :""+hours);
            String minutes_s = ((minutes<10)?"0"+minutes :""+minutes);
            String day_s = ((day<10)?"0"+day :""+day);
            String month_s = ((month<10)?"0"+month :""+month);


            int seconds = mCalendar.get(Calendar.SECOND);
            String seconds_s = ((seconds<10)?"0"+seconds :""+seconds);
            String day_word = mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            String month_word = mCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            int year = mCalendar.get(Calendar.YEAR);

            String output_time = ""+hours_s+":"+minutes_s+":"+seconds_s;
            //String output_bottom =""+day_s+":"+month_s+":"+day_word;
            String output_day = day_word+" ("+day_s+")";
            String output_month = month_word+" ("+month_s+")";

            if(seconds_s.equals("00")){ // update this every minute
                uptime_text = read_from_sys("uptime | cut -d \",\" -f1  | cut -d \" \" -f3-10\n",getApplicationContext()).trim();
                ConnectivityManager connMgr =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                for (Network network : connMgr.getAllNetworks()) {
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        isWifiConn |= networkInfo.isConnected();
                    }
                    if (networkInfo.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
                        isBlueTooth |= networkInfo.isConnected();
                    }
                }

                network_text = "Wifi: " + (isWifiConn ? "on" : "off") + " Blue: " + (isBlueTooth ? "on" : "off");
            }
            mBackgroundPaint.setAntiAlias(true);



            Paint font_info = new Paint();
            font_info.setAntiAlias(true);
            font_info.setTextSize(20);
            font_info.setTypeface(typeface);
            font_info.setColor(mainColor);


            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level / (float)scale;
            int percent = (int)(batteryPct*100);

            Paint battery_paint = new Paint();
            battery_paint.setAntiAlias(true);
            battery_paint.setTextSize(font_size_battery);
            battery_paint.setTypeface(typeface);
            battery_paint.setColor(mainColor);
            battery_paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Battery is at "+percent+"%",mCenterX,mCenterY-font_size_time/2,battery_paint);

            Paint uptime_p = new Paint();
            uptime_p.setAntiAlias(true);
            uptime_p.setTextSize(font_size_uptime);
            uptime_p.setTypeface(typeface);
            uptime_p.setColor(mainColor);
            uptime_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(uptime_text,mCenterX,mCenterY-font_size_time/2-font_size_battery-font_size_network,uptime_p);

            Paint network_p = new Paint();
            network_p.setAntiAlias(true);
            network_p.setTextSize(font_size_network);
            network_p.setTypeface(typeface);
            network_p.setColor(mainColor);
            network_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(network_text,mCenterX,mCenterY-font_size_time/2-font_size_battery,network_p);


            Paint time_p = new Paint();
            time_p.setAntiAlias(true);
            time_p.setColor(mainColor);
            time_p.setTextSize(font_size_time);
            time_p.setTypeface(typeface);
            time_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(output_time,mCenterX ,mCenterY+30,time_p);

            Paint day_p = new Paint();
            day_p.setAntiAlias(true);
            day_p.setColor(mainColor);
            day_p.setTextSize(font_size_date);
            day_p.setTypeface(typeface);
            day_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(output_day,mCenterX,mCenterY+font_size_time-30,day_p);

            Paint month_p = new Paint();
            month_p.setAntiAlias(true);
            month_p.setColor(mainColor);
            month_p.setTextSize(font_size_month);
            month_p.setTypeface(typeface);
            month_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(output_month,mCenterX,mCenterY+font_size_time-40+font_size_date,month_p);

            Paint year_p = new Paint();
            year_p.setAntiAlias(true);
            year_p.setColor(mainColor);
            year_p.setTextSize(font_size_year);
            year_p.setTypeface(typeface);
            year_p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(""+year,mCenterX,mCenterY+font_size_time-45+font_size_date+font_size_month,year_p);

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            //return isVisible() && !mAmbient;
            return isVisible();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private String read_from_sys(String command, Context context){
            Process p;
            String result = "";
            try {
                p = Runtime.getRuntime().exec("sh");

                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                DataInputStream in = new DataInputStream(p.getInputStream());
                os.writeBytes(command);
                // Close the terminal
                os.writeBytes("exit\n");
                os.flush();
                try {
                    p.waitFor();
                    if (p.exitValue() != 255) {

                        if(p.exitValue() == 0){
                            Log.i("SystemWriter","read successfully");
                            int i;
                            String output = "";
                            char c;
                            while((i = in.read())!=-1) {
                                c = (char)i;
                                output +=c;
                            }
                            result = output.toString();
                        }else{
                            Log.i("SystemWriter","failed to read");
                            Toast toast = Toast.makeText(context, "Could not read files", Toast.LENGTH_LONG);
                            toast.show();
                        }

                    }
                    else {
                        Log.i("SystemWriter","not rooted 1");
                        Toast toast = Toast.makeText(context, "root required please root your phone", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                } catch (InterruptedException e) {
                    Log.i("SystemWriter","not rooted 2");
                    Toast toast = Toast.makeText(context, "root required please root your phone", Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (IOException e) {
                Log.i("SystemWriter","not rooted 3");
                Toast toast = Toast.makeText(context, "root required please root your phone", Toast.LENGTH_SHORT);
                toast.show();
            }
            return result;
        }
    }
}
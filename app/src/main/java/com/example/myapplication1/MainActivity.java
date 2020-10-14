package com.example.myapplication1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean flag=false;//定义标志位，识别是否连接无人机
    private Socket socket;//定义客户端
    private OutputStream out;//定义输出流
    private byte[] data=new byte[34];//定义一个34字节的数组存放数据
    private int x34=0;//临时存放油门值
    private int x56;//航向值
    private int x78;//临时保存横滚值
    private int x910;//临时保存俯仰值
    private int value=50;
    TextView tv1,tv2,tv3,tv4;
    private SharedPreferences sp,version;
    private MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inituav();
        Seekbarrock();
        Qian();
        Hou();
        Zuo();
        You();
        tv1=this.findViewById(R.id.textview1);
        tv2=this.findViewById(R.id.textview2);
        tv3=this.findViewById(R.id.textview3);
        tv4=this.findViewById(R.id.textview4);
    }
    //    初始化无人机数据
    public void inituav()
    {
        sp=getSharedPreferences("uav",MODE_PRIVATE);
        version=getSharedPreferences("uav2",MODE_PRIVATE);
        int vv=version.getInt("version",0);
        if (vv==0)
        {
            x56=1500;
            x78=1500;
            x910=1500;
            initdata();
        }else
        {
            List<Integer> list = read();
            x56 = list.get(0);
            x78 = list.get(1);
            x910 = list.get(2);
            initdata();
        }
    }
    public void initdata()
    {
        data[0]=(byte)0xAA;                                       //协议固定数据
        data[1]=(byte)0xC0;                                      //协议固定数据
        data[2]=(byte)0x1C;                                      //协议固定数据
        /**设置油门--控制上下方向*/
        data[3]=(byte)(x34>>8);
        data[4]=(byte)(x34&0xff);
        data[5]=(byte)(x56>>8);
        data[6]=(byte)(x56&0xff);
        data[7]=(byte)(x78>>8);
        data[8]=(byte)(x78&0xff);
        data[9]=(byte)(x910>>8);
        data[10]=(byte)(x910&0xff);
        data[31]=(byte)0x1C;                                     //协议固定数据
        data[32]=(byte)0x0D;                                    //协议固定数据
        data[33]=(byte)0x0A;
    }
    //启动按钮连接无人机
    public void start(View v)
    {
        //判断是否连接
        if(flag==false)
        {
            flag=true;
            //无人机未连接，创建线程
            Thread thread1=new Thread(new ConnectThread());
            thread1.start();
        }else
        {
            //连接成功
            Toast.makeText(MainActivity.this, "启动成功", Toast.LENGTH_SHORT).show();
        }
    }
    //创建连接无人机线程，网络编程
    public class ConnectThread implements Runnable
    {
        @Override
        public void run() {
            //完成网络编程,创建socket
            try {
                socket=new Socket();
                socket.connect(new InetSocketAddress("192.168.4.1",333),1000);
                out=socket.getOutputStream();
                out.write("GEC\r\n".getBytes());
                out.flush();
            }catch (UnknownHostException e)
            {
                e.printStackTrace();
                handler.sendEmptyMessage(3);
            } catch (IOException e)
            {
                e.printStackTrace();
                stopMusic();
                initMusic(R.raw.lian);
                mp.start();
                handler.sendEmptyMessage(2);
            }catch (Exception e)
            {
                e.printStackTrace();
                handler.sendEmptyMessage(4);
            }
            if (socket!=null)
            {
                try {
                    socket.sendUrgentData(88);//
                    flag=true;
                    Thread thread=new Thread(new SenddataThread());
                    thread.start();
                }catch (Exception e)
                {
                    e.printStackTrace();
                    handler.sendEmptyMessage(1);
                }
            }
        }
    }
    public class SenddataThread implements Runnable
    {
        @Override
        public void run() {
          try {
              out=socket.getOutputStream();
              while (flag){
                  out.write(data);
                  out.flush();
                  Thread.sleep(5);
              }
          }catch (IOException e)
          {
              e.printStackTrace();
          }
          catch (Exception e)
          {
              e.printStackTrace();
          }
        }
    }
    //处理消息机制
    Handler handler=new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 1:
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(MainActivity.this, "请连接无人机", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(MainActivity.this, "IP地址错误", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    Toast.makeText(MainActivity.this, "未知异常", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    break;
                case 6:
                    break;
            }
        }
    };
    public void stop(View v)
    {
        flag=false;
        x34=0;
        data[3]=0;
        data[4]=0;
        tv1.setText(x34+"");
        SeekBar seekBar = findViewById(R.id.seekbar);
        seekBar.setProgress(0);
    }
    //加速
    public void Up(View v)
    {
        if (x34<=990)
        {
            x34+=10;
            data[3]=(byte)(x34>>8);
            data[4]=(byte)(x34&0xff);
            tv1.setText(x34+"");
        }else
        {
            Toast.makeText(MainActivity.this, "已达到最大转速，无法再加速", Toast.LENGTH_SHORT).show();
        }
    }
    //减速
    public void Down(View v)
    {
        if (x34>=10)
        {
            x34-=10;
            data[3]=(byte)(x34>>8);
            data[4]=(byte)(x34&0xff);
            tv1.setText(x34+"");
        }else
        {
            Toast.makeText(MainActivity.this, "已达到最低转速，无法再减速", Toast.LENGTH_SHORT).show();
        }
    }
    //滚动条控制油门
    public void Seekbarrock()
    {
        SeekBar seekBar = findViewById(R.id.seekbar);
        seekBar.setMax(990);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                x34=progress;
                data[3]=(byte)(progress>>8);
                data[4]=(byte)(progress&0xff);
                tv1.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    //前
    public void Front(View v)
    {
        if (x910<=2880)
        {
            x910+=value;
            data[9]=(byte)(x910>>8);
            data[10]=(byte)(x910&0xff);
            tv2.setText("俯仰值:"+x910);
        }else
        {
            Toast.makeText(MainActivity.this, "俯仰值已达到最大", Toast.LENGTH_SHORT).show();
        }
    }
    //后
    public void After(View v)
    {
        if (x910>=0)
        {
            x910-=value;
            data[9]=(byte)(x910>>8);
            data[10]=(byte)(x910&0xff);
            tv2.setText("俯仰值:"+x910);
        }else
        {
            Toast.makeText(MainActivity.this, "俯仰值已达到最小", Toast.LENGTH_SHORT).show();
        }
    }
    //左
    public void Left(View v)
    {
        if (x78<=2880)
        {
            x78+=value;
            data[7]=(byte)(x78>>8);
            data[8]=(byte)(x78&0xff);
            tv3.setText("横滚值:"+x78);
        }else
        {
            Toast.makeText(MainActivity.this, "横滚值已达到最大", Toast.LENGTH_SHORT).show();
        }
    }
    //右
    public void Right(View v)
    {
        if (x78>=0)
        {
            x78-=value;
            data[7]=(byte)(x78>>8);
            data[8]=(byte)(x78&0xff);
            tv3.setText("横滚值:"+x78);
        }else
        {
            Toast.makeText(MainActivity.this, "横滚值已达到最小", Toast.LENGTH_SHORT).show();
        }
    }
    public void Shun(View v)
    {
        if (x56>=0)
        {
            x56-=value;
            data[5]=(byte)(x56>>8);
            data[6]=(byte)(x56&0xff);
            tv4.setText("航向值:"+x78);
        }else
        {
            Toast.makeText(MainActivity.this, "航向值已达到最小", Toast.LENGTH_SHORT).show();
        }
    }
    public void Ni(View v)
    {
        if (x56<=2880)
        {
            x56+=value;
            data[5]=(byte)(x56>>8);
            data[6]=(byte)(x56&0xff);
            tv4.setText("航向值:"+x78);
        }else
        {
            Toast.makeText(MainActivity.this, "航向值已达到最大", Toast.LENGTH_SHORT).show();
        }
    }
    public void Cu(View v)
    {
        value=50;
    }
    public void Xi(View v)
    {
        value=1;
    }
    public void save(int x56,int x78,int x910) {
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt("x56", x56);
        spe.putInt("x78", x78);
        spe.putInt("x910", x910);
        spe.commit();
    }
    public void btsave(View v)
    {
        save(x56,x78,x910);
        int vso=version.getInt("version",0);
        vso++;
        saveversion(vso);
    }
    public void saveversion(int v)
    {
        SharedPreferences.Editor spe=version.edit();
        spe.putInt("version",v);
        spe.commit();
        Toast.makeText(MainActivity.this, "当前版本:"+v, Toast.LENGTH_SHORT).show();
    }
    //读取
    public List<Integer> read()
    {
        List<Integer> uav=new ArrayList<>();
        int x56 = sp.getInt("x56",0);
        uav.add(x56);
        int x78 = sp.getInt("x78",0);
        uav.add(x78);
        int x910 = sp.getInt("x910",0);
        uav.add(x910);
        return uav;
    }
    public void Qian()
    {
        Button btn=findViewById(R.id.qian);
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int n=motionEvent.getAction();
                if (n==MotionEvent.ACTION_DOWN)
                {
                    x910=x910+300;
                    data[9]=(byte)(x910>>8);
                    data[10]=(byte)(x910&0xff);
                    tv2.setText("俯仰值:"+x910);
                    stopMusic();
                    initMusic(R.raw.qian);
                    mp.start();
                }else if (n==MotionEvent.ACTION_UP)
                {
                    List<Integer> list=read();
                    int xx=list.get(2);
                    if (xx!=0)
                    {
                        x910=xx;
                    }else
                    {
                        x910=1500;
                    }
                    data[9]=(byte)(x910>>8);
                    data[10]=(byte)(x910&0xff);
//                    tv2.setText("");
                }
                return false;
            }
        });
    }
    public void Hou()
    {
        Button btn1=findViewById(R.id.hou);
        btn1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int n=motionEvent.getAction();
                if (n==MotionEvent.ACTION_DOWN)
                {
                    x910=x910-300;
                    data[9]=(byte)(x910>>8);
                    data[10]=(byte)(x910&0xff);
                    tv2.setText("俯仰值:"+x910);
                    stopMusic();
                    initMusic(R.raw.hou);
                    mp.start();
                }else if (n==MotionEvent.ACTION_UP)
                {
                    List<Integer> list=read();
                    int xx=list.get(2);
                    if (xx!=0)
                    {
                        x910=xx;
                    }else
                    {
                        x910=1500;
                    }
                    data[9]=(byte)(x910>>8);
                    data[10]=(byte)(x910&0xff);
//                    tv2.setText("");
                }
                return false;
            }
        });
    }
    public void Zuo()
    {
        Button btn2=findViewById(R.id.zuo);
        btn2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int n=motionEvent.getAction();
                if (n==MotionEvent.ACTION_DOWN)
                {
                    x78=x78+300;
                    data[7]=(byte)(x78>>8);
                    data[8]=(byte)(x78&0xff);
                    tv4.setText("横滚值:"+x78);
                    stopMusic();
                    initMusic(R.raw.zuo);
                    mp.start();
                }else if (n==MotionEvent.ACTION_UP)
                {
                    List<Integer> list=read();
                    int xx=list.get(1);
                    if (xx!=0)
                    {
                        x78=xx;
                    }else
                    {
                        x78=1500;
                    }
                    data[7]=(byte)(x78>>8);
                    data[8]=(byte)(x78&0xff);
//                    tv4.setText("");
                }
                return false;
            }
        });
    }
    public void You()
    {
        Button btn3=findViewById(R.id.you);
        btn3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int n=motionEvent.getAction();
                if (n==MotionEvent.ACTION_DOWN)
                {
                    x78=x78-300;
                    data[7]=(byte)(x78>>8);
                    data[8]=(byte)(x78&0xff);
                    tv4.setText("横滚值:"+x78);
                    stopMusic();
                    initMusic(R.raw.you);
                    mp.start();
                }else if (n==MotionEvent.ACTION_UP)
                {
                    List<Integer> list=read();
                    int xx=list.get(1);
                    if (xx!=0)
                    {
                        x78=xx;
                    }else
                    {
                        x78=1500;
                    }
                    data[7]=(byte)(x78>>8);
                    data[8]=(byte)(x78&0xff);
//                    tv4.setText("");
                }
                return false;
            }
        });
    }
    //打开音乐方法
    public void initMusic(int res)
    {
        mp=MediaPlayer.create(MainActivity.this,res);
        try {
            mp.prepare();
        }catch (Exception e)
        {
            e.printStackTrace();
            handler.sendEmptyMessage(5);
        }
    }
    //关闭音乐
    public void stopMusic()
    {
        if (mp!=null)
        {
            mp.reset();
        }
    }
    public void display(View v)
    {
        LinearLayout layout=findViewById(R.id.uav);
        LinearLayout layout2=findViewById(R.id.uav2);
        if (layout.getVisibility()==View.VISIBLE)
        {
            layout.setVisibility(View.INVISIBLE);
            layout2.setVisibility(View.VISIBLE);
        }else
        {
            layout.setVisibility(View.VISIBLE);
            layout2.setVisibility(View.INVISIBLE);
        }

    }
    protected void onDestroy()
    {
        super.onDestroy();
        try {
            out.close();
            socket.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
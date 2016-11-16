package com.example.simpleintegral;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener{

	SensorManager sensorManager;
	EditText editText1;
	EditText editText2;
	TextView origin_x,origin_y,origin_z,trans_z,trans_x,trans_y;
	public static final float G=(float) 10;//g=9.8，但是由于传感器的原因设置为10
	long pre_time=System.currentTimeMillis();//定义时间轴
	float[] pre_acc={0,0,0};//存储上一个周期内的加速度值
	float[] pre_ori={0,0,0};//存储上一个周期内的方向值
	int section=2000,count=0;
	boolean flag=true,
			lineracc=false,
			gravity_ready=false,
			pre_acc_ready=false,
			start=false;
	float[] acc_z=new float[section];//大约能存储10s的数据量
    float x=0,y=0,z=0;
	float velocity=0;
	float pre_velocity=velocity;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//editText=(EditText) findViewById(R.id.resultText);
		editText1=(EditText) findViewById(R.id.editText1);
		editText2=(EditText) findViewById(R.id.editText2);
		origin_x=(TextView) findViewById(R.id.origin_x);
		origin_y=(TextView) findViewById(R.id.origin_y);
		origin_z=(TextView) findViewById(R.id.origin_z);
		trans_z=(TextView) findViewById(R.id.trans_z);
		trans_x=(TextView) findViewById(R.id.trans_x);
		trans_y=(TextView) findViewById(R.id.trans_y);
		Initialize();
		sensorManager = (SensorManager) getSystemService(
				Context.SENSOR_SERVICE);
		//判断手机有没有线性加速度传感器。
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		for (Sensor item : sensors) {
			if(item.getType()==10){
				lineracc=true;//true表示有
				break;
			}
		}


		//测试Displacement函数
		/*
		float a[]={1,1,1,-6,-8,-1,-1,1,2,3,1,1};
		int len=a.length;
		float disp=Displacement(a, len);
		StringBuilder dispBuilder=new StringBuilder();
		dispBuilder.append(disp);
		editText1.setText(dispBuilder.toString());
		*/
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if(!lineracc){
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_FASTEST);
		}
    	else
    		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),SensorManager.SENSOR_DELAY_FASTEST);

	}

	@Override
	protected void onStop()
	{
		// 取消注册
		sensorManager.unregisterListener(this);
		super.onStop();
	}

	public void btnstart(View source){
		count=0;
		start=true;
		pre_time=System.currentTimeMillis();
	}

	// 以下是实现SensorEventListener接口必须实现的方法
	// 当传感器的值发生改变时回调该方法
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		float[] values=event.values;
		int sensorType=event.sensor.getType();
		StringBuilder sb =new StringBuilder();
		StringBuilder result=new StringBuilder();
		switch (sensorType) {
			case Sensor.TYPE_LINEAR_ACCELERATION:
			case Sensor.TYPE_ACCELEROMETER:
				origin_x.setText(String.valueOf(Math.round(values[0] * 100) / 100.0));
				origin_y.setText(String.valueOf(Math.round(values[1] * 100) / 100.0));
				origin_z.setText(String.valueOf(Math.round(values[2] * 100) / 100.0));
				if(start) {
					if (!lineracc) {
						if (gravity_ready && pre_acc_ready) {
							acc_z[count] = (float) (pre_acc[2] - z + 0.10);
							count++;
						}
					} else {
						if (pre_acc_ready) {
							acc_z[count] = pre_acc[2];
							count++;
						}
					}

					if (System.currentTimeMillis() - pre_time >= 3000) {
						//float [] a = new float[count];
						filter(acc_z,count);
						float disp = Displacement(acc_z, count);
						StringBuilder resultBuilder = new StringBuilder();
						resultBuilder.append("Displacement=" + Math.round(disp * 100) / 100.0);
						resultBuilder.append(",count=" + count);
						int max = 0;
						for (int i = 0; i < count; i++) {
							if (Math.abs(acc_z[i]) > Math.abs(acc_z[max]))
								max = i;
						}
						resultBuilder.append("\nmax=" + max + ",acc_z[max]=" + acc_z[max]);
						editText1.setText(resultBuilder.toString());
						start=false;
					}
				}

				//保存变量
				pre_acc[0] = values[0];
				pre_acc[1] = values[1];
				pre_acc[2] = values[2];
				pre_acc_ready=true;
				break;

			case Sensor.TYPE_ORIENTATION:
				if(!lineracc){
					//保存变量
					pre_ori[0]=values[0];
					pre_ori[1]=values[1];
					pre_ori[2]=values[2];
					//重力加速度在三个轴上的投影
					x = (float) Math.abs(G * Math.sin(pre_ori[2] * Math.PI / 180));
					y = (float) Math.abs(G * Math.sin(pre_ori[1] * Math.PI / 180) * Math.cos(pre_ori[2] * Math.PI / 180));
					z = (float) Math.abs(G * Math.cos((Math.abs(pre_ori[1]) >= Math.abs(pre_ori[2]) ? pre_ori[1] : pre_ori[2]) * Math.PI / 180) *
							Math.cos((Math.abs(pre_ori[1]) >= Math.abs(pre_ori[2]) ? pre_ori[2] : pre_ori[1]) * Math.PI / 180));

					trans_z.setText(String.valueOf(Math.round(z * 100) / 100.0));
					trans_x.setText(String.valueOf(Math.round(x * 100) / 100.0));
					trans_y.setText(String.valueOf(Math.round(y * 100) / 100.0));
					gravity_ready=true;
				}
				break;
			default:
			    break;
		}

	}

	// 当传感器精度改变时回调该方法。
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}
	
	int findNextZero(float a[],int len,int origin)//找到为0的位点
	{
		if(origin+2>len)
		    return -1;
		for(int i=origin;i<=len-2;i++)
			if(a[i]*a[i+1]<0 || a[i]==0)
			    return i;
		return -1;//若没有找到 
	}
	
	float findMax(float a[],int start,int end)//start<=i<=end    //找到最大的点
	{
		float max=a[start];
		for(int i=start;i<=end;i++)
			if(a[i]>max)
				max=a[i];
		return max;
	}
	float findMin(float a[],int start,int end)//start<=i<=end    //找到最小的点
	{
		float min=a[start];
		for(int i=start;i<=end;i++)
			if(a[i]<min)
				min=a[i];
		return min;
	}

	private void filter(float a[],int length){
		int parm=128;//滤波参数 0~256
		for(int i=1;i<length;i++){
			a[i]=(a[i]*parm+a[i-1]*(256-parm))/256;
		}
	}

	float Displacement(float a[],int length)
	{
		int i;
		float[] v=new float[length];
		v[0]=0;
		for(i=1;i<length;i++)
		    v[i]=v[i-1]+(a[i]+a[i-1])/(2*(length-1));
		//for(i=0;i<length;i++)
		//   printf("v[%d]=%.3f\n",i,v[i]);
		float[] d=new float[length];
		d[0]=0;
		for(i=1;i<length;i++)
		    d[i]=d[i-1]+(v[i]+v[i-1])/(2*(length-1));//二重积分计算。
		//printf("\n");
		//for(i=0;i<length;i++)
		//    printf("d[%d]=%.3f\n",i,d[i]);
		//printf("\n");
		//正负位移测试
	    int pre_status=0;
		// 0：前面的已经做出了上下运动的判断，可以从这里开始。
		// 1：前一个阶段是向上，还没有做出判断。
		// 2：前一个阶段是向下，还没有做出判断。
		//只有当碰到相反的位移或者是到最后一段的时候，才会做出上下运动的判断，判断完归零。
		int flag=0;
		int positive_time=0;//正向运动的次数
		int negative_time=0;
		float positive_disp=0;//正向运动的位移
		float negative_disp=0;
		int place;//零点的位置
		int k;
		double yuzhi=0,yuzhiplus=0;//阈值判断法的加速度的阈值，原始值为2
		float limit=(float) Math.sqrt(5);//正负距离之比大于该值则可以判断上下运动，小于表示静止,原值为根号五
		int pre_info=-1;//记录上一次上下运动的判断结果
		StringBuilder resultBuilder=new StringBuilder();
		for(i=flag;i<length&&flag<length;i++)
		{
			if(v[flag]*v[flag+1]<=0)//排除特殊点情况
		    {
		    	flag++;
		    	continue;
		    }
		    if(v[flag]>0 && v[flag+1]>0)//速度大于0的阶段
		    {
		    	place=findNextZero(v,length,flag);//从flag开始寻找零点。
		    	if(Math.abs(findMax(a, flag, place==-1?length-1:place))<=yuzhiplus &&
						Math.abs(findMin(a, flag, place==-1?length-1:place))<=yuzhiplus)
				//从flag到零点的阶段或者从flag到取样结束的阶段，阈值法判别，最大值小于阈值的情况
		    	{
		    		//resultBuilder.append("v>0的时候太小了！flag="+flag+",place="+place+"\n");
		    		//resultBuilder.append("Max="+findMax(v, flag, place)+",Min="+findMin(v, flag, place)+"\n");
		    		if(place==-1)//没有找到零点，最后一段
		    		{
		    			if(pre_status==0)
		    			{
		    				if(pre_info!=0)
		    					editText2.setText("未移动。");
		    			    pre_info=0;
		    				break;
		    			}
		    			else if(pre_status==1)
		    			{
		    				if(positive_disp>0 && pre_info!=1)
		    				{
		    					resultBuilder.append("向上移动了。\n");
		    					pre_info=1;
		    				}
					    	else if(positive_disp<0 && pre_info!=2)
					    	{
					    		resultBuilder.append("向下移动了。\n");
					    		pre_info=2;
					    	}
					    	else if(pre_info!=0)
					    	{
					    		resultBuilder.append("未移动。\n");
					    		pre_info=0;
					    	}
		    			}
		    			else
		    			{
		    				if(negative_disp>0 && pre_info!=1)
		    				{
		    					resultBuilder.append("向上移动了。\n");
		    					pre_info=1;
		    				}
					    	else if(negative_disp<0 && pre_info!=2)
					    	{
					    		resultBuilder.append("向下移动了。\n");
					    		pre_info=2;
					    	}
					    	else if(pre_info!=0)
					    	{
					    		resultBuilder.append("未移动。\n");
					    		//pre_info=0;
					    	}
		    			}
		    			break;
		    		}
		    		flag=place+1;
		    		continue;
		    	}
				//速度大于阈值的情况
				if(place!=-1)//找到了零点，积分算flag到place的位移。
				{
					//printf("i=%d,flag=%d,place=%d\n",i,flag,place);
					if(pre_status!=1)
						positive_disp=0;
					for(k=flag;k<=place;k++)
					    positive_disp+=(v[k]+v[k-1])/(2*(length-1));//积
					//printf("第%d个Positive=%.3f\n",++positive_time,positive_disp);
					resultBuilder.append("第"+(++positive_time)+"个Positive="+positive_disp+"\n");
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					flag=place+1;
					//continue;
				}
				else//没有找到零点，积分算flag到末尾的位移。判定状态后退出
				{
					if(pre_status!=1)
						positive_disp=0;
					for(k=flag;k<length;k++)
					    positive_disp+=(v[k]+v[k-1])/(2*(length-1));
					//printf("第%d个Positive=%.3f\n",++positive_time,positive_disp);
					resultBuilder.append("第"+(++positive_time)+"个Positive="+positive_disp+"\n");

					//resultBuilder.append("flag="+flag+",place="+place+"\n");

					if(pre_status==2)
				    {
				    	if(Math.abs(positive_disp/negative_disp)>limit && pre_info!=1)
				    	{
				    		resultBuilder.append("向上移动了。\n");
				    		pre_info=1;
				    	}
				    	else if(Math.abs(positive_disp/negative_disp)<1.0/limit && pre_info!=2)
				    	{
				    		resultBuilder.append("向下移动了。\n");
				    		pre_info=2;
				    	}
				    	else if(pre_info!=0)
				    	{
				    		resultBuilder.append("未移动。\n");
				    		pre_info=0;
				    	}
				    }
				    else
				    {
				    	if(positive_disp>0 && pre_info!=1)
	    				{
	    					resultBuilder.append("向上移动了。\n");
	    					pre_info=1;
	    				}
				    	else if(positive_disp<0 && pre_info!=2)
				    	{
				    		resultBuilder.append("向下移动了。\n");
				    		pre_info=2;
				    	}
				    	else if(pre_info!=0)
				    	{
				    		resultBuilder.append("未移动。\n");
				    		pre_info=0;
				    	}
				    }
					break;
				}

				if(pre_status==1 || pre_status==0)
				{
					pre_status=1;
					continue;
				}
				if(pre_status==2)
				{
					if(Math.abs(positive_disp/negative_disp)>limit && pre_info!=1)
			    	{
			    		resultBuilder.append("向上移动了。\n");
			    		pre_info=1;
			    	}
			    	else if(Math.abs(positive_disp/negative_disp)<1.0/limit && pre_info!=2)
			    	{
			    		resultBuilder.append("向下移动了。\n");
			    		pre_info=2;
			    	}
			    	else if(pre_info!=0)
			    	{
			    		resultBuilder.append("未移动。\n");
			    		pre_info=0;
			    	}
					pre_status=0;
				}
		    }

		    if(v[flag]<0 && v[flag+1]<0)//速度都小于零的阶段
		    {
		    	place=findNextZero(v,length,flag);
				//速度小于阈值的情况
		    	if(Math.abs(findMax(a, flag, place==-1?length-1:place))<=yuzhi &&
						Math.abs(findMin(a, flag, place==-1?length-1:place))<=yuzhi)
		    	{
		    		//resultBuilder.append("v<0的时候太小了！flag="+flag+",place="+place+"\n");
		    		if(place==-1)//没有找到0点，最后一段了，执行完就退出
		    		{
		    			if(pre_status==0)
		    			{
		    				if(pre_info!=0)
		    				    editText2.setText("未移动。");
		    				pre_info=0;
		    				break;
		    			}
		    			else if(pre_status==1)//上一个阶段判断的是向上走了
		    			{
		    				if(positive_disp>0 && pre_info!=1)
		    				{
		    					resultBuilder.append("向上移动了。\n");
		    					pre_info=1;
		    				}
					    	else if(positive_disp<0 && pre_info!=2)
					    	{
					    		resultBuilder.append("向下移动了。\n");
					    		pre_info=2;
					    	}
					    	else if(pre_info!=0)
					    	{
					    		resultBuilder.append("未移动。\n");
					    		pre_info=0;
					    	}
		    			}
		    			else//上一个阶段判断的是向下走了
		    			{
		    				if(negative_disp>0 && pre_info!=1)
		    				{
		    					resultBuilder.append("向上移动了。\n");
		    					pre_info=1;
		    				}
					    	else if(negative_disp<0)
					    	{
					    		resultBuilder.append("向下移动了。\n");
					    		pre_info=2;
					    	}
					    	else if(pre_info!=0)
					    	{
					    		resultBuilder.append("未移动。\n");
					    		pre_info=0;
					    	}
		    			}
		    			break;
		    		}

		    		flag=place+1;//找到了零点，这段时间加速度小于阈值，那么直接转到下一阶段
		    		continue;
		    	}

				//速度大于阈值的情况
				if(place!=-1)//找到了零点，同时不是最后一段位移，积分算flag到place的位移。
				{
					//printf("i=%d,flag=%d,place=%d\n",i,flag,place);
					if(pre_status!=2)
						negative_disp=0;
					for(k=flag;k<=place;k++)
					    negative_disp+=(v[k]+v[k-1])/(2*(length-1));//对这段时间的速度进行积分计算位移
					//printf("第%d个Negative=%.3f\n",++negative_time,negative_disp);
					resultBuilder.append("第"+(++negative_time)+"个Negative="+negative_disp+"\n");//对位移进行输出
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					flag=place+1;
					//continue;
				}
				else//大于阈值的最后一段位移的情况
				{
					if(pre_status!=2)
						negative_disp=0;
					for(k=flag;k<length;k++)
					    negative_disp+=(v[k]+v[k-1])/(2*(length-1));
					//printf("第%d个Negative=%.3f\n",++negative_time,negative_disp);
					resultBuilder.append("第"+(++negative_time)+"个Negative="+negative_disp+"\n");
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					if(pre_status==1)//如果上一个阶段向上
				    {
						if(Math.abs(positive_disp/negative_disp)>limit && pre_info!=1)
						{
							resultBuilder.append("向上移动了。\n");
							pre_info=1;
						}
						else if(Math.abs(positive_disp/negative_disp)<1.0/limit && pre_info!=2)
						{
							resultBuilder.append("向下移动了。\n");
							pre_info=2;
						}
						else if(pre_info!=0)
						{
							resultBuilder.append("未移动。\n");
							pre_info=0;
						}
				    }
				    else//如果这个是第一个阶段或上一个阶段也是向下
						{
				    	if(negative_disp>0 && pre_info!=1)
	    				{
	    					resultBuilder.append("向上移动了。\n");
	    					pre_info=1;
	    				}
				    	else if(negative_disp<0 && pre_info!=2)
				    	{
				    		resultBuilder.append("向下移动了。\n");
				    		pre_info=2;
				    	}
				    	else if( pre_info!=0)
				    	{
				    		resultBuilder.append("未移动。\n");
				    		pre_info=0;
				    	}
				    }
					break;
				}
				//对于找到了零点但是又不是最后一个阶段进行标记
				if(pre_status==2 || pre_status==0)//上一个超过阈值的是向下，那么不用重新输出
				{
					pre_status=2;
					continue;
				}
				if(pre_status==1)//如果上一个超过阈值的是向上，那么就要重新判断后输出了
				{
					if(Math.abs(positive_disp/negative_disp)>limit && pre_info!=1)//上一个发出的消息不是向上就输出向上
					{
						resultBuilder.append("向上移动了。\n");
						pre_info=1;
					}
					else if(Math.abs(positive_disp/negative_disp)<(1.0/limit) && pre_info!=2)
					{
						resultBuilder.append("向下移动了。\n");
						pre_info=2;
					}
					else if(pre_info!=0)
					{
						resultBuilder.append("未移动。\n");
						pre_info=0;
					}
					pre_status=0;//做出判断之后归零，等待下一次判断
				}
		    }
		}
		if(resultBuilder.length()!=0)
		    editText2.setText(resultBuilder.toString());
		else
			editText2.setText("未移动。");
		return d[length-1];
	}
	
	public void Initialize()
	{
		int i;
		for(i=0;i<section;i++)
		{
			acc_z[i]=0;
		}
	}

}

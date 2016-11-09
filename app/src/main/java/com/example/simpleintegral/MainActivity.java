package com.example.simpleintegral;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.EditText;

public class MainActivity extends Activity implements SensorEventListener{

	SensorManager sensorManager;
	EditText editText1;
	EditText editText2;
	public static final float G=(float) 10;//g=9.8，但是由于传感器的原因设置为10
	long pre_time=System.currentTimeMillis();//定义时间轴
	float[] pre_acc={0,0,0};//存储上一个周期内的加速度值
	float[] pre_ori={0,0,0};//存储上一个周期内的方向值
	int section=2000;
	int count=0;
	boolean flag=true;
	float[] acc_z=new float[section];//大约能存储10s的数据量
    float x=0;
    float y=0;
    float z=0;
	float velocity=0;
	float pre_velocity=velocity;
	float displacement=0;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//editText=(EditText) findViewById(R.id.resultText);
		editText1=(EditText) findViewById(R.id.editText1);
		editText2=(EditText) findViewById(R.id.editText2);
		Initialize();
		sensorManager = (SensorManager) getSystemService(
				Context.SENSOR_SERVICE);
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
    	sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    	sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onStop()
	{
		// 取消注册
		sensorManager.unregisterListener(this);
		super.onStop();
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
		case Sensor.TYPE_ACCELEROMETER:
			    if(values[2]!=pre_acc[2] && flag)
			    {
			    	acc_z[count]=(float) (pre_acc[2]-z+0.10);
			    	count++;
			    }
			    if(System.currentTimeMillis()-pre_time>=3000 && flag)
			    {
			    	float disp=Displacement(acc_z, count);
			    	StringBuilder resultBuilder=new StringBuilder();
			    	resultBuilder.append("Displacement="+Math.round(disp*100)/100.0);
			    	resultBuilder.append(",count="+count);
			    	editText1.setText(resultBuilder.toString());
			    	flag=false;
			    }
			    //保存变量
			    pre_acc[0]=values[0];
			    pre_acc[1]=values[1];
			    pre_acc[2]=values[2];
			    break; 
		case Sensor.TYPE_ORIENTATION:
		        //保存变量
		        pre_ori[0]=values[0];
			    pre_ori[1]=values[1];
			    pre_ori[2]=values[2];
		        break;
		default:
			    break;
		}
		//给出纠正后的三轴加速度值
		x=(float) Math.abs(G*Math.sin(pre_ori[2]*Math.PI/180));
		y=(float) Math.abs(G*Math.sin(pre_ori[1]*Math.PI/180)*Math.cos(pre_ori[2]*Math.PI/180));
		z=(float) Math.abs(G*Math.cos((Math.abs(pre_ori[1])>=Math.abs(pre_ori[2])?
				Math.abs(pre_ori[1]):Math.abs(pre_ori[2]))*Math.PI/180)*Math.cos((Math.abs(pre_ori[1])>=Math.abs(pre_ori[2])?pre_ori[2]:pre_ori[1])*Math.PI/180));
		
	}

	// 当传感器精度改变时回调该方法。
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}
	
	int findNextZero(float a[],int len,int origin)
	{
		if(origin+2>len)
		    return -1;
		for(int i=origin;i<=len-2;i++)
			if(a[i]*a[i+1]<0 || a[i]==0)
			    return i;
		return -1;//若没有找到 
	}
	
	float findMax(float a[],int start,int end)//start<=i<=end
	{
		float max=a[start];
		for(int i=start;i<=end;i++)
			if(a[i]>max)
				max=a[i];
		return max;
	}
	float findMin(float a[],int start,int end)//start<=i<=end
	{
		float min=a[start];
		for(int i=start;i<=end;i++)
			if(a[i]<min)
				min=a[i];
		return min;
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
		    d[i]=d[i-1]+(v[i]+v[i-1])/(2*(length-1));
		//printf("\n");
		//for(i=0;i<length;i++)
		//    printf("d[%d]=%.3f\n",i,d[i]);
		//printf("\n");
		//正负位移测试
	    int pre_status=0;
		int flag=0;
		int positive_time=0;
		int negative_time=0;
		float positive_disp=0;
		float negative_disp=0;
		int place;
		int k;
		float limit=(float) Math.sqrt(5);
		int pre_info=-1;
		StringBuilder resultBuilder=new StringBuilder();
		for(i=flag;i<length&&flag<length;i++)
		{
			if(v[flag]*v[flag+1]<=0)
		    {
		    	flag++;
		    	continue;
		    }
		    if(v[flag]>0 && v[flag+1]>0)
		    {
		    	place=findNextZero(v,length,flag);
		    	if(Math.abs(findMax(a, flag, place==-1?length-1:place))<=2 && Math.abs(findMin(a, flag, place==-1?length-1:place))<=2)
		    	{
		    		//resultBuilder.append("v>0的时候太小了！flag="+flag+",place="+place+"\n");
		    		//resultBuilder.append("Max="+findMax(v, flag, place)+",Min="+findMin(v, flag, place)+"\n");
		    		if(place==-1)
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
				if(place!=-1)
				{
					//printf("i=%d,flag=%d,place=%d\n",i,flag,place);
					positive_disp=0;
					for(k=flag;k<=place;k++)
					    positive_disp+=(v[k]+v[k-1])/(2*(length-1));
					//printf("第%d个Positive=%.3f\n",++positive_time,positive_disp);
					resultBuilder.append("第"+(++positive_time)+"个Positive="+positive_disp+"\n");
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					flag=place+1;
					//continue;
				}
				else
				{
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
		    if(v[flag]<0 && v[flag+1]<0)
		    {
		    	place=findNextZero(v,length,flag);
		    	if(Math.abs(findMax(a, flag, place==-1?length-1:place))<=2 && Math.abs(findMin(a, flag, place==-1?length-1:place))<=2)
		    	{
		    		//resultBuilder.append("v<0的时候太小了！flag="+flag+",place="+place+"\n");
		    		if(place==-1)
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
		    		flag=place+1;
		    		continue;
		    	}
				if(place!=-1)
				{
					//printf("i=%d,flag=%d,place=%d\n",i,flag,place);
					negative_disp=0;
					for(k=flag;k<=place;k++)
					    negative_disp+=(v[k]+v[k-1])/(2*(length-1));
					//printf("第%d个Negative=%.3f\n",++negative_time,negative_disp);
					resultBuilder.append("第"+(++negative_time)+"个Negative="+negative_disp+"\n");
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					flag=place+1;
					//continue;
				}
				else
				{
					negative_disp=0;
					for(k=flag;k<length;k++)
					    negative_disp+=(v[k]+v[k-1])/(2*(length-1));
					//printf("第%d个Negative=%.3f\n",++negative_time,negative_disp);
					resultBuilder.append("第"+(++negative_time)+"个Negative="+negative_disp+"\n");
					//resultBuilder.append("flag="+flag+",place="+place+"\n");
					if(pre_status==1)
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
				    	else if(pre_info!=0 && pre_info!=0)
				    	{
				    		resultBuilder.append("未移动。\n");
				    		pre_info=0;
				    	}
				    }
					break;
				}
				if(pre_status==2 || pre_status==0)
				{
					pre_status=2;
					continue;
				}
				if(pre_status==1)
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

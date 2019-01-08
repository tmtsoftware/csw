package csw.benchmark.time;

public class TimeJNI {  // Save as HelloJNI.java
   static {
      System.loadLibrary("time"); // Load native library hello.dll (Windows) or libhello.so (Unixes)
                                   //  at runtime
                                   // This library contains a native method called sayHello()
   }
 
   // Declare an instance native method sayHello() which receives no parameter and returns void
   	public native TimeSpec gettime();
	//private native TimeSpec createTimeSpec(lon);
  
   // Test Driver
   public static void main(String[] args) throws InterruptedException{
//   	System.out.println("HellO");
//   	TimeJNI tjni = new TimeJNI();
//   	int i=1000;
//   	while(i>=0){
//		TimeSpec ots = tjni.gettime();
//		System.out.println(ots.seconds+" "+ots.nanoseconds);
//		Thread.sleep(1);
//		i--;
//        }
	
   }
}

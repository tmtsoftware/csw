#include "csw_benchmark_time_TimeJNI.h"
#include <time.h>

JNIEXPORT jobject JNICALL Java_csw_benchmark_time_TimeJNI_gettime
  (JNIEnv *env, jobject thisObj){
  
  struct timespec ts;
  clock_gettime(0,&ts); //tv_sec,tv_nsec
  jclass timeSpecClass = (*env)->FindClass(env, "csw/benchmark/time/TimeSpec");
  jobject newTS = (*env)->AllocObject(env, timeSpecClass);
  
  jfieldID secField = (*env)->GetFieldID(env, timeSpecClass, "seconds", "J");
  jfieldID nsecField = (*env)->GetFieldID(env, timeSpecClass, "nanoseconds", "J");
  
  (*env)->SetLongField(env, newTS, secField, ts.tv_sec);
  (*env)->SetLongField(env, newTS, nsecField, ts.tv_nsec);
  
  return newTS;
  
  }

package com.healthtracker.framework.util;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;


import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;


public class RestoreUtils {

    public static void onSaveInstanceState(Activity activity,@NonNull Bundle outState) {
        Field[] fields = activity.getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        Annotation[] ans;
        for (Field f : fields) {
            ans = f.getDeclaredAnnotations();
            for (Annotation an : ans) {
                if (an instanceof Restore) {
                    try {
                        Object o = f.get(activity);
                        if (o == null) {
                            continue;
                        }
                        String fieldName = f.getName();
                        if (o instanceof Integer) {
                            outState.putInt(fieldName, f.getInt(activity));
                        } else if (o instanceof String) {
                            outState.putString(fieldName, (String) f.get(activity));
                        } else if (o instanceof Long) {
                            outState.putLong(fieldName, f.getLong(activity));
                        } else if (o instanceof Short) {
                            outState.putShort(fieldName, f.getShort(activity));
                        } else if (o instanceof Boolean) {
                            outState.putBoolean(fieldName, f.getBoolean(activity));
                        } else if (o instanceof Byte) {
                            outState.putByte(fieldName, f.getByte(activity));
                        } else if (o instanceof Character) {
                            outState.putChar(fieldName, f.getChar(activity));
                        } else if (o instanceof CharSequence) {
                            outState.putCharSequence(fieldName, (CharSequence) f.get(activity));
                        } else if (o instanceof Float) {
                            outState.putFloat(fieldName, f.getFloat(activity));
                        } else if (o instanceof Double) {
                            outState.putDouble(fieldName, f.getDouble(activity));
                        } else if (o instanceof String[]) {
                            outState.putStringArray(fieldName, (String[]) f.get(activity));
                        } else if (o instanceof Parcelable) {
                            outState.putParcelable(fieldName, (Parcelable) f.get(activity));
                        } else if (o instanceof Serializable) {
                            outState.putSerializable(fieldName, (Serializable) f.get(activity));
                        } else if (o instanceof Bundle) {
                            outState.putBundle(fieldName, (Bundle) f.get(activity));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

  
    public static void onRestoreInstanceState(Activity activity, @NonNull Bundle savedInstanceState) {
        Field[] fields = activity.getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        Annotation[] ans;
        for (Field f : fields) {
            ans = f.getDeclaredAnnotations();
            for (Annotation an : ans) {
                if (an instanceof Restore) {
                    try {
                        String fieldName = f.getName();
                        Class cls = f.getType();
                        if (cls == int.class || cls == Integer.class) {
                            f.setInt(activity, savedInstanceState.getInt(fieldName));
                        } else if (String.class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getString(fieldName));
                        } else if (Serializable.class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getSerializable(fieldName));
                        } else if (cls == long.class || cls == Long.class) {
                            f.setLong(activity, savedInstanceState.getLong(fieldName));
                        } else if (cls == short.class || cls == Short.class) {
                            f.setShort(activity, savedInstanceState.getShort(fieldName));
                        } else if (cls == boolean.class || cls == Boolean.class) {
                            f.setBoolean(activity, savedInstanceState.getBoolean(fieldName));
                        } else if (cls == byte.class || cls == Byte.class) {
                            f.setByte(activity, savedInstanceState.getByte(fieldName));
                        } else if (cls == char.class || cls == Character.class) {
                            f.setChar(activity, savedInstanceState.getChar(fieldName));
                        } else if (CharSequence.class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getCharSequence(fieldName));
                        } else if (cls == float.class || cls == Float.class) {
                            f.setFloat(activity, savedInstanceState.getFloat(fieldName));
                        } else if (cls == double.class || cls == Double.class) {
                            f.setDouble(activity, savedInstanceState.getDouble(fieldName));
                        } else if (String[].class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getStringArray(fieldName));
                        } else if (Parcelable.class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getParcelable(fieldName));
                        } else if (Bundle.class.isAssignableFrom(cls)) {
                            f.set(activity, savedInstanceState.getBundle(fieldName));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
     
    }
}

/**
 * Copyright(c) 2011 TeleNav, Inc.
 *
 * @Title: SharedPreferences.java
 * @Package com.telenav.dwf.aws
 * @author jwdong 
 * @date 2013-9-3
 * @version V1.0  
 */
package com.telenav.amazon.s3.tvmclient;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: SharedPreferences
 * @author <a href="mailto:jwdong@telenav.cn">DongJiawei</a>
 */
public class SharedPreferences {
    private static Map<String, String> cacheMap = new HashMap<String, String>();
    private static SharedPreferences reference = new SharedPreferences();
    private SharedPreferences(){
    }
    public static SharedPreferences getInstance(){
        return reference;
    }
 
    public void putString(String key, String value){
        if(key != null && value != null){
            cacheMap.put(key, value);
        }
    }
    public String getString(String key){
        if(key == null){
            return null;
        }
        return cacheMap.get(key);
    }
}

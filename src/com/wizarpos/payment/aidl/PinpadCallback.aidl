// ICloudPayCallback.aidl
package com.wizarpos.payment.aidl;

// Declare any non-default types here with import statements

interface PinpadCallback {
    void processCallbackOnlinePin(int data);
   	void processCallbackOfflinePin( int nCount, int nExtra);
}
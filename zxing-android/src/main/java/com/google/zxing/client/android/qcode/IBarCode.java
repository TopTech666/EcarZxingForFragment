package com.google.zxing.client.android.qcode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;

/*
 *===============================================
 *
 * 文件名:${type_name}
 *
 * 描述: 
 *
 * 作者:
 *
 * 版权所有:深圳市亿车科技有限公司
 *
 * 创建日期: ${date} ${time}
 *
 * 修改人:   金征
 *
 * 修改时间:  ${date} ${time} 
 *
 * 修改备注: 
 *
 * 版本:      v1.0 
 *
 *===============================================
 */
public interface IBarCode {

    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);

    Activity getActivity();

    CameraManager getCameraManager();

    Handler getHandler();
}

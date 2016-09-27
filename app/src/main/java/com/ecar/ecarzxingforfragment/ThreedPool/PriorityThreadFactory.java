package com.ecar.ecarzxingforfragment.ThreedPool;/*
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

import android.os.Process;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory implements ThreadFactory {
    private final String mName;
    private final int mPriority;
    private final AtomicInteger mNumber = new AtomicInteger();

    public PriorityThreadFactory(String name, int priority) {
        mName = name;// 线程池的名称
        mPriority = priority;//线程池的优先级
    }
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, mName +"-"+mNumber.getAndIncrement()){
            @Override
            public void run() {
                // 设置线程的优先级
                Process.setThreadPriority(mPriority);
                super.run();
            }
        };
    }
}

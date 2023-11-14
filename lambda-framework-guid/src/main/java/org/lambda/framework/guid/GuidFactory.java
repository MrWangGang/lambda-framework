package org.lambda.framework.guid;


import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.lambda.framework.guid.enums.GuidExceptionEnum.*;


/**
 * @description: GUID 采用雪花算法
 * @author: Mr.WangGang
 * @create: 2018-11-19 下午 1:09
 **/
@Component
public class GuidFactory {
    // 起始的时间戳
    private final static long START_STMP = 1480166465631L;
    // 每一部分占用的位数，就三个
    private final static long SEQUENCE_BIT = 12;// 序列号占用的位数
    private final static long MACHINE_BIT = 5; // 机器标识占用的位数
    private final static long DATACENTER_BIT = 5;// 数据中心占用的位数
    // 每一部分最大值
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
    // 每一部分向左的位移
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    @Value("${lambda.guid.datacenter-id:-1}")
    public void setDatacenterId(Long datacenterId) {
        if(datacenterId == null || datacenterId == -1L){
            throw new EventException(ES_GUID_002);
        }
        this.datacenterId = datacenterId;
    }
    @Value("${lambda.guid.machine-id:-1}")
    public void setMachineId(Long machineId) {
        if(machineId == null || machineId == -1L){
            throw new EventException(ES_GUID_003);
        }
        this.machineId = machineId;
    }

    private Long datacenterId;
    private Long machineId;
    private long sequence = 0L; // 序列号
    private long lastStmp = -1L;// 上一次时间戳
    public String GUID(){
         try {
             return String.valueOf(nextId());
         }catch (Exception e){
                throw new EventException(ES_GUID_001);
         }
    }
    //产生下一个ID
    private synchronized long nextId() {
        long currStmp = getNewstmp();
        if (currStmp < lastStmp) {
            throw new EventException(ES_GUID_000);
        }

        if (currStmp == lastStmp) {
            //if条件里表示当前调用和上一次调用落在了相同毫秒内，只能通过第三部分，序列号自增来判断为唯一，所以+1.
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大，只能等待下一个毫秒
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            //执行到这个分支的前提是currTimestamp > lastTimestamp，说明本次调用跟上次调用对比，已经不再同一个毫秒内了，这个时候序号可以重新回置0了。
            sequence = 0L;
        }

        lastStmp = currStmp;
        //就是用相对毫秒数、机器ID和自增序号拼接
        return (currStmp - START_STMP) << TIMESTMP_LEFT //时间戳部分
                | datacenterId << DATACENTER_LEFT      //数据中心部分
                | machineId << MACHINE_LEFT            //机器标识部分
                | sequence;                            //序列号部分
    }

    private long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    private long getNewstmp() {
        return System.currentTimeMillis();
    }

}

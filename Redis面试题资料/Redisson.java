setnx
	1、死锁
	2、误删锁
	3、锁续命
	4、锁可重入
	5、锁一致性
	
redisoon：解决了上述的一些问题
	可重入锁（基于hash结构）：源码
	// waitTime：获取锁的等待时间（指定时间之类，如果没有获取到锁，会一直尝试获取锁）
	// leaseTime：锁的释放时间，如果是-1，代表开启了看门狗机制，默认就是30秒且每隔10秒续命一次至30秒
	// unit：前面两个时间的单位
	// threadId：当前线程ID
	// command
	RFuture<T> tryLockInnerAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
        internalLockLeaseTime = unit.toMillis(leaseTime);

        return evalWriteAsync(getName(), LongCodec.INSTANCE, command,
                "if (redis.call('exists', KEYS[1]) == 0) then " +
				"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
				"redis.call('pexpire', KEYS[1], ARGV[1]); " +
				"return nil; " +
				"end; " +
				
				"if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
				"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
				"redis.call('pexpire', KEYS[1], ARGV[1]); " +
				"return nil; " +
				"end; " +
				"return redis.call('pttl', KEYS[1]);",
		Collections.singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
    }
	
	//lua脚本逻辑
	KEYS : Collections.singletonList(getName()) //锁名称，new Rlock时指定的   coupon:001
	ARGV :
		internalLockLeaseTime  //锁的有效期     
		getLockName(threadId)  // 当前线程ID
		
	1、判断coupon:001这个key在redis中是否存在，如果不存在（redis.call('exists', KEYS[1]) == 0）： 
		原子性操作1.1 + 1.2
		1.1、利用hincrby命令直接设置hashkey、hashvalue
			coupon:001
				899a91d98d899123122: 1
		1.2、利用pexpire命令为当前锁设置过期时间
			效果：为coupon:001这把锁，设置了指定的过期时间

	2、判断大key coupon:001下的hashkey 	899a91d98d899123122是否存在，如果存在
		原子性操作2.1 + 2.2
		2.1、利用hincrby命令对锁可重入次数+1
			coupon:001
				899a91d98d899123122: 4
		2.2、利用pexpire命令为当前锁设置过期时间
			效果：为coupon:001这把锁，设置了指定的过期时间

		
		
	
package com.aol.cyclops.react.async.future;

import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import uk.co.real_logic.agrona.concurrent.ManyToOneConcurrentArrayQueue;
/**
 * Single consumer / multiple producer future pool
 * 
 * @author johnmcclean
 *
 */
@AllArgsConstructor
public class FuturePool {

	private final ManyToOneConcurrentArrayQueue<FastFuture> pool;
	private final int max;
	
	
	public<T> FastFuture<T> next(Supplier<FastFuture<T>> factory){
		if(pool.size()>0){
			
			FastFuture next = pool.poll();
			next.clearFast();
			return next;
		}
		
		return factory.get();
	}
	
	public <T> void done(FastFuture<T> f){
		if(pool.size()<max){
			
			pool.offer(f);
		}
		
	}	
}
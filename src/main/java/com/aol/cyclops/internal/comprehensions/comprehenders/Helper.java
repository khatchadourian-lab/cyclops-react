package com.aol.cyclops.internal.comprehensions.comprehenders;

import java.util.Collection;
import java.util.Iterator;

import com.aol.cyclops.types.extensability.Comprehender;

public class Helper {

	public static <T> T first(Comprehender<T> comp,Collection c){
		Iterator<T> it = c.iterator();
		if(it.hasNext())
			return comp.of(it.next());
		return comp.empty();
	}
}

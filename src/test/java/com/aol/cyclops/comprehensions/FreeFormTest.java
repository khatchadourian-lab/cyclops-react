package com.aol.cyclops.comprehensions;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.aol.cyclops.control.For;
public class FreeFormTest {

	
	
	
	@Test
	public void freeFormDo(){
		List<Integer> list= Arrays.asList(1,2,3);
		List<Integer> stream = For.iterable(list)
								.yield((Integer a)-> a +2).unwrap();
				
										
		
		assertThat(Arrays.asList(3,4,5),equalTo(stream));
										
	}
	@Test
	public void freeFormDoWithFilter(){
		List<Integer> list= Arrays.asList(1,2,3);
		List<Integer> stream = For.iterable(list)
								.filter((Integer a) -> a>2)
								.yield((Integer a)-> a +2).unwrap();
				
										
		
		assertThat(Arrays.asList(5),equalTo(stream));
										
	}
	@Test
	public void freeFormDo2(){
		List<Integer> stream = For.iterable(asList(20,30))
								   .iterable(i->asList(1,2,3))
								   .yield((Integer a)-> (Integer b) -> a + b+2).unwrap();
		
		assertThat(stream,equalTo(Arrays.asList(23,24,25,33,34,35)));
			
	}
	@Test
	public void freeFormDo3(){
		List<Integer> stream = For.iterable(asList(20,30))
								   .iterable(i->asList(1,2,3))
								   .yield((Integer a)-> (Integer b) -> a + b+2).unwrap();
		
		assertThat(stream,equalTo(Arrays.asList(23,24,25,33,34,35)));
			
	}
	
	
	
}

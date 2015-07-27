package com.aol.cyclops.lambda.monads;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.aol.cyclops.internal.AsGenericMonad;
import com.aol.cyclops.internal.Monad;
import com.aol.cyclops.lambda.api.AsStreamable;
import com.aol.cyclops.lambda.api.Monoid;
import com.aol.cyclops.lambda.api.Streamable;
import com.aol.cyclops.lambda.api.Unwrapable;
import com.aol.cyclops.streams.HeadAndTail;
import com.aol.cyclops.streams.Pair;
import com.aol.cyclops.streams.Quadruple;
import com.aol.cyclops.streams.StreamUtils;
import com.aol.cyclops.streams.Triple;
import com.nurkiewicz.lazyseq.LazySeq;


public class SequenceM<T> implements Unwrapable, Stream<T>, Iterable<T>{
	private final Stream<T> monad;
	
	SequenceM(Stream<T> stream){
		monad = stream;
	}
	static <T> SequenceM<T> monad(Stream<T> stream){
		return new SequenceM(stream);
	}
	
	public final <R> R unwrap(){
		return (R)monad;
	}

	/**
	 * join / flatten one level of a nested hierarchy
	 * 
	 * <pre>
	 * {@code 
	 *  assertThat(SequenceM.<Integer>of(Arrays.asList(1,2)).flatten().toList().size(),equalTo(asList(1,  2).size()));		
	 *  
	 *  // or more advanced example 
	 *  
	 *  AnyM<AnyM<Integer>> applied =anyM(Optional.of(2))
			 							.simpleFilter(anyM(Streamable.of( (Integer a)->a>5 ,(Integer a) -> a<3)));
	
	     assertThat(applied.toSequence().flatten().toList(),equalTo(Arrays.asList(2)));
	 * 
	 * }
	 * 
	 * </pre>
	 * 
	 * @return Flattened / joined one level
	 */
	public final <T1> SequenceM<T1> flatten(){
		return AsGenericMonad.monad(monad).flatten().sequence();
		
	}
	/**
	 * Type safe unwrap
	 * 
	 * <pre>
	 * {@code 
	 * Stream<String> stream = anyM("hello","world").asSequence().unwrapStream();
		assertThat(stream.collect(Collectors.toList()),equalTo(Arrays.asList("hello","world")));
	 * }
	 * </pre>
	 * @return Stream with current wrapped values
	 */
	public final Stream<T> unwrapStream(){
		
		Stream unwrapper = Stream.of(1);
		return (Stream)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	/**
	 * Type safe unwrap 
	 * <pre>
	 * {@code 
	 * Optional<List<String>> stream = anyM("hello","world")
											.asSequence()
											.unwrapOptional();
		assertThat(stream.get(),equalTo(Arrays.asList("hello","world")));
	 * }
	 * 
	 * </pre>
	 * @return
	 */
	public final Optional<List<T>> unwrapOptional(){
		
		Optional unwrapper = Optional.of(1);
		return (Optional)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	/**
	 * <pre>
	 * {@code 
	 * CompletableFuture<List<String>> cf = anyM("hello","world")
											.asSequence()
											.unwrapCompletableFuture();
		assertThat(cf.join(),equalTo(Arrays.asList("hello","world")));
	 * }
	 * </pre>
	 * @return
	 */
	public final CompletableFuture<List<T>> unwrapCompletableFuture(){
		
		CompletableFuture unwrapper = CompletableFuture.completedFuture(1);
		return (CompletableFuture)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	
	/**
	 * Convert to a Stream with the values repeated specified times
	 * 
	 * <pre>
	 * {@code 
	 * 		assertThat(anyM(Stream.of(1,2,2)).asSequence()
											.cycle(3).collect(Collectors.toList()),
											equalTo(Arrays.asList(1,2,2,1,2,2,1,2,2)));

	 * 
	 * }
	 * </pre>
	 * @param times
	 *            Times values should be repeated within a Stream
	 * @return Stream with values repeated
	 */
	public final SequenceM<T> cycle(int times) {
		return new SequenceM(StreamUtils.cycle(times,AsStreamable.asStreamable(monad)));
	}
	/**
	 * Convert to a Stream with the values infinitely cycled
	 * 
	 * <pre>
	 * {@code 
	 *   assertEquals(asList(1, 1, 1, 1, 1,1),of(1).cycle().limit(6).toList());
	 *   }
	 * </pre>
	 * 
	 * @return Stream with values repeated
	 */
	public final SequenceM<T> cycle() {
		return new SequenceM(StreamUtils.cycle(monad));
	}
	/**
	 * Duplicate a Stream
	 * <pre>
	 * {@code 
	 *  Pair<SequenceM<Integer>, SequenceM<Integer>> copies =of(1,2,3,4,5,6).duplicate();
		 assertTrue(copies.v1.anyMatch(i->i==2));
		 assertTrue(copies.v2.anyMatch(i->i==2));
	 * 
	 * }
	 * </pre>
	 * 
	 * @return duplicated stream
	 */
	public final Pair<SequenceM<T>,SequenceM<T>> duplicate(){
		Streamable<T> streamable = Streamable.fromStream(monad);
		return new Pair(streamable.sequenceM(),streamable.sequenceM());
	}
	@SuppressWarnings("unchecked")
	public final Triple<SequenceM<T>,SequenceM<T>,SequenceM<T>> triplicate(){
		Streamable<T> streamable = Streamable.fromStream(monad);
		return new Triple(streamable.sequenceM(),streamable.sequenceM(),streamable.sequenceM());
	}
	@SuppressWarnings("unchecked")
	public final Quadruple<SequenceM<T>,SequenceM<T>,SequenceM<T>,SequenceM<T>> quadruplicate(){
		Streamable<T> streamable = Streamable.fromStream(monad);
		return new Quadruple(streamable.sequenceM(),streamable.sequenceM(),streamable.sequenceM(),streamable.sequenceM());
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Pair<Optional<T>,SequenceM<T>> splitAtHead(){
		Pair<SequenceM<T>,SequenceM<T>> pair = splitAt(1);
		return new Pair(pair.v1.unwrapOptional()
							.flatMap( l-> l.size()>0 ? Optional.of(l.get(0)) : Optional.empty()  )
							,pair.v2);
	}
	public final Pair<SequenceM<T>,SequenceM<T>> splitAt(int where){
		Pair<SequenceM<T>,SequenceM<T>> pair = duplicate();
		return new Pair(pair.v1.limit(where),pair.v2.skip(where));
	}
	public final Pair<SequenceM<T>,SequenceM<T>> splitBy(Predicate<T> splitter){
		Pair<SequenceM<T>,SequenceM<T>> pair = duplicate();
		return new Pair(pair.v1.limitWhile(splitter),pair.v2.skipWhile(splitter));
	}
	
	public final static <T,U> Pair<SequenceM<T>,SequenceM<U>> unzip(SequenceM<Pair<T,U>> sequence){
		Pair<SequenceM<Pair<T,U>>,SequenceM<Pair<T,U>>> pair = sequence.duplicate();
		return new Pair(pair.v1.map(Pair::_1),pair.v2.map(Pair::_2));
	}
	public final static <T1,T2,T3> Triple<SequenceM<T1>,SequenceM<T2>,SequenceM<T3>> unzip3(SequenceM<Triple<T1,T2,T3>> sequence){
		Triple<SequenceM<Triple<T1,T2,T3>>,SequenceM<Triple<T1,T2,T3>>,SequenceM<Triple<T1,T2,T3>>> triple = sequence.triplicate();
		return new Triple(triple.v1.map(Triple::_1),triple.v2.map(Triple::_2),triple.v3.map(Triple::_3));
	}
	public final static <T1,T2,T3,T4> Quadruple<SequenceM<T1>,SequenceM<T2>,SequenceM<T3>,SequenceM<T4>> unzip4(SequenceM<Quadruple<T1,T2,T3,T4>> sequence){
		Quadruple<SequenceM<Quadruple<T1,T2,T3,T4>>,SequenceM<Quadruple<T1,T2,T3,T4>>,SequenceM<Quadruple<T1,T2,T3,T4>>,SequenceM<Quadruple<T1,T2,T3,T4>>> quad = sequence.quadruplicate();
		return new Quadruple(quad.v1.map(Quadruple::_1),quad.v2.map(Quadruple::_2),quad.v3.map(Quadruple::_3),quad.v4.map(Quadruple::_4));
	}
	
	/**
	 * Convert to a Stream with the result of a reduction operation repeated
	 * specified times
	 * 
	 * <pre>
	 * {@code 
	 *   		List<Integer> list = AsGenericMonad,asMonad(Stream.of(1,2,2))
	 * 										.cycle(Reducers.toCountInt(),3)
	 * 										.collect(Collectors.toList());
	 * 	//is asList(3,3,3);
	 *   }
	 * </pre>
	 * 
	 * @param m
	 *            Monoid to be used in reduction
	 * @param times
	 *            Number of times value should be repeated
	 * @return Stream with reduced values repeated
	 */
	public final SequenceM<T> cycle(Monoid<T> m, int times) {
		return monad(StreamUtils.cycle(times,AsStreamable.asStreamable(m.reduce(monad))));
		
	}

	private Stream<T> createLazySeq() {
		return monad;
	}
	/**
	 * 
	 * Convert to a Stream, repeating the resulting structure specified times
	 * and lifting all values to the specified Monad type
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Optional&lt;Integer&gt;&gt; list = monad(Stream.of(1, 2)).cycle(Optional.class,
	 * 			2).toList();
	 * 
	 * 	// is asList(Optional.of(1),Optional.of(2),Optional.of(1),Optional.of(2) ));
	 * 
	 * }
	 * </pre>
	 * 
	 * 
	 * 
	 * @param monadC
	 *            class type
	 * @param times
	 * @return
	 */
	public final <R> SequenceM<R> cycle(Class<R> monadC, int times) {
		return (SequenceM)cycle(times).map(r -> new ComprehenderSelector().selectComprehender(monadC).of(r));	
	}

	/**
	 * Repeat in a Stream while specified predicate holds
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	public final SequenceM<T> cycleWhile(Predicate<? super T> predicate) {
		return monad(LazySeq.of(StreamUtils.cycle(monad).iterator()).takeWhile(predicate).stream());
	}

	/**
	 * Repeat in a Stream until specified predicate holds
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	public final SequenceM<T> cycleUntil(Predicate<? super T> predicate) {
		return monad(LazySeq.of(StreamUtils.cycle(monad).iterator()).takeWhile(predicate.negate()).stream());
	}
	public final <S> SequenceM<Pair<T,S>> zip(Stream<? extends S> second){
		return zipStream(second,(a,b)->new Pair<>(a,b));
	}
	public final <S,U> SequenceM<Triple<T,S,U>> zip3(Stream<? extends S> second,Stream<? extends U> third){
		return zip(second).zip(third).map(p -> new Triple(p._1()._1(),p._1()._2(),p._2()));
		
	}
	public final <T2,T3,T4> SequenceM<Quadruple<T,T2,T3,T4>> zip4(Stream<T2> second,Stream<T3> third,Stream<T4> fourth){
		return zip3(second,third).zip(fourth).map(t ->  new Quadruple(t._1()._1(), t._1()._2(),t._1()._3(),t._2()));
		
	}
	public final  SequenceM<Pair<T,Long>> zipWithIndex(){
		return zipStream(LongStream.iterate(0, i->i+1),(a,b)->new Pair<>(a,b));
	}
	/**
	 * Generic zip function. E.g. Zipping a Stream and an Optional
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Stream&lt;List&lt;Integer&gt;&gt; zipped = asMonad(Stream.of(1, 2, 3)).zip(
	 * 			asMonad(Optional.of(2)), (a, b) -&gt; Arrays.asList(a, b));
	 * 	// [[1,2]]
	 * }
	 * </pre>
	 * 
	 * @param second
	 *            Monad to zip with
	 * @param zipper
	 *            Zipping function
	 * @return Stream zipping two Monads
	 */
	public final <S, R> SequenceM<R> zip(SequenceM<? extends S> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return monad((Stream)LazySeq.of(createLazySeq().iterator()).zip(LazySeq.of(second.createLazySeq().iterator()), zipper).stream());
		
	}
	public final <S, R> SequenceM<R> zip(AnyM<? extends S> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return zip(second.toSequence(), zipper);
	}

	/**
	 * Zip this Monad with a Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Stream&lt;List&lt;Integer&gt;&gt; zipped = asMonad(Stream.of(1, 2, 3)).zip(
	 * 			Stream.of(2, 3, 4), (a, b) -&gt; Arrays.asList(a, b));
	 * 
	 * 	// [[1,2][2,3][3,4]]
	 * }
	 * </pre>
	 * 
	 * @param second
	 *            Stream to zip with
	 * @param zipper
	 *            Zip funciton
	 * @return This monad zipped with a Stream
	 */
	public final <S, R> SequenceM<R> zipStream(BaseStream<? extends S,? extends BaseStream<? extends S,?>> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return monad(StreamUtils.zipStream(monad,second, zipper));
	}

	/**
	 * Create a sliding view over this monad
	 * 
	 * @param windowSize
	 *            Size of sliding window
	 * @return Stream with sliding view over monad
	 */
	public final SequenceM<List<T>> sliding(int windowSize) {
		return monad(StreamUtils.sliding(monad,windowSize));
	}

	/**
	 * Group elements in a Monad into a Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;List&lt;Integer&gt;&gt; list = monad(Stream.of(1, 2, 3, 4, 5, 6)).grouped(3)
	 * 			.collect(Collectors.toList());
	 * 
	 * 	assertThat(list.get(0), hasItems(1, 2, 3));
	 * 	assertThat(list.get(1), hasItems(4, 5, 6));
	 * 
	 * }
	 * </pre>
	 * 
	 * @param groupSize
	 *            Size of each Group
	 * @return Stream with elements grouped by size
	 */
	public final SequenceM<List<T>> grouped(int groupSize) {
		return monad(LazySeq.of(createLazySeq().iterator()).grouped(groupSize).stream());
	}
	public final <K> Map<K, List<T>> groupBy(Function<? super T, ? extends K> classifier) {
		return collect(Collectors.groupingBy(classifier));
	}

	/*
	 * Return the distinct Stream of elements
	 * 
	 * <pre>{@code List<Integer> list =
	 * monad(Optional.of(Arrays.asList(1,2,2,2,5,6)))
	 * .<Stream<Integer>,Integer>streamedMonad() .distinct()
	 * .collect(Collectors.toList()); }</pre>
	 */
	public final SequenceM<T> distinct() {
		return monad(monad.distinct());
	}

	/**
	 * Scan left using supplied Monoid
	 * 
	 * <pre>
	 * {@code  
	 * 
	 * 	assertEquals(asList("", "a", "ab", "abc"),monad(Stream.of("a", "b", "c")).scanLeft(Reducers.toString("")).toList());
	 *         
	 *         }
	 * </pre>
	 * 
	 * @param monoid
	 * @return
	 */
	public final SequenceM<T> scanLeft(Monoid<T> monoid) {
		return monad(StreamUtils.scanLeft(monad,monoid));
	}
	public final SequenceM<T> scanLeft(T identity,BiFunction<T, T, T>  combiner) {
		
		return scanLeft(Monoid.of(identity,combiner));
	}
	
	public final SequenceM<T> scanRight(Monoid<T> monoid) {
		return monad(StreamUtils.scanLeft(reverse(),monoid));
	}
	public final<U> SequenceM<T> scanRight(T identity,BiFunction<T,T,T>  combiner) {
		return scanRight(Monoid.of(identity,combiner));
	}
	

	
	/**
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7)).sorted().toList(),equalTo(Arrays.asList(3,4,6,7))); }
	 * </pre>
	 * 
	 */
	public final SequenceM<T> sorted() {
		return monad(monad.sorted());
	}

	/**
	 *
	 * 
	 * 
	 * @param c
	 *            Compartor to sort with
	 * @return Sorted Monad
	 */
	public final SequenceM<T> sorted(Comparator<? super T> c) {
		return monad(monad.sorted(c));
	}

	/**
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().skip(2).toList(),equalTo(Arrays.asList(6,7))); }
	 * </pre>
	 * 
	
	 * 
	 * @param num
	 *            Number of elemenets to skip
	 * @return Monad converted to Stream with specified number of elements
	 *         skipped
	 */
	public final SequenceM<T> skip(long num) {
		return new SequenceM( monad.skip(num));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code
	 * assertThat(anyM(Stream.of(4,3,6,7)).asSequence().sorted().skipWhile(i->i<6).toList(),equalTo(Arrays.asList(6,7)));
	 * }
	 * </pre>
	 * 
	 * @param p
	 *            Predicate to skip while true
	 * @return Monad converted to Stream with elements skipped while predicate
	 *         holds
	 */
	public final SequenceM<T> skipWhile(Predicate<? super T> p) {
		return monad(StreamUtils.skipWhile(monad, p));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().skipUntil(i->i==6).toList(),equalTo(Arrays.asList(6,7)));}
	 * </pre>
	 * 
	 * 
	 * @param p
	 *            Predicate to skip until true
	 * @return Monad converted to Stream with elements skipped until predicate
	 *         holds
	 */
	public final SequenceM<T> skipUntil(Predicate<? super T> p) {
		return monad(StreamUtils.skipUntil(monad,p));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().limit(2).toList(),equalTo(Arrays.asList(4,3)));}
	 * </pre>
	 * 
	 * @param num
	 *            Limit element size to num
	 * @return Monad converted to Stream with elements up to num
	 */
	public final SequenceM<T> limit(long num) {
		return monad(monad.limit(num));
	}

	/**
	 *
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().sorted().limitWhile(i->i<6).toList(),equalTo(Arrays.asList(3,4)));}
	 * </pre>
	 * 
	 * @param p
	 *            Limit while predicate is true
	 * @return Monad converted to Stream with limited elements
	 */
	public final SequenceM<T> limitWhile(Predicate<? super T> p) {
		return monad(StreamUtils.limitWhile(monad,p));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).limitUntil(i->i==6).toList(),equalTo(Arrays.asList(4,3))); }
	 * </pre>
	 * 
	 * @param p
	 *            Limit until predicate is true
	 * @return Monad converted to Stream with limited elements
	 */
	public final SequenceM<T> limitUntil(Predicate<? super T> p) {
		return monad(StreamUtils.limitUntil(monad,p));
	}
	/**
	 * @return this monad converted to a Parallel Stream, via streamedMonad() wraped in the SequenceM interface
	 */
	public final SequenceM<T> parallel(){
		return (SequenceM)monad(monad.parallel());
	}
	
	/**
	 * True if predicate matches all elements when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if all match
	 */
	public final  boolean  allMatch(Predicate<? super T> c) {
		return monad.allMatch(c);
	}
	/**
	 * True if a single element matches when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if any match
	 */
	public final  boolean  anyMatch(Predicate<? super T> c) {
		return monad.anyMatch(c);
	}
	public  boolean xMatch(int num, Predicate<? super T> c) {
		return monad.filter(t -> c.test(t)) .collect(Collectors.counting()) == num;
	}
	public final  boolean  noneMatch(Predicate<? super T> c) {
		return monad.allMatch(c.negate());
	}
	public final  String join(){
		return StreamUtils.join(monad,"");
	}
	public final  String join(String sep){
		return StreamUtils.join(monad,sep);
	}
	public final   String join(String sep,String start, String end){
		return StreamUtils.join(monad,start, sep, end);
	}
	
	
	public final  <C extends Comparable<? super C>> Optional<T> minBy(Function<T,C> f){
		return StreamUtils.minBy(monad,f);
	}
	public final   Optional<T> min(Comparator<? super T> comparator){
		return  StreamUtils.min(monad,comparator);
	}
	public final  <C extends Comparable<? super C>> Optional<T> maxBy(Function<T,C> f){
		
		return StreamUtils.maxBy(monad,f);
	}
	public final  Optional<T> max(Comparator<? super T> comparator){
		return StreamUtils.max(monad,comparator);
	}
	
	
	
	/**
	 * extract head and tail together
	 * 
	 * <pre>
	 * {@code
	 *  SequenceM<String> helloWorld = SequenceM.of("hello","world","last");
		HeadAndTail<String> headAndTail = helloWorld.headAndTail();
		 String head = headAndTail.head();
		 assertThat(head,equalTo("hello"));
		
		SequenceM<String> tail =  headAndTail.tail();
		assertThat(tail.headAndTail().head(),equalTo("world"));
	 * }
	 * </pre>
	 * @return
	 */
	public final  HeadAndTail<T> headAndTail(){
		return StreamUtils.headAndTail(monad);
	}
	public final  Optional<HeadAndTail<T>> headAndTailOptional(){
		return StreamUtils.headAndTailOptional(monad);
	}
	
	/**
	 * @return First matching element in sequential order
	 * 
	 * (deterministic)
	 * 
	 */
	public final  Optional<T>  findFirst() {
		return monad.findFirst();
	}
	/**
	 * @return first matching element,  but order is not guaranteed
	 * 
	 * (non-deterministic) 
	 */
	public final  Optional<T>  findAny() {
		return monad.findAny();
	}
	
	/**
	 * Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final  <R> R mapReduce(Monoid<R> reducer){
		return reducer.mapReduce(monad);
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid, using supplied function
	 *  Then use Monoid to reduce values
	 *  
	 * @param mapper Function to map Monad type
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final  <R> R mapReduce(Function<? super T,? extends R> mapper, Monoid<R> reducer){
		return reducer.reduce(monad.map(mapper));
	}
	
	/**
	 * Mutable reduction / collection over this Monad converted to a Stream
	 * 
	 * @param collector Collection operation definition
	 * @return Collected result
	 */
	public final <R, A> R collect(Collector<? super T, A, R> collector){
		return monad.collect(collector);
	}
	public final  <R> R collect(Supplier<R> supplier,
            BiConsumer<R, ? super T> accumulator,
            BiConsumer<R, R> combiner){
		return monad.collect(supplier, accumulator, combiner);
	}
	/**
	 * Apply multiple collectors Simulataneously to this Monad
	 * 
	 * <pre>{@code
	  	List result =SequenceM.of(1,2,3).collect(Stream.of(Collectors.toList(),
	  															Collectors.summingInt(Integer::intValue),
	  															Collectors.averagingInt(Integer::intValue)));
		
		assertThat(result.get(0),equalTo(Arrays.asList(1,2,3)));
		assertThat(result.get(1),equalTo(6));
		assertThat(result.get(2),equalTo(2.0));
		}</pre>
		
	 * 
	 * @param collectors Stream of Collectors to apply
	 * @return  List of results
	 */
	public final  List collect(Stream<Collector> collectors){
		return StreamUtils.collect(monad,collectors);
	}
	/**
	 *  Apply multiple Collectors, simultaneously to a Stream
	 * <pre>
	 * {@code 
	 * List result = SequenceM.of(1,2,3).collect(
								Arrays.asList(Collectors.toList(),
								Collectors.summingInt(Integer::intValue),
								Collectors.averagingInt(Integer::intValue)));
		
		assertThat(result.get(0),equalTo(Arrays.asList(1,2,3)));
		assertThat(result.get(1),equalTo(6));
		assertThat(result.get(2),equalTo(2.0));
	 * }
	 * </pre>
	 * @param stream Stream to collect
	 * @param collectors Collectors to apply
	 * @return Result as a list
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <R> List<R> collectIterable(Iterable<Collector> collectors){
		return StreamUtils.collect(monad, collectors);
	}
	
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values
	 * @return reduced values
	 */
	public final  T reduce(Monoid<T> reducer){
		
		return reducer.reduce(monad);
	}
	/* 
	 * <pre>
	 * {@code 
	 * assertThat(SequenceM.of(1,2,3,4,5).map(it -> it*100).reduce( (acc,next) -> acc+next).get(),equalTo(1500));
	 * }
	 * </pre>
	 * 
	 */
	public final Optional<T> reduce(BinaryOperator<T> accumulator){
		 return monad.reduce(accumulator);
	 } 
	 public final T reduce(T identity, BinaryOperator<T> accumulator){
		 return monad.reduce(identity, accumulator);
	 }
	 public final <U> U reduce(U identity,
             BiFunction<U, ? super T, U> accumulator,
             BinaryOperator<U> combiner){
		 return monad.reduce(identity, accumulator, combiner);
	 }
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	public final List<T> reduce(Stream<Monoid<T>> reducers){
		return StreamUtils.reduce(monad, reducers);
	}
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	public final List<T> reduce(Iterable<Monoid<T>> reducers){
		return StreamUtils.reduce(monad, reducers);
	}
	
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldLeft
	 * @return Reduced result
	 */
	public final T foldLeft(Monoid<T> reducer){
		return reduce(reducer);
	}
	public final T foldLeft(T identity,  BinaryOperator<T> accumulator){
		 return monad.reduce(identity, accumulator);
	 }
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final <T> T foldLeftMapToType(Monoid<T> reducer){
		return reducer.mapReduce(monad);
	}
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldRight
	 * @return Reduced result
	 */
	public final T foldRight(Monoid<T> reducer){
		return reducer.reduce(StreamUtils.reverse(monad));
	}
	public final T foldRight(T identity,  BinaryOperator<T> accumulator){
		 return foldRight(Monoid.of(identity, accumulator));
	 }
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final <T> T foldRightMapToType(Monoid<T> reducer){
		return reducer.mapReduce(StreamUtils.reverse(monad));
	}
	/**
	 * @return Underlying monad converted to a Streamable instance
	 */
	public final Streamable<T> toStreamable(){
		return  AsStreamable.asStreamable(stream());
	}
	/**
	 * @return This monad converted to a set
	 */
	public final Set<T> toSet(){
		return (Set)monad.collect(Collectors.toSet());
	}
	/**
	 * @return this monad converted to a list
	 */
	public final List<T> toList(){
	
		return (List)monad.collect(Collectors.toList());
	}
	public final <C extends Collection<T>> C toCollection(Supplier<C> collectionFactory) {
        return monad.collect(Collectors.toCollection(collectionFactory));
    }
	/**
	 * @return  calls to stream() but more flexible on type for inferencing purposes.
	 */
	public final <T> Stream<T> toStream(){
		return (Stream<T>) monad;
	}
	/**
	 * Unwrap this Monad into a Stream.
	 * If the underlying monad is a Stream it is returned
	 * Otherwise we flatMap the underlying monad to a Stream type
	 */
	public final Stream<T> stream(){
		 return monad;
			
	}
	
	/**
	 * 
	 * <pre>{@code 
	 * assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3)));
	 * }</pre>
	 * 
	 * @param iterable
	 * @return True if Monad starts with Iterable sequence of data
	 */
	public final boolean startsWith(Iterable<T> iterable){
		return StreamUtils.startsWith(monad,iterable);
		
	}
	/**
	 * 	<pre>{@code assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3).iterator())) }</pre>

	 * @param iterator
	 * @return True if Monad starts with Iterators sequence of data
	 */
	public final boolean startsWith(Iterator<T> iterator){
		return StreamUtils.startsWith(monad,iterator);
		
	}
	
	public AnyM<T> anyM(){
		return new AnyM<>(AsGenericMonad.asMonad(monad));
	}
	public final  <R> SequenceM<R> map(Function<? super T,? extends R> fn){
		return new SequenceM(monad.map(fn));
	}
	public final   SequenceM<T>  peek(Consumer<? super T> c) {
		return new SequenceM(monad.peek(c));
	}
	/**
	 * flatMap operation
	 * 
	 * @param fn
	 * @return
	 */
	public final <R> SequenceM<R> flatMap(Function<? super T,? extends Stream<? extends R>> fn) {
		
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapAnyM(Function<? super T,AnyM<? extends R>> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in).unwrap()).sequence();
	}
	/**
	 * Convenience method & performance optimisation
	 * 
	 * flatMapping to a Stream will result in the Stream being converted to a List, if the host Monad
	 * type is not a Stream. (i.e.
	 *  <pre>
	 *  {@code  
	 *   AnyM<Integer> opt = anyM(Optional.of(20));
	 *   Optional<List<Integer>> optionalList = opt.flatMap( i -> anyM(Stream.of(1,2,i))).unwrap();  
	 *   
	 *   //Optional [1,2,20]
	 *  }</pre>
	 *  
	 *  In such cases using Arrays.asList would be more performant
	 *  <pre>
	 *  {@code  
	 *   AnyM<Integer> opt = anyM(Optional.of(20));
	 *   Optional<List<Integer>> optionalList = opt.flatMapCollection( i -> asList(1,2,i))).unwrap();  
	 *   
	 *   //Optional [1,2,20]
	 *  }</pre>
	 * @param fn
	 * @return
	 */
	public final <R> SequenceM<R> flatMapCollection(Function<? super T,Collection<? extends R>> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapStream(Function<? super T,BaseStream<? extends R,?>> fn) {
		 Monad<Object,T> m = AsGenericMonad.asMonad(monad);
		return m.flatMap(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapOptional(Function<? super T,Optional<? extends R>> fn) {
		 Monad<Object,T> m = AsGenericMonad.asMonad(monad);
		 return m.flatMap(in -> fn.apply(in)).sequence();
	//	return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapCompletableFuture(Function<? super T,CompletableFuture<? extends R>> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapLazySeq(Function<? super T,LazySeq<? extends R>> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	
	/**
	 * Perform a flatMap operation where the result will be a flattened stream of Characters
	 * from the CharSequence returned by the supplied function.
	 * 
	 * <pre>
	 * {@code 
	 *   List<Character> result = anyM("input.file")
									.asSequence()
									.liftAndBindCharSequence(i->"hello world")
									.toList();
		
		assertThat(result,equalTo(Arrays.asList('h','e','l','l','o',' ','w','o','r','l','d')));
	 * }
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<Character> liftAndBindCharSequence(Function<? super T,CharSequence> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	 *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied files.
	 * 
	 * <pre>
	 * {@code
	 * 
		List<String> result = anyM("input.file")
								.asSequence()
								.map(getClass().getClassLoader()::getResource)
								.peek(System.out::println)
								.map(URL::getFile)
								.liftAndBindFile(File::new)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * }
	 * 
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<String> liftAndBindFile(Function<? super T,File> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	 *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied URLs 
	 * 
	 * <pre>
	 * {@code 
	 * List<String> result = anyM("input.file")
								.asSequence()
								.liftAndBindURL(getClass().getClassLoader()::getResource)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * }
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<String> liftAndBindURL(Function<? super T, URL> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	  *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied BufferedReaders
	 * 
	 * <pre>
	 * List<String> result = anyM("input.file")
								.asSequence()
								.map(getClass().getClassLoader()::getResourceAsStream)
								.map(InputStreamReader::new)
								.liftAndBindBufferedReader(BufferedReader::new)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * </pre>
	 * 
	 * 
	 * @param fn
	 * @return
	 */
	public final SequenceM<String> liftAndBindBufferedReader(Function<? super T,BufferedReader> fn) {
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	public final   SequenceM<T>  filter(Predicate<? super T> fn){
		return AsGenericMonad.<Stream<T>,T>asMonad(monad).filter(fn).sequence();
	}
	public void forEach(Consumer<? super T> action) {
		monad.forEach(action);
		
	}
	
	public Iterator<T> iterator() {
		return monad.iterator();
	}
	
	public Spliterator<T> spliterator() {
		return monad.spliterator();
	}
	
	public boolean isParallel() {
		return monad.isParallel();
	}
	
	public SequenceM<T> sequential() {
		return monad(monad.sequential());
	}
	
	
	public SequenceM<T> unordered() {
		return monad(monad.unordered());
	}
	
	
	
	
	public IntStream mapToInt(ToIntFunction<? super T> mapper) {
		return monad.mapToInt(mapper);
	}
	
	public LongStream mapToLong(ToLongFunction<? super T> mapper) {
		return monad.mapToLong(mapper);
	}
	
	public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
		return monad.mapToDouble(mapper);
	}
	
	public IntStream flatMapToInt(
			Function<? super T, ? extends IntStream> mapper) {
		return monad.flatMapToInt(mapper);
	}
	
	public LongStream flatMapToLong(
			Function<? super T, ? extends LongStream> mapper) {
		return monad.flatMapToLong(mapper);
	}
	
	public DoubleStream flatMapToDouble(
			Function<? super T, ? extends DoubleStream> mapper) {
		return monad.flatMapToDouble(mapper);
	}

	
	public void forEachOrdered(Consumer<? super T> action) {
		monad.forEachOrdered(action);
		
	}
	
	public Object[] toArray() {
		return monad.toArray();
	}
	
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return monad.toArray(generator);
	}
	
	public long count() {
		return monad.count();
	}
	/**
	 * Returns a stream with a given value interspersed between any two values
	 * of this stream.
	 * 
	 * 
	 * // (1, 0, 2, 0, 3, 0, 4) SequenceM.of(1, 2, 3, 4).intersperse(0)
	 * 
	 */
	public  SequenceM<T> intersperse(T value) {
	
		return new SequenceM(monad.flatMap(t -> Stream.of(value,t)).skip(1l));
	}
	/**
	 * Keep only those elements in a stream that are of a given type.
	 * 
	 * 
	 * // (1, 2, 3) SequenceM.of(1, "a", 2, "b",3).ofType(Integer.class)
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <U> SequenceM<U> ofType(Class<U> type) {
		return SequenceM.fromStream(monad.filter(type::isInstance).map(t -> (U) t));
	}
	/**
	 * Cast all elements in a stream to a given type, possibly throwing a
	 * {@link ClassCastException}.
	 * 
	 * 
	 * // ClassCastException SequenceM.of(1, "a", 2, "b", 3).cast(Integer.class)
	 * 
	 */
	public <U> SequenceM<U> cast(Class<U> type) {
		return SequenceM.fromStream(monad.map(type::cast));
	}
	/**
	 * Lazily converts this SequenceM into a Collection. This does not trigger the Stream. E.g.
	 * Collection is not thread safe on the first iteration.
	 * <pre>
	 * {@code 
	 * Collection<Integer> col = SequenceM.of(1,2,3,4,5)
											.peek(System.out::println)
											.toLazyCollection();
		System.out.println("first!");
		col.forEach(System.out::println);
	 * }
	 * 
	 * //Will print out "first!" before anything else
	 * </pre>
	 * @return
	 */
	public Collection<T> toLazyCollection(){
		return StreamUtils.toLazyCollection(monad);
	}
	/**
	 * Lazily converts this SequenceM into a Collection. This does not trigger the Stream. E.g.
	 * 
	 * <pre>
	 * {@code 
	 * Collection<Integer> col = SequenceM.of(1,2,3,4,5)
											.peek(System.out::println)
											.toConcurrentLazyCollection();
		System.out.println("first!");
		col.forEach(System.out::println);
	 * }
	 * 
	 * //Will print out "first!" before anything else
	 * </pre>
	 * @return
	 */
	public Collection<T> toConcurrentLazyCollection(){
		return StreamUtils.toConcurrentLazyCollection(monad);
	}
	
	/**
	 * @return Streamable that can replay this SequenceM
	 */
	public Streamable<T> toLazyStreamable(){
		return AsStreamable.asStreamable(monad);
	}
	/**
	 * @return Streamable that replay this SequenceM
	 */
	public Streamable<T> toConcurrentLazyStreamable(){
		return AsStreamable.asConcurrentStreamable(monad);
	}
	public SequenceM<T> reverse(){
		return new SequenceM(StreamUtils.reverse(monad));
	}
	
	/**
	 * Construct a Sequence from the provided elements
	 * @param elements To Construct sequence from
	 * @return
	 */
	public static <T> SequenceM<T> of(T... elements){
		return new SequenceM(Stream.of(elements));
	}
	/**
	 * Construct a Reveresed Sequence from the provided elements
	 * @param elements To Construct sequence from
	 * @return
	 */
	public static <T> SequenceM<T> reversedOf(T... elements){
		return new SequenceM(StreamUtils.reversedStream(Arrays.asList(elements)));
	}
	/**
	 * Construct a Reveresed Sequence from the provided elements
	 * @param elements To Construct sequence from
	 * @return
	 */
	public static <T> SequenceM<T> reversedListOf(List<T> elements){
		return new SequenceM(StreamUtils.reversedStream(elements));
	}
	/**
	 * Construct a Sequence from a Stream
	 * @param stream Stream to construct Sequence from
	 * @return
	 */
	public static <T> SequenceM<T> fromStream(Stream<T> stream){
		if(stream instanceof SequenceM)
			return (SequenceM)stream;
		return new SequenceM(stream);
	}
	
	/**
	 * Construct a Sequence from an Iterable
	 * @param iterable  to construct Sequence from
	 * @return SequenceM
	 */
	public static <T> SequenceM<T> fromIterable(Iterable<T> iterable){
		return new SequenceM(StreamUtils.stream(iterable));
	}
	@Override
	public Stream<T> onClose(Runnable closeHandler) {
		
		return this;
	}
	@Override
	public void close() {
		
		
	}
	public static <T> SequenceM<T> empty() {
		return SequenceM.of();
	}
	
}

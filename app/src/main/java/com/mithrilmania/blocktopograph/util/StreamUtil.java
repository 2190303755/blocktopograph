package com.mithrilmania.blocktopograph.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    public static <A, B, R> Stream<R> zip(
            Stream<? extends A> first,
            Stream<? extends B> second,
            BiFunction<? super A, ? super B, ? extends R> zipper) {

        Spliterator<? extends A> spliterator1 = first.spliterator();
        Spliterator<? extends B> spliterator2 = second.spliterator();

        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<>(
                        Math.min(spliterator1.estimateSize(), spliterator2.estimateSize()),
                        spliterator1.characteristics() & spliterator2.characteristics()
                ) {
                    final Iterator<? extends A> left = Spliterators.iterator(spliterator1);
                    final Iterator<? extends B> right = Spliterators.iterator(spliterator2);

                    @Override
                    public boolean tryAdvance(Consumer<? super R> action) {
                        if (left.hasNext() && right.hasNext()) {
                            action.accept(zipper.apply(left.next(), right.next()));
                            return true;
                        } else {
                            return false;
                        }
                    }
                },
                false
        );
    }
}

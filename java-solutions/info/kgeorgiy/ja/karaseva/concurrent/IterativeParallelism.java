package info.kgeorgiy.ja.karaseva.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class IterativeParallelism implements ScalarIP {
    private final ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<List<? extends T>> getSublists(int threads, List<? extends T> values) {
        int sizeBlock = values.size() / threads;
        final List<List<? extends T>> blocks = new ArrayList<>();
        int currentIndex = 0;
        for (int i = 0; i < threads; i++) {
            if (i == (threads - (values.size() % threads))) {
                sizeBlock++;
            }
            blocks.add(values.subList(currentIndex, currentIndex + sizeBlock));
            currentIndex += sizeBlock;
        }
        return blocks;
    }

    private <T, E, R> R getResult(int threads, List<? extends T> values,
                                  Function<List<? extends T>, E> getAns,
                                  Function<List<? extends E>, R> mergeAns) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("threads = " + threads + ", expected threads >= 1");
        }
        threads = Math.max(1, Math.min(threads, values.size()));
        final List<List<? extends T>> lists = getSublists(threads, values);

        final List<E> partialAnswers;
        if (mapper == null) {
            partialAnswers = new ArrayList<>(Collections.nCopies(threads, null));
            List<Thread> workers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int finalI = i;
                workers.add(new Thread(() -> partialAnswers.set(finalI, getAns.apply(lists.get(finalI)))));
                workers.get(i).start();
            }
            for (Thread worker : workers) {
                worker.join();
            }
        } else {
            partialAnswers = mapper.map(getAns, lists);
        }
        return mergeAns.apply(partialAnswers);

    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> getMax = (list) -> list.stream().max(comparator).orElseThrow();
        return getResult(threads, values, getMax, getMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getResult(threads, values, (list) -> list.stream().allMatch(predicate), (list) -> list.stream().allMatch(b -> b));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}

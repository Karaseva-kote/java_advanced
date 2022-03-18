package info.kgeorgiy.ja.karaseva.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> workers;
    private final Queue<Runnable> tasks;

    public ParallelMapperImpl(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1");
        }

        workers = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        solve();
                    }
                } catch (InterruptedException ignored) {
                }
            }));
            workers.get(i).start();
        }
    }

    private void solve() throws InterruptedException {
        Runnable currentTask;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            currentTask = tasks.poll();
        }
        currentTask.run();
    }

    private void add(Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        CheckerFinish check = new CheckerFinish();

        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            add(() -> {
                result.set(finalI, f.apply(args.get(finalI)));
                check.increment();
            });
        }

        return check.getList(result);
    }

    @Override
    public void close() {
        workers.forEach((i) -> {
            i.interrupt();
            try {
                i.join();
            } catch (InterruptedException ignored) {
            }
        });
    }

    public static class CheckerFinish {
        private int cnt = 0;

        public synchronized <T> List<T> getList(List<T> list) throws InterruptedException {
            while (cnt != list.size()) {
                this.wait();
            }
            return list;
        }

        public synchronized void increment() {
            cnt++;
            this.notify();
        }
    }
}
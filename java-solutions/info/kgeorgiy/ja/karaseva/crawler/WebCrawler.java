package info.kgeorgiy.ja.karaseva.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;

    private final ExecutorService loaders;
    private final ExecutorService extractors;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.loaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1");
        }
        // :NOTE: either explain why you use concurrentskiplist or use more efficient ds
        Set<String> downloaded = new ConcurrentSkipListSet<>();
        Set<String> urls = new ConcurrentSkipListSet<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Phaser phaser = new Phaser(1);
        downloadLink(url, depth, downloaded, urls, errors, phaser);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void downloadLink(String url, int depth, Set<String> downloaded, Set<String> urls,
                              Map<String, IOException> errors, Phaser phaser) {
        if (urls.add(url)) {
            try {
                Document document = downloader.download(url);
                downloaded.add(url);
                if (depth > 1) {
                    phaser.register();
                    extractors.execute(() -> {
                        try {
                            parseLinkFromDocument(document, depth - 1, downloaded, urls, errors, phaser);
                        } catch (IOException e) {
                            errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                }
            } catch (IOException e) {
                errors.put(url, e);
            }
        }
    }

    private void parseLinkFromDocument(Document document, int depth, Set<String> downloaded, Set<String> urls,
                                       Map<String, IOException> errors, Phaser phaser) throws IOException {
        document.extractLinks().forEach((link) -> {
            phaser.register();
            loaders.execute(() -> {
                downloadLink(link, depth, downloaded, urls, errors, phaser);
                phaser.arrive();
            });
        });
    }

    @Override
    public void close() {
        loaders.shutdownNow();
        extractors.shutdownNow();
    }

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("args[] can not be null");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("arguments can not be null");
            return;
        }

        if (args.length != 5) {
            System.err.println("expect 5 arguments: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        String url = args[0];

        int[] arguments = new int[4];
        for (int i = 1; i < 5; i++) {
            try {
                arguments[i] = Integer.parseInt(args[i]);
            } catch (NumberFormatException e) {
                System.err.println(args[i] + " isn't correct number");
                return;
            }
        }

        try {
            WebCrawler crawler = new WebCrawler(new CachingDownloader(), arguments[1], arguments[2], arguments[3]);
            crawler.download(url, arguments[0]);
        } catch (IOException e) {
            System.err.println("error occurred while downloading");
        }
    }
}

package com.fde.gallery3d.ui.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.common.MediaRepository;
import com.fde.gallery3d.ui.utils.LogTools;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 分类的全局调度器：单线程后台逐张对未分类的图片做推理并缓存结果，
 * 结果实时通知到所有注册的页面（分类页）。
 */
public class AiMediaClassifier {

    public interface Listener {
        void onClassified(Multimedia media, AiCategory category);

        void onFinished();
    }

    private static volatile AiMediaClassifier sInstance;

    private final Context appContext;
    private final Set<Listener> listeners = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean rerun = new AtomicBoolean(false);

    public static AiMediaClassifier getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AiMediaClassifier.class) {
                if (sInstance == null) {
                    sInstance = new AiMediaClassifier(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private AiMediaClassifier(Context context) {
        appContext = context;
    }

    /**
     * 注册监听后开始（或继续）后台分类；重复注册无效。
     */
    public void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        ensureRunning();
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void ensureRunning() {
        if (!running.compareAndSet(false, true)) {
            // 正在运行：结束前再补一轮，保证后注册的页面也能拿到完整结果
            rerun.set(true);
            return;
        }
        executor.execute(this::processAll);
    }

    private void processAll() {
        try {
            List<Multimedia> all = MediaRepository.loadAll(appContext);
            for (Multimedia media : all) {
                if (media.getMediaType() != Constant.MEDIA_PIC) {
                    continue;
                }
                if (AiCategoryStore.isCached(appContext, media.getPath())) {
                    continue;
                }
                AiCategory category = ImageClassifier.getInstance(appContext)
                        .classify(media.getPath());
                AiCategoryStore.put(appContext, media.getPath(), category);
                final AiCategory result = category;
                for (Listener listener : listeners) {
                    mainHandler.post(() -> listener.onClassified(media, result));
                }
            }
        } catch (Throwable e) {
            LogTools.e("AiMediaClassifier process failed: " + e);
        } finally {
            running.set(false);
            for (Listener listener : listeners) {
                mainHandler.post(listener::onFinished);
            }
            if (rerun.getAndSet(false)) {
                ensureRunning();
            }
        }
    }
}

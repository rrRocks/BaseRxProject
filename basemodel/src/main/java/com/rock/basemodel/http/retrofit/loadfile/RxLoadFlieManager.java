package com.rock.basemodel.http.retrofit.loadfile;

import android.util.Log;


import com.rock.basemodel.http.Api;
import com.rock.basemodel.http.ApiServer;
import com.rock.basemodel.http.basebean.DownloadInfo;
import com.rock.basemodel.utils.AppStoragePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * @author: ruan
 * @date: 2020/4/14
 * 用于文件下载
 */
public class RxLoadFlieManager {

    private volatile static RxLoadFlieManager instance;
    HashMap<String, ProgressDownSubscriber> submap;
    private OkHttpClient okHttpClient;
    private static String mDownloadPath;


    public static RxLoadFlieManager getInstance() {
        if (instance == null) {
            synchronized (RxLoadFlieManager.class) {
                if (instance == null) {
                    instance = new RxLoadFlieManager();
                }
            }
        }
        return instance;
    }

    private RxLoadFlieManager() {
        submap = new HashMap<>();
    }

    /**
     * 开始下载
     *
     * @param url              下载地址
     * @param downFileCallback 进度回调接口
     */
    public void download(final String url, final DownFileCallback downFileCallback) {
        /*正在下载不处理*/
        if (url == null || submap.get(url) != null) {
            return;
        }

        Downloadinterceptor interceptor = new Downloadinterceptor(downFileCallback, url);
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(AppStoragePath.getBasUrl(Api.loadUrl))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        final ApiServer httpservice = retrofit.create(ApiServer.class);

        ProgressDownSubscriber subscriber =
                Observable.just(url)
                        .flatMap(new Function<String, ObservableSource<DownloadInfo>>() {
                            @Override
                            public ObservableSource<DownloadInfo> apply(String s) throws Exception {
                                Log.i("load", "----->> 文件连接地址" + s);
                                return Observable.just(createDownInfo(s));
                            }
                        })
                        .map(new Function<DownloadInfo, DownloadInfo>() {
                            @Override
                            public DownloadInfo apply(DownloadInfo s) throws Exception {
                                return getRealFileName(s);
                            }
                        })
                        .flatMap(new Function<DownloadInfo, Observable<ResponseBody>>() {
                            @Override
                            public Observable<ResponseBody> apply(DownloadInfo downInfo) throws Exception {
                                return httpservice.download("bytes=" + downInfo.getProgress() + "-", downInfo.getUrl());
                            }
                        })//下载
                        .map(new Function<ResponseBody, DownloadInfo>() {
                            @Override
                            public DownloadInfo apply(ResponseBody responsebody) {
                                try {
                                    return writecache(responsebody, url);
                                } catch (IOException e) {
                                    //*失败抛出异常*//
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())//在主线程回调
                        .subscribeOn(Schedulers.io())//在子线程执行
                        .subscribeWith(new ProgressDownSubscriber<DownloadInfo>() {
                            @Override
                            public void onNext(DownloadInfo downInfo) {
                                downFileCallback.onSuccess(downInfo);
                                submap.remove(downInfo.getUrl());
                            }

                            @Override
                            public void onError(Throwable t) {
                                downFileCallback.onFail(t.getMessage());
                                submap.remove(url);
                            }
                        });


        submap.put(url, subscriber);
    }


    /**
     * @param url
     * @return
     */
    public String getTemporaryName(String url) {
        return getTemporaryPath() + url.substring(url.lastIndexOf("/"));
    }

    private String getTemporaryPath() {
        return this.mDownloadPath;
    }

    public RxLoadFlieManager downloadPath(String mDownloadPath) {
        this.mDownloadPath = mDownloadPath;
        if (instance != null) {
            return instance;
        } else {
            return getInstance();
        }
    }

    /**
     * 写入文件
     *
     * @param url
     * @throws IOException
     */
    private DownloadInfo writecache(ResponseBody responsebody, String url) throws IOException {
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        File file = new File(getTemporaryName(url));
        try {
            raf = new RandomAccessFile(getTemporaryName(url), "rw");
            inputStream = responsebody.byteStream();
            byte[] fileReader = new byte[4096];
            raf.seek(file.length());

            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                raf.write(fileReader, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        DownloadInfo downloadInfo = new DownloadInfo(url);
        downloadInfo.setSavepath(getTemporaryName(url));
        downloadInfo.setFileName(file.getName());
        downloadInfo.setProgress(file.length());
        return downloadInfo;
    }


    /**
     * 暂停下载
     */
    public void stop(String url) {
        if (url == null) return;
        if (submap.containsKey(url)) {
            ProgressDownSubscriber subscriber = submap.get(url);
            subscriber.dispose();
            submap.remove(url);
        }
    }

    /**
     * 获取下载长度
     *
     * @param downloadUrl
     * @param mClient
     * @return
     */
    private long getContentLength(OkHttpClient mClient, String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

    /**
     * DownloadInfo
     *
     * @param url 请求网址
     * @return DownloadInfo
     */
    private DownloadInfo createDownInfo(String url) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        long contentLength = getContentLength(okHttpClient, url);//获得文件大小
        downloadInfo.setTotal(contentLength);
        String fileName = url.substring(url.lastIndexOf("/"));
        downloadInfo.setFileName(fileName);
        return downloadInfo;
    }

    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {
        String fileName = downloadInfo.getFileName();
        long downloadLength = 0, contentLength = downloadInfo.getTotal();
        File file = new File(getTemporaryName(downloadInfo.getUrl()));
        downloadInfo.setSavepath(getTemporaryName(downloadInfo.getUrl()));
        if (file.exists()) {
            //找到了文件,代表已经下载过,则获取其长度
            downloadLength = file.length();
        }
        //之前下载过,需要重新来一个文件
        if (downloadLength >= contentLength) {
            file.delete();
        }
        file = new File(getTemporaryName(downloadInfo.getUrl()));
        //设置改变过的文件名/大小
        downloadInfo.setProgress(file.length());
        downloadInfo.setFileName(file.getName());
        downloadInfo.setSavepath(getTemporaryPath() + file.getName());
        return downloadInfo;
    }
}

package com.rock.basemodel.http.retrofit.loadfile;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * @author: ruan
 * @date: 2020/4/14
 * 自定义返回主题类型
 */
public class DownloadResponseBody extends ResponseBody {
    private ResponseBody responseBody;

    //进度回调接口
    private DownFileCallback downFileCallback;

    private BufferedSource bufferedSource;
    private String downUrl;


    public DownloadResponseBody(ResponseBody responseBody, DownFileCallback downFileCallback, String downUrl) {
        this.responseBody = responseBody;
        this.downFileCallback = downFileCallback;
        this.downUrl = downUrl;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;
            File file = new File(RxLoadFlieManager.getInstance().getTemporaryName(downUrl));

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                if (null != downFileCallback) {
                    if (bytesRead != -1) {
                        long loacalSize = file.length();//本地已下载的长度
                        long trueTotal = loacalSize + responseBody.contentLength() - totalBytesRead;//文件真实长度
                        downFileCallback.onProgress(trueTotal, loacalSize);
                    } else {

                    }

                }
                return bytesRead;
            }
        };

    }
}

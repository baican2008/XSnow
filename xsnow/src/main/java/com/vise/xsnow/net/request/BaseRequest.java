package com.vise.xsnow.net.request;

import android.content.Context;

import com.vise.log.ViseLog;
import com.vise.utils.assist.SSLUtil;
import com.vise.xsnow.cache.DiskCache;
import com.vise.xsnow.common.ViseConfig;
import com.vise.xsnow.net.ViseNet;
import com.vise.xsnow.net.api.ApiService;
import com.vise.xsnow.net.callback.ACallback;
import com.vise.xsnow.net.callback.DCallback;
import com.vise.xsnow.net.callback.UCallback;
import com.vise.xsnow.net.config.NetGlobalConfig;
import com.vise.xsnow.net.core.ApiCookie;
import com.vise.xsnow.net.func.ApiFunc;
import com.vise.xsnow.net.func.ApiRetryFunc;
import com.vise.xsnow.net.interceptor.DownloadProgressInterceptor;
import com.vise.xsnow.net.interceptor.HeadersInterceptor;
import com.vise.xsnow.net.interceptor.TagInterceptor;
import com.vise.xsnow.net.interceptor.UploadProgressInterceptor;
import com.vise.xsnow.net.mode.ApiHost;
import com.vise.xsnow.net.mode.CacheMode;
import com.vise.xsnow.net.mode.CacheResult;
import com.vise.xsnow.net.mode.HttpHeaders;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @Description: 请求基类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-28 16:05
 */
public abstract class BaseRequest<R extends BaseRequest> {
    protected NetGlobalConfig netGlobalConfig;//全局配置
    protected ApiService apiService;//通用接口服务
    protected Retrofit retrofit;//Retrofit对象
    protected List<Interceptor> interceptors = new ArrayList<>();//局部请求的拦截器
    protected List<Interceptor> networkInterceptors = new ArrayList<>();//局部请求的网络拦截器
    protected Map<String, String> params = new LinkedHashMap<>();//请求参数
    protected HttpHeaders headers = new HttpHeaders();//请求头
    protected int retryDelayMillis;//请求失败重试间隔时间
    protected int retryCount;//重试次数
    protected String baseUrl;//基础域名
    protected String suffixUrl = "";//链接后缀
    protected Object tag;//请求标签
    protected long readTimeOut;//读取超时时间
    protected long writeTimeOut;//写入超时时间
    protected long connectTimeOut;//连接超时时间
    protected boolean isHttpCache;//是否使用Http缓存
    protected boolean isLocalCache;//是否使用本地缓存
    protected CacheMode cacheMode;//本地缓存类型
    protected String cacheKey;//本地缓存Key
    protected long cacheTime;//本地缓存时间
    protected DCallback downCallback;//下载进度回调
    protected UCallback uploadCallback;//上传进度回调

    /**
     * 添加请求参数
     * @param paramKey
     * @param paramValue
     * @return
     */
    public R addParam(String paramKey, String paramValue) {
        if (paramKey != null && paramValue != null) {
            this.params.put(paramKey, paramValue);
        }
        return (R) this;
    }

    /**
     * 添加请求参数
     * @param params
     * @return
     */
    public R addParams(Map<String, String> params) {
        if (params != null) {
            this.params.putAll(params);
        }
        return (R) this;
    }

    /**
     * 移除请求参数
     * @param paramKey
     * @return
     */
    public R removeParam(String paramKey) {
        if (paramKey != null) {
            this.params.remove(paramKey);
        }
        return (R) this;
    }

    /**
     * 设置请求参数
     * @param params
     * @return
     */
    public R params(Map<String, String> params) {
        if (params != null) {
            this.params = params;
        }
        return (R) this;
    }

    /**
     * 添加请求头
     * @param headerKey
     * @param headerValue
     * @return
     */
    public R addHeader(String headerKey, String headerValue) {
        this.headers.put(headerKey, headerValue);
        return (R) this;
    }

    /**
     * 添加请求头
     * @param headers
     * @return
     */
    public R addHeaders(Map<String, String> headers) {
        this.headers.put(headers);
        return (R) this;
    }

    /**
     * 移除请求头
     * @param headerKey
     * @return
     */
    public R removeHeader(String headerKey) {
        this.headers.remove(headerKey);
        return (R) this;
    }

    /**
     * 设置请求头
     * @param headers
     * @return
     */
    public R headers(HttpHeaders headers) {
        if (headers != null) {
            this.headers = headers;
        }
        return (R) this;
    }

    /**
     * 设置请求失败重试间隔时间（毫秒）
     * @param retryDelayMillis
     * @return
     */
    public R retryDelayMillis(int retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
        return (R) this;
    }

    /**
     * 设置请求失败重试次数
     * @param retryCount
     * @return
     */
    public R retryCount(int retryCount) {
        this.retryCount = retryCount;
        return (R) this;
    }

    /**
     * 设置基础域名，当前请求会替换全局域名
     * @param baseUrl
     * @return
     */
    public R baseUrl(String baseUrl) {
        if (baseUrl != null) {
            this.baseUrl = baseUrl;
        }
        return (R) this;
    }

    /**
     * 设置请求链接后缀
     * @param suffixUrl
     * @return
     */
    public R suffixUrl(String suffixUrl) {
        this.suffixUrl = suffixUrl;
        return (R) this;
    }

    /**
     * 设置请求标签
     * @param tag
     * @return
     */
    public R tag(Object tag) {
        this.tag = tag;
        return (R) this;
    }

    /**
     * 设置连接超时时间（秒）
     *
     * @param connectTimeOut
     * @return
     */
    public R connectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
        return (R) this;
    }

    /**
     * 设置读取超时时间（秒）
     *
     * @param readTimeOut
     * @return
     */
    public R readTimeOut(int readTimeOut) {
        this.readTimeOut = readTimeOut;
        return (R) this;
    }

    /**
     * 设置写入超时时间（秒）
     *
     * @param writeTimeOut
     * @return
     */
    public R writeTimeOut(int writeTimeOut) {
        this.writeTimeOut = writeTimeOut;
        return (R) this;
    }

    /**
     * 设置是否进行HTTP缓存
     * @param isHttpCache
     * @return
     */
    public R setHttpCache(boolean isHttpCache) {
        this.isHttpCache = isHttpCache;
        return (R) this;
    }

    /**
     * 设置是否进行本地缓存
     * @param isLocalCache
     * @return
     */
    public R setLocalCache(boolean isLocalCache) {
        this.isLocalCache = isLocalCache;
        return (R) this;
    }

    /**
     * 设置本地缓存类型
     * @param cacheMode
     * @return
     */
    public R cacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
        return (R) this;
    }

    /**
     * 设置本地缓存Key
     *
     * @param cacheKey
     * @return
     */
    public R cacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return (R) this;
    }

    /**
     * 设置本地缓存时间(毫秒)，默认永久
     *
     * @param cacheTime
     * @return
     */
    public R cacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return (R) this;
    }

    /**
     * 局部设置拦截器
     *
     * @param interceptor
     * @return
     */
    public R interceptor(Interceptor interceptor) {
        if (interceptor != null) {
            interceptors.add(interceptor);
        }
        return (R) this;
    }

    /**
     * 局部设置网络拦截器
     *
     * @param interceptor
     * @return
     */
    public R networkInterceptor(Interceptor interceptor) {
        if (interceptor != null) {
            networkInterceptors.add(interceptor);
        }
        return (R) this;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public int getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSuffixUrl() {
        return suffixUrl;
    }

    public long getReadTimeOut() {
        return readTimeOut;
    }

    public long getWriteTimeOut() {
        return writeTimeOut;
    }

    public long getConnectTimeOut() {
        return connectTimeOut;
    }

    public boolean isHttpCache() {
        return isHttpCache;
    }

    public boolean isLocalCache() {
        return isLocalCache;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public <T> Observable<T> request(Type type) {
        generateGlobalConfig();
        generateLocalConfig();
        return execute(type);
    }

    public <T> Observable<CacheResult<T>> cacheRequest(Type type) {
        generateGlobalConfig();
        generateLocalConfig();
        return cacheExecute(type);
    }

    public <T> Subscription request(Context context, ACallback<T> callback) {
        generateGlobalConfig();
        generateLocalConfig();
        return execute(context, callback);
    }

    protected abstract <T> Observable<T> execute(Type type);

    protected abstract <T> Observable<CacheResult<T>> cacheExecute(Type type);

    protected abstract <T> Subscription execute(Context context, ACallback<T> callback);

    protected <T> Observable.Transformer<ResponseBody, T> norTransformer(final Type type) {
        return new Observable.Transformer<ResponseBody, T>() {
            @Override
            public Observable<T> call(Observable<ResponseBody> apiResultObservable) {
                return apiResultObservable.subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io()).observeOn
                        (AndroidSchedulers
                                .mainThread()).map(new ApiFunc<T>(type)).retryWhen(new ApiRetryFunc(retryCount, retryDelayMillis));
            }
        };
    }

    /**
     * 生成局部配置
     */
    protected void generateLocalConfig() {
        OkHttpClient.Builder newBuilder = ViseNet.getInstance().getOkHttpClient().newBuilder();

        if (netGlobalConfig.getGlobalParams() != null) {
            params.putAll(netGlobalConfig.getGlobalParams());
        }

        if (netGlobalConfig.getGlobalHeaders() != null) {
            headers.put(netGlobalConfig.getGlobalHeaders());
        }

        if (!interceptors.isEmpty()) {
            for (Interceptor interceptor : interceptors) {
                newBuilder.addInterceptor(interceptor);
            }
        }

        if (!networkInterceptors.isEmpty()) {
            for (Interceptor interceptor : networkInterceptors) {
                newBuilder.addNetworkInterceptor(interceptor);
            }
        }

        if (headers.headersMap.size() > 0) {
            newBuilder.addInterceptor(new HeadersInterceptor(headers.headersMap));
        }

        if (tag != null) {
            newBuilder.addInterceptor(new TagInterceptor(tag));
        }

        if (downCallback != null) {
            newBuilder.addNetworkInterceptor(new DownloadProgressInterceptor(downCallback));
        }

        if (uploadCallback != null) {
            newBuilder.addNetworkInterceptor(new UploadProgressInterceptor(uploadCallback));
        }

        if (retryCount <= 0) {
            retryCount = netGlobalConfig.getRetryCount();
        }

        if (retryDelayMillis <= 0) {
            retryDelayMillis = netGlobalConfig.getRetryDelayMillis();
        }

        if (readTimeOut > 0) {
            newBuilder.readTimeout(readTimeOut, TimeUnit.SECONDS);
        }

        if (writeTimeOut > 0) {
            newBuilder.readTimeout(writeTimeOut, TimeUnit.SECONDS);
        }

        if (connectTimeOut > 0) {
            newBuilder.readTimeout(connectTimeOut, TimeUnit.SECONDS);
        }

        if (isHttpCache) {
            try {
                if (netGlobalConfig.getHttpCache() == null) {
                    netGlobalConfig.httpCache(new Cache(netGlobalConfig.getHttpCacheDirectory(), ViseConfig.CACHE_MAX_SIZE));
                }
                netGlobalConfig.cacheOnline(netGlobalConfig.getHttpCache());
                netGlobalConfig.cacheOffline(netGlobalConfig.getHttpCache());
            } catch (Exception e) {
                ViseLog.e("Could not create http cache" + e);
            }
            newBuilder.cache(netGlobalConfig.getHttpCache());
        }

        if (isLocalCache) {
            if (cacheKey != null) {
                ViseNet.getApiCacheBuilder().cacheKey(cacheKey);
            }
            if (cacheTime > 0) {
                ViseNet.getApiCacheBuilder().cacheTime(cacheTime);
            } else {
                ViseNet.getApiCacheBuilder().cacheTime(DiskCache.CACHE_NEVER_EXPIRE);
            }
        }

        if (baseUrl != null) {
            Retrofit.Builder newRetrofitBuilder = new Retrofit.Builder();
            newRetrofitBuilder.baseUrl(baseUrl);
            if (netGlobalConfig.getConverterFactory() != null) {
                newRetrofitBuilder.addConverterFactory(netGlobalConfig.getConverterFactory());
            }
            if (netGlobalConfig.getCallAdapterFactory() != null) {
                newRetrofitBuilder.addCallAdapterFactory(netGlobalConfig.getCallAdapterFactory());
            }
            if (netGlobalConfig.getCallFactory() != null) {
                newRetrofitBuilder.callFactory(netGlobalConfig.getCallFactory());
            }
            newRetrofitBuilder.client(ViseNet.getInstance().getOkHttpClient());
            retrofit = newRetrofitBuilder.build();
        } else {
            ViseNet.getRetrofitBuilder().client(ViseNet.getInstance().getOkHttpClient());
            retrofit = ViseNet.getRetrofitBuilder().build();
        }

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * 生成全局配置
     */
    protected void generateGlobalConfig() {
        netGlobalConfig = ViseNet.CONFIG();

        if (netGlobalConfig.getBaseUrl() == null) {
            netGlobalConfig.baseUrl(ApiHost.getHost());
        }
        ViseNet.getRetrofitBuilder().baseUrl(netGlobalConfig.getBaseUrl());

        if (netGlobalConfig.getConverterFactory() != null) {
            ViseNet.getRetrofitBuilder().addConverterFactory(netGlobalConfig.getConverterFactory());
        }

        if (netGlobalConfig.getCallAdapterFactory() == null) {
            netGlobalConfig.callAdapterFactory(RxJavaCallAdapterFactory.create());
        }
        ViseNet.getRetrofitBuilder().addCallAdapterFactory(netGlobalConfig.getCallAdapterFactory());

        if (netGlobalConfig.getCallFactory() != null) {
            ViseNet.getRetrofitBuilder().callFactory(netGlobalConfig.getCallFactory());
        }

        if (netGlobalConfig.getHostnameVerifier() == null) {
            netGlobalConfig.hostnameVerifier(new SSLUtil.UnSafeHostnameVerifier(netGlobalConfig.getBaseUrl()));
        }
        ViseNet.getOkHttpBuilder().hostnameVerifier(netGlobalConfig.getHostnameVerifier());

        if (netGlobalConfig.getSslSocketFactory() == null) {
            netGlobalConfig.SSLSocketFactory(SSLUtil.getSslSocketFactory(null, null, null));
        }
        ViseNet.getOkHttpBuilder().sslSocketFactory(netGlobalConfig.getSslSocketFactory());

        if (netGlobalConfig.getConnectionPool() == null) {
            netGlobalConfig.connectionPool(new ConnectionPool(ViseConfig.DEFAULT_MAX_IDLE_CONNECTIONS,
                    ViseConfig.DEFAULT_KEEP_ALIVE_DURATION, TimeUnit.SECONDS));
        }
        ViseNet.getOkHttpBuilder().connectionPool(netGlobalConfig.getConnectionPool());

        if (netGlobalConfig.isCookie() && netGlobalConfig.getApiCookie() == null) {
            netGlobalConfig.apiCookie(new ApiCookie(ViseNet.getContext()));
        }
        if (netGlobalConfig.isCookie()) {
            ViseNet.getOkHttpBuilder().cookieJar(netGlobalConfig.getApiCookie());
        }

        if (netGlobalConfig.getHttpCacheDirectory() == null) {
            netGlobalConfig.setHttpCacheDirectory(new File(ViseNet.getContext().getCacheDir(), ViseConfig.CACHE_HTTP_DIR));
        }
        if (netGlobalConfig.isHttpCache()) {
            try {
                if (netGlobalConfig.getHttpCache() == null) {
                    netGlobalConfig.httpCache(new Cache(netGlobalConfig.getHttpCacheDirectory(), ViseConfig.CACHE_MAX_SIZE));
                }
                netGlobalConfig.cacheOnline(netGlobalConfig.getHttpCache());
                netGlobalConfig.cacheOffline(netGlobalConfig.getHttpCache());
            } catch (Exception e) {
                ViseLog.e("Could not create http cache" + e);
            }
        }
        if (netGlobalConfig.getHttpCache() != null) {
            ViseNet.getOkHttpBuilder().cache(netGlobalConfig.getHttpCache());
        }
    }

    protected  <T> Type getType(T t) {
        Type genType = t.getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        Type type = params[0];
        Type finalNeedType;
        if (params.length > 1) {
            if (!(type instanceof ParameterizedType)) throw new IllegalStateException("没有填写泛型参数");
            finalNeedType = ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            finalNeedType = type;
        }
        return finalNeedType;
    }
}
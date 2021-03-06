package com.liujun.search.element.download;

import com.liujun.search.common.io.CommonIOUtils;
import com.liujun.search.element.download.charsetFlow.HtmlCharsetFlow;
import com.liujun.search.common.constant.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 进行https网页的下载操作
 *
 * @author liujun
 * @version 0.0.1
 * @date 2019/04/04
 */
public class HttpsHtmlDownloadImpl implements HtmlDownLoadInf {

  private Logger logger = LoggerFactory.getLogger(HttpsHtmlDownloadImpl.class);

  private static final Map<String, String> HEAD_MAP = new HashMap<>();

  static {
    // 指定报文头Content-type、User-Agent
    HEAD_MAP.put("Content-type", "application/x-www-form-urlencoded");
    HEAD_MAP.put(
        "User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
  }

  public static final HttpsHtmlDownloadImpl INSTNACE = new HttpsHtmlDownloadImpl();

  @Override
  public String downloadHtml(String url, CloseableHttpClient client) {

    String body = null;

    long startTime = System.currentTimeMillis();

    CloseableHttpResponse response = null;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SysConfig.SYS_MAX_BUFFER_SIZE);

    try {
      // 创建get方式请求对象
      HttpGet get = new HttpGet(url);

      for (Map.Entry<String, String> entryValue : HEAD_MAP.entrySet()) {
        get.setHeader(entryValue.getKey(), entryValue.getValue());
      }

      // 执行请求操作，并拿到结果（同步阻塞）
      response = client.execute(get);

      // 获取结果实体
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        boolean isStream = entity.isStreaming();
        long contextLength = entity.getContentLength();

        logger.info(
            "html downloadHtml url :{} ,rsp context type {}, issteam: {} ,content length : {}  ,",
            url,
            entity.getContentType().getValue(),
            isStream,
            contextLength);

        // 如果当前文件非流，则进行下载文本操作
        if (HttpUtils.ContextTypeChec(entity.getContentType().getValue())) {

          InputStream input = entity.getContent();

          byte[] buffer = new byte[SysConfig.SYS_DEFA_BUFFER_SIZE];

          int index = -1;
          while ((index = input.read(buffer)) != -1) {
            outputStream.write(buffer, 0, index);
          }

          buffer = null;
          byte[] outDataBytes = outputStream.toByteArray();
          ContentType type = ContentType.get(entity);

          // 进行分析
          body = HtmlCharsetFlow.INSTANCE.htmlCharsetValue(outDataBytes, type);
        }
      }

    } catch (ClientProtocolException e) {
      e.printStackTrace();
      logger.error("https download error ,ClientProtocolException", e);
    } catch (IOException e) {
      e.printStackTrace();
      logger.error("https download error ,IOException", e);
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("https download error ,Exception", e);
    } finally {
      CommonIOUtils.close(outputStream);
      HttpUtils.close(response);
    }

    long endTime = System.currentTimeMillis();

    if (StringUtils.isNotEmpty(body)) {
      logger.info(
          "http download :"
              + url
              + ",use time:"
              + (endTime - startTime)
              + " html length :"
              + body.length());
    } else {
      logger.info(
          "http download :" + url + ",use time:" + (endTime - startTime) + " html length 0");
    }

    return body;
  }
}

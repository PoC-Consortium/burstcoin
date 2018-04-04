package integration.common;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.common.TestConstants;
import brs.common.TestInfrastructure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class APISender {

  private final HttpClient httpclient;
  private final JSONParser parser = new JSONParser();

  public APISender() {
    httpclient = HttpClientBuilder.create().build();
  }

  public JSONObject retrieve(String requestType, List<BasicNameValuePair> extraParams) throws IOException, ParseException {
    final HttpPost post = new HttpPost("/burst");

    final List<NameValuePair> urlParameters = new ArrayList<>();
    urlParameters.add(new BasicNameValuePair("requestType", requestType));
    urlParameters.add(new BasicNameValuePair("random", "0.7113466594385798"));
    urlParameters.addAll(extraParams);

    post.setEntity(new UrlEncodedFormEntity(urlParameters));

    final HttpResponse result = httpclient.execute(new HttpHost("localhost", TestInfrastructure.TEST_API_PORT), post);

    return (JSONObject) parser.parse(EntityUtils.toString(result.getEntity(), "UTF-8"));
  }

  public JSONObject getAccount(String accountName) throws IOException, ParseException {
    return retrieve("getAccount", Arrays.asList(
        new BasicNameValuePair(ACCOUNT_PARAMETER, accountName),
        new BasicNameValuePair(FIRST_INDEX_PARAMETER, "0"),
        new BasicNameValuePair(LAST_INDEX_PARAMETER, "100")
      )
    );
  }

}

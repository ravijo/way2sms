package way2sms;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ravijo
 * @dependencies apache httpcomponents-client-4.2.3 and jsoup-1.7.2
 */
public class SMS {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		HttpClient httpclient = new DefaultHttpClient();

		try {

			String username = "your mobile number";
			String password = "your password";
			String msg = "message to send";
			String num = "recepient number";

			HttpPost postLogin = new HttpPost(
					"http://site1.way2sms.com/Login1.action");

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("username", username));
			nvps.add(new BasicNameValuePair("password", password));
			nvps.add(new BasicNameValuePair("userLogin", "no"));
			nvps.add(new BasicNameValuePair("button", "Login"));

			postLogin
					.addHeader("Accept",
							"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			postLogin.addHeader("Accept-Language", "en-US,en;q=0.5");
			postLogin
					.addHeader("User-Agent",
							"Mozilla/5.0 (Windows NT 6.1; rv:18.0) Gecko/20100101 Firefox/18.0");
			postLogin.addHeader("Content-Type",
					"application/x-www-form-urlencoded");

			postLogin.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
			HttpResponse response = httpclient.execute(postLogin);
			HttpEntity entity = response.getEntity();
			EntityUtils.consume(entity);
			System.out.println("Login : " + response.getStatusLine());

			String cookie = response.getFirstHeader("Set-Cookie").getValue();

			String loc = response.getFirstHeader("Location").getValue();
			int index = loc.indexOf("jsessionid=") + 11;
			String token = loc.substring(index, loc.indexOf("?id=", index));

			System.out.println(token);

			HttpGet getPage = new HttpGet(
					"http://site1.way2sms.com/jsp/SingleSMS.jsp?Token=" + token);

			getPage.addHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			getPage.addHeader("Accept-Language", "en-US,en;q=0.5");
			getPage.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; rv:18.0) Gecko/20100101 Firefox/18.0");
			getPage.addHeader("Content-Type",
					"application/x-www-form-urlencoded");
			getPage.addHeader("Referer",
					"http://site1.way2sms.com/Main.action?id=" + token);
			getPage.addHeader("Cookie", cookie);
			getPage.addHeader("Connection", " Keep-Alive");
			getPage.addHeader("Keep-Alive", "300");

			response = httpclient.execute(getPage);
			entity = response.getEntity();
			System.out.println("GetPage : " + response.getStatusLine());

			String pageContent = null;
			if (entity != null) {
				pageContent = EntityUtils.toString(entity);
			}

			String rqTok = null;
			Pattern pattern = Pattern.compile("rqTok=\"(\\w+)\";.*");
			Matcher matcher = pattern.matcher(pageContent);
			while (matcher.find()) {
				rqTok = matcher.group(1);
			}

			StringBuilder temp = new StringBuilder();
			Document doc = Jsoup.parseBodyFragment(pageContent);

			Elements inputs = doc.select("input");
			for (Element el : inputs) {
				Attributes attrs = el.attributes();
				for (Attribute attr : attrs) {
					String key = attr.getKey();
					if (key.equals("name") || key.equals("value")) {
						temp.append(attr.getValue());
						temp.append('=');
					}
				}
				temp.append('&');
			}

			String params = temp.toString().replaceAll("(=[\\w:]+)=&", "$1&");

			if (params.startsWith("=&"))
				params = params.substring(2);

			if (params.endsWith("=&"))
				params = params.substring(0, params.length() - 2);

			params = params + "&textArea=" + msg + "&HiddenAction=instantsms";
			params = params.replaceFirst(rqTok + '=', rqTok + '=' + token);
			params = params.replaceFirst("chkall=", "chkall=on");
			params = params.replaceAll("(=[\\w.]+)=&", "$1&");
			params = params.replaceAll("==", "=");
			params = params.replaceFirst("Mobile Number", num);

			int txtLenIndex = params.indexOf("txtLen=") + 7;
			String txtLen = params.substring(txtLenIndex,
					params.indexOf('&', txtLenIndex));
			params = params.replaceFirst("txtLen=" + txtLen, "txtLen="
					+ URLEncoder.encode(msg, "UTF-8").length());

			// List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			String[] param = params.split("&");
			for (String tmp : param) {
				String[] p = tmp.split("=");
				String key = p[0];

				if (p.length == 2) {
					String val = p[1];

					if (val.endsWith("="))
						val = val.substring(0, val.length() - 1);

					nvps.add(new BasicNameValuePair(key, val));
				}

				else
					nvps.add(new BasicNameValuePair(key, ""));
			}

			System.out.println("----------------------------------------");
			Iterator<NameValuePair> it = nvps.iterator();
			while (it.hasNext()) {
				NameValuePair a = it.next();
				System.out.println(a.getName() + "=" + a.getValue());
			}
			System.out.println("----------------------------------------");

			HttpPost postSend = new HttpPost(
					"http://site1.way2sms.com/jsp/stp2p.action");

			postSend.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; rv:18.0) Gecko/20100101 Firefox/18.0");
			postSend.addHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			postSend.addHeader("Accept-Charset",
					"ISO-8859-1,utf-8;q=0.7,*;q=0.3");
			postSend.addHeader("Accept-Encoding", "gzip,deflate,sdch");
			postSend.addHeader("Accept-Language", "en-US,en;q=0.8");
			postSend.addHeader("Cache-Control", "max-age=0");
			postSend.addHeader("Connection", "keep-alive");
			postSend.addHeader("Content-Type",
					"application/x-www-form-urlencoded");
			postSend.addHeader("Cookie", cookie);
			postSend.addHeader("Host", "site1.way2sms.com");
			postSend.addHeader("Referer",
					"http://site1.way2sms.com/jsp/SingleSMS.jsp?Token=" + token);

			postSend.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
			response = httpclient.execute(postSend);
			entity = response.getEntity();
			EntityUtils.consume(entity);
			System.out.println("Send : " + response.getStatusLine());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				System.out.println(headers[i]);
			}
			System.out.println("----------------------------------------");

			loc = response.getFirstHeader("Location").getValue();
			System.out.println(loc);

		}

		catch (HttpHostConnectException e) {
			System.err.println("Error=" + e.getMessage());
		}

		finally {
			httpclient.getConnectionManager().shutdown();
		}

	}
}
